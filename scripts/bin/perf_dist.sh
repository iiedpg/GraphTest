#!/usr/bin/env bash

PATTERN_FILE=$1
OUTPUT_DIR=$2
TICKS=$3
HZ=$4
NETHOGS=$5
SLEEP_TIME=$6

BIN_DIR=`dirname $0`;

function handle_process(){
	pid=$1
	pname=$2

	#printf "test for $pid,  $pname\n"
    #1. nethogs
    last_line=`tac $NETHOGS | grep "#$pid#" | head -1`

    #2. perf_dist
    #echo "$pid $TICKS $HZ $last_line"
    CPU_USAGE=`ps -p $pid -o pcpu | grep -v "CPU"`
    RESULT=`python $BIN_DIR/perf_proc.py $pid $TICKS $HZ "$last_line" $CPU_USAGE`

    printf "#$pname\n" >> $OUTPUT_DIR/${pid}
    printf "$RESULT\n" >> $OUTPUT_DIR/${pid}
}

RUNNING=1
trap "RUNNING=0" SIGINT SIGTERM

while [ $RUNNING -eq 1 ];do
    cat $PATTERN_FILE | 
        while read patt reverse_patt;do
    		if [[ "$patt" == \#* ]];then
				echo ${patt###} >> /tmp/hehe
				jps | grep ${patt###} |
					while read pid pname;do	
						echo $pid $pname >> /tmp/hehe
						handle_process $pid $pname
					done
			else
				echo ${patt###} >> /tmp/hehe_2

				if [ -z "$reverse_patt" ];then
					ps aux | grep $patt | grep -v 'grep' | awk '{print $2, $0}' |
						while read pid pname;do
							handle_process $pid "$pname"
					   done
				else
					echo ${reverse_patt} >> /tmp/hehe_3
					ps aux | grep $patt | grep -v "${reverse_patt}" | grep -v 'grep' | awk '{print $2, $0}' |
						while read pid pname;do
							handle_process $pid "$pname"
					   done
	
				fi
			fi
        done
    sleep $SLEEP_TIME
    #sleep 1
done
