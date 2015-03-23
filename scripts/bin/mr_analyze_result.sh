#!/usr/bin/env bash

. `dirname $0`/config.sh

function list_jobs(){
	local dout=$1
	if [ ! -e "$dout" ];then
		return 1
	fi

	grep -ir "running job" $dout | sed -e 's/.*\(job_[0-9_]*\)/\1/g'						
}

function copy_task_logs_back(){
	local i=0
	for job in `list_jobs $output_dir/dout`;do
		mkdir -p $output_dir/_copy_tmp/$i
		local task_logs_dir=$hadoop_dir/logs/userlogs/application_${job##job_}
		for m in `cat $hadoop_dir/etc/hadoop/slaves`;do
			has=`ssh -n $m "if [ -d $task_logs_dir ];then echo 1;else echo 0;fi"`
			if [ "$has" = "1" ];then
				scp -r $m:$task_logs_dir/* $output_dir/_copy_tmp/$i/
			fi
		done

		i=$((i+1))
	done
}

function analyze_logs_by_turn(){
	mkdir -p $output_dir/_results_by_turn

	for turn in `ls $output_dir/_copy_tmp`;do
		local sd=$output_dir/_copy_tmp/$turn
		local af=$output_dir/_results_by_turn/${turn}

		if [ -f ${af}_map ];then
			rm ${af}_map
		fi

		if [ -f ${af}_red ];then
			rm ${af}_red
		fi

		nr_tasks=`ls $sd | wc -l`
		reduce_no_start=$((nr_tasks - nr_split + 1))
		for f in `ls $sd`;do
			no=`echo ${f##*_} | awk '{printf("%d\n", $1)}'`
			if [ "$no" = "1" ];then	
				continue
			elif [ $no -lt $reduce_no_start ];then
				cat $sd/$f/syslog | grep -ir "^====" | python $BASE_DIR/bin/mr_analyze_to_line.py $no >> ${af}_map
			else
				cat $sd/$f/syslog | grep -ir "^====" | python $BASE_DIR/bin/mr_analyze_to_line.py $no >> ${af}_red
			fi
		done
	done
}

function summary_turns_time(){
	grep -ir "Total time spent by all maps in occupied slots (ms)" $output_dir/dout | awk -F '=' '{print NR - 1, $2}' > $output_dir/_map_time
	grep -ir "Total time spent by all reduces in occupied slots (ms)" $output_dir/dout | awk -F '=' '{print NR - 1, $2}' > $output_dir/_red_time
	cat $output_dir/dout | sed -n -e 's/turn \([0-9]\+\) time = \([0-9]\+\)ms/\1 \2/g p' > $output_dir/_turns

	join $output_dir/_map_time $output_dir/_red_time > $output_dir/_tmp
	join $output_dir/_tmp $output_dir/_turns > $output_dir/-1
	rm $output_dir/_map_time $output_dir/_red_time $output_dir/_tmp
}

function data_to_sql(){
	total_turn=`ls $output_dir/_copy_tmp | wc -l`
	sql=$output_dir/insert.sql

	[ -e $sql ] && rm -f $sql

	printf "use performance_overview;\n" >> $sql
	printf "SET AUTOCOMMIT = 0;\n" >> $sql
	printf "START TRANSACTION;\n" >> $sql

	i=0
	while [ $i -lt $total_turn ];do
		python $BASE_DIR/bin/trans_sql.py "mr" "$nr_split" "$total_turn" "$i" "Y" "$t_input" "$output_dir/_results_by_turn/${i}_map" >> $sql
		python $BASE_DIR/bin/trans_sql.py "mr" "$nr_split" "$total_turn" "$i" "N" "$t_input" "$output_dir/_results_by_turn/${i}_red" >> $sql
		i=$((i+1))
	done

	printf "COMMIT;\n" >> $sql
}

output_dir=$1
nr_split=$2
t_input=$3
hadoop_dir=$MR_DIR

set -x
copy_task_logs_back
analyze_logs_by_turn
summary_turns_time
data_to_sql
set +x
