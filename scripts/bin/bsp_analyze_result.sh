#!/usr/bin/env bash

. `dirname $0`/config.sh

function list_jobs(){
	local dout=$1
	if [ ! -e "$dout" ];then
		return 1
	fi

    grep -ir "job id = " $dout | awk '{print $NF}'
}

function copy_task_logs_back(){
	local i=0
	for job in `list_jobs $output_dir/dout`;do
        echo $job
		mkdir -p $output_dir/_copy_tmp/$i
		local task_logs_dir=$output_dir
		
		for m in `cat $bsp_dir/conf/slaves | grep -v "^#"`;do
            for d in `ssh -n $m "ls $bsp_output_dir | grep $job"`;do
                local copy_name=${d##*_}
                scp -r $m:$bsp_output_dir/$d $output_dir/_copy_tmp/$i/$copy_name
            done
		done

		i=$((i+1))
	done
}

function analyze_dout_to_stages(){
    cat $output_dir/dout | grep "^====" > $output_dir/statis
}


function analyze_logs_by_turn(){
	mkdir -p $output_dir/_results_by_turn

    local max_turn=`cat $output_dir/statis | awk -F ':' '{print $2}' | sort -k1n | tail -1`

	i=0;
	while [ "$i" -le "$max_turn" ];do
		local af=$output_dir/_results_by_turn/${i}

		if [ -f ${af} ];then
			rm ${af}
		fi

        j=0;
        while [ "$j" -lt "$nr_split" ];do  
            cat $output_dir/statis | grep -ir ".*_$j:$i:" | python $BASE_DIR/bin/bsp_analyze_to_line.py $j >> ${af} 
            j=$((j + 1))
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
		python $BASE_DIR/bin/trans_sql.py "bsp" "$nr_split" "$total_turn" "$i" "N" "$t_input" "$output_dir/_results_by_turn/${i}" >> $sql
		i=$((i+1))
	done

	printf "COMMIT;\n" >> $sql
}


output_dir=$1
nr_split=$2
t_input=$3
bsp_output_dir=$BSP_OUTPUT_DIR
bsp_dir=$BSP_DIR

set -x
copy_task_logs_back
analyze_dout_to_stages
analyze_logs_by_turn
#data_to_sql
set +x
