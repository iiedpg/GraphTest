#!/usr/bin/env bash

. `dirname $0`/config.sh

function list_jobs(){
	local dout=$1
	if [ ! -e "$dout" ];then
		return 1
	fi

	grep -ir "Connected to Spark cluster with app ID" $dout | awk '{print $NF}'	
}

function copy_task_logs_back(){
	local i=0
	for job in `list_jobs $output_dir/dout`;do
		mkdir -p $output_dir/_copy_tmp/$i
		local task_logs_dir=$spark_dir/work/$job
		
		for m in `cat $spark_dir/conf/slaves | grep -v "^#"`;do
			has=`ssh -n $m "if [ -d $task_logs_dir ];then echo 1;else echo 0;fi"`
			if [ "$has" = "1" ];then
				scp -r $m:$task_logs_dir/* $output_dir/_copy_tmp/$i/
			fi
		done

		i=$((i+1))
	done
}


function list_spark_app_jobs(){
grep -in "Job finished" $output_dir/dout |
	 sed 's/\([0-9]\{1,\}\).*took \([^ ]*\) s/\1 \2/g' |
		 awk 'BEGIN{last=1} {print last,$1,$2;last=$1 + 1}' |
		 	tail -1
}

function analyze_dout_to_stages(){
	mkdir -p $output_dir/_dout_by_job	
	while read start end time;do			
		local jobs_dir=$output_dir/_dout_by_job/${end}_${time}
		mkdir -p $jobs_dir

		sed -n -e "$start, $end p" $output_dir/dout | grep "====dep====" | sed -e 's/.*====dep====\(.*\)/\1/g' | sort -k1n -u > $jobs_dir/dep_list
		sed -n -e "$start, $end p" $output_dir/dout | grep "====stage_to_rdd====" | sed -e 's/.*====stage_to_rdd====\(.*\)/\1/g' | awk -F ',' '{print $1,$2}' | sort -k1n -u > $jobs_dir/stage_to_rdd_list
	    sed -n -e "$start, $end p" $output_dir/dout | grep -ir "Stage .* finished" | sed -e 's/.* Stage \([0-9]\{1,\}\).* in \(.*\) s/\1 \2/g' | sort -k1n > $jobs_dir/stage_time_list

	done< <(list_spark_app_jobs)

	[ -e $output_dir/statis ] && rm $output_dir/statis

	for f in `ls $output_dir/_copy_tmp/0`;do
		cat $output_dir/_copy_tmp/0/$f/stderr | grep -ir "^====[^=]" | sed -e 's/^====\(.*\)/\1/g' |
			while read line;do
				echo $f $line >> $output_dir/statis
			done
	done
}


function analyze_logs_by_turn(){
	[ -e $output_dir/_tmp ] && rm -rf $output_dir/_tmp
	
	mkdir -p $output_dir/_tmp/0
	mkdir -p $output_dir/_results_by_turn

	local jobs_dir=$output_dir/_dout_by_job/*

	while read task_id name time times;do 
		printf "%s\t%s\t%s\n" $name $time $times >> $output_dir/_tmp/0/$task_id	
	done< <(python $BASE_DIR/bin/spark_stage_to_turn.py $jobs_dir/dep_list $jobs_dir/stage_to_rdd_list $jobs_dir/stage_time_list $output_dir/statis)

	mv $output_dir/_tmp/0/-1 $output_dir/
	local max_turn=`grep -ir "^====.*:.*$" $output_dir/_tmp/0 | awk -F [:'\t'] '{print $3}' | sort -k1n | tail -1`

	i=0;
	while [ "$i" -le "$max_turn" ];do
		local sd=$output_dir/_tmp/0
		local af=$output_dir/_results_by_turn/${i}

		if [ -f ${af} ];then
			rm ${af}
		fi

		grep_str="^====.*:${i}.*$"
		nr_tasks=`ls $sd | wc -l`
		for f in `ls $sd`;do
			no=`echo ${f##*_} | awk '{printf("%d\n", $1)}'`
            if [ "$no" -ge "$nr_split" ];then
                continue
            fi
			cat $sd/$f | grep -ir $grep_str | python $BASE_DIR/bin/spark_analyze_to_line.py $no >> ${af}
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
spark_dir=$SPARK_DIR

set -x
copy_task_logs_back
analyze_dout_to_stages
analyze_logs_by_turn
data_to_sql
set +x
