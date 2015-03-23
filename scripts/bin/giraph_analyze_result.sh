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

	local max_turn=`grep -ir "^====.*:.*$" $output_dir/_copy_tmp/0 | awk -F [:'\t'] '{print $3}' | sort -k1n | tail -1`

	i=0;
	while [ "$i" -le "$max_turn" ];do
		local sd=$output_dir/_copy_tmp/0
		local af=$output_dir/_results_by_turn/${i}

		if [ -f ${af} ];then
			rm ${af}
		fi

		if [ $i -eq 0 ];then
			grep_str="^====[^:]\{1,\}$"
		else
			grep_str="^====.*:${i}\s.*$"
		fi

		nr_tasks=`ls $sd | wc -l`
		for f in `ls $sd`;do
			no=`echo ${f##*_} | awk '{printf("%d\n", $1)}'`
			if [ "$no" = "1" ];then	
				continue
			else
				cat $sd/$f/syslog | grep -ir $grep_str | python $BASE_DIR/bin/giraph_analyze_to_line.py $no >> ${af}
			fi
		done
	
		i=$((i+1))
	done
}

function data_to_sql(){
	total_turn=`ls $output_dir/_results_by_turn | wc -l`
	sql=$output_dir/insert.sql

	[ -e $sql ] && rm -f $sql

	printf "use performance_overview;\n" >> $sql
	printf "SET AUTOCOMMIT = 0;\n" >> $sql
	printf "START TRANSACTION;\n" >> $sql


	i=0
	while [ $i -lt $total_turn ];do
		python $BASE_DIR/bin/trans_sql.py "giraph" "$nr_split" "$total_turn" "$i" "N" "$t_input" "$output_dir/_results_by_turn/${i}" >> $sql
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
data_to_sql
set +x
