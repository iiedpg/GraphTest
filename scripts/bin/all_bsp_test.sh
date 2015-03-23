#!/usr/bin/env bash
. `dirname $0`/config.sh

with_run=$1

COUNT=1
while read head_file so_file input output nr_split turn;do
    if [[ "$head_file" == \#* ]];then
        continue
    fi

    echo $head_file $so_file $input $output $nr_split $turn

    #. $BASE_DIR/bin/perf_init.sh bsp.conf "bsp/`basename ${so_file}`_`basename $output`_${nr_split}_${turn}"

    #sleep 1 #make sure monitor start

	if [ "x$with_run" = "xrun" ];then
        $BASE_DIR/bin/bsp_test.sh run $head_file $so_file $input $output $nr_split $turn
	fi

	$BASE_DIR/bin/bsp_test.sh summary $head_file $so_file $input $output $nr_split $turn

#    echo "sleeping 5"
#    sleep 5;
	
	#. $BASE_DIR/bin/perf_collect_result.sh

    COUNT=$((COUNT + 1))
done< <(cat $BASE_DIR/job_def/bsp/bsp_testset)
