#!/usr/bin/env bash

. `dirname $0`/config.sh

with_run=$1
COUNT=1

$BASE_DIR/bin/mr_test.sh jar
$BASE_DIR/bin/mr_test.sh upload

while read class_name input output nr_split turn;do
    if [[ "$class_name" == \#* ]];then
        continue
    fi

    #. $BASE_DIR/bin/perf_init.sh mr.conf "mr/mr_${class_name}_`basename $output`_${nr_split}_${turn}"

    if [ "x$with_run" = "xrun" ];then
        $BASE_DIR/bin/mr_test.sh run $class_name $input $output $nr_split $turn
    fi

	#echo "sleeping 5..."
    #sleep 5;

    $BASE_DIR/bin/mr_test.sh summary $class_name $input $output $nr_split $turn

	#. $BASE_DIR/bin/perf_collect_result.sh

    COUNT=$((COUNT + 1))
done< <(cat $BASE_DIR/job_def/mr/mr_testset)