#!/usr/bin/env bash

. `dirname $0`/config.sh

with_run=$1

$BASE_DIR/bin/spark_test.sh jar
$BASE_DIR/bin/spark_test.sh upload

COUNT=1
while read class_name input output nr_split turn;do
    if [[ "$class_name" == \#* ]];then
        continue
    fi
    echo $class_name $input $output $nr_split $turn
    
    #. $BASE_DIR/bin/perf_init.sh spark.conf "spark/${class_name}_`basename $output`_${nr_split}_${turn}"

    if [ "x$with_run" = "xrun" ];then
        echo "running..."
        $BASE_DIR/bin/spark_test.sh run $class_name $input $output $nr_split $turn
    fi

    #$BASE_DIR/bin/spark_test.sh summary $class_name $input $output $nr_split $turn

	#. $BASE_DIR/bin/perf_collect_result.sh

    COUNT=$((COUNT + 1))
done< <(cat $BASE_DIR/job_def/spark/spark_testset)
