#!/usr/bin/env bash

. `dirname $0`/config.sh
action=${1:-"jar"}

OUTPUT_DIR=$BASE_DIR/output/spark

if [ "x$action" = "xjar" ];then
	compile_eclipse_scala $SPARK_TEST_WS $SPARK_TEST_WS/src/graph
elif [ "x$action" = "xupload" ];then
	scp $SPARK_TEST_WS/deploy.jar $SPARK_TEST_USER@$SPARK_TEST_M:$SPARK_TEST_DIR
elif [ "x$action" = "xrun" ];then
	class=${2:-"graph.SSSPBig"}
	input=$3
	output=$4
    nr_split=$5
    max_turn=$6

	name=${class}_`basename $input`_${nr_split}_${max_turn}

    [ -e $OUTPUT_DIR/${name} ] && rm -rf $OUTPUT_DIR/${name}
	mkdir -p $OUTPUT_DIR/${name} || exit 1

	tmp_cp=.
	for f in `ls $SPARK_TEST_WS/lib`;do
		tmp_cp=$tmp_cp:$SPARK_TEST_WS/lib/$f;
	done

	set -x
	echo $OUTPUT_DIR/${name}/dout
	time ssh -n $SPARK_TEST_USER@$SPARK_TEST_M "source /etc/profile;cd $SPARK_TEST_DIR;java -cp deploy.jar:$tmp_cp $class $input $output $nr_split $max_turn" > $OUTPUT_DIR/${name}/dout 2>&1
	set +x

elif [ "x$action" = "xsummary" ];then
	class=${2:-"graph.SSSPBig"}
	input=$3
	output=$4
    nr_split=$5
    max_turn=$6

	name=${class}_`basename $input`_${nr_split}_${max_turn}

    set -x
    $BASE_DIR/bin/spark_analyze_result.sh $OUTPUT_DIR/${name} $nr_split $input
    set +x

	#generate stage statistics
#	cat $OUTPUT_DIR/${name}/dout | python $BASE_DIR/bin/stage.py > $OUTPUT_DIR/${name}/stages
#	#generate iteration statistics
#	cat $OUTPUT_DIR/${name}/dout | python $BASE_DIR/bin/turn.py > $OUTPUT_DIR/${name}/turns
#elif [ "x$action" = "xsummary_bagel" ];then
#	name=${2:-"result"}		
#	#generate stage statistics
#	cat $OUTPUT_DIR/${name}/dout | python $BASE_DIR/bin/stage.py > $OUTPUT_DIR/${name}/stages		
#	#generate iteration statistics
#	cat $OUTPUT_DIR/${name}/dout | python $BASE_DIR/bin/superstep.py > $OUTPUT_DIR/${name}/turns
else
	printf "action $action is unknown \n"
	exit 1
fi
