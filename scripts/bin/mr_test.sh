#!/usr/bin/env bash

. `dirname $0`/config.sh
action=${1:-"jar"}

OUTPUT_DIR=$BASE_DIR/output/mr

if [ "x$action" = "xjar" ];then
	compile_eclipse_java $MR_TEST_WS
elif [ "x$action" = "xupload" ];then
	scp $MR_TEST_WS/deploy.jar $MR_TEST_USER@$MR_TEST_M:$MR_TEST_DIR/mr_test.jar
elif [ "x$action" = "xrun" ];then
	class=${2:-"PageRank"}
	input=$3
	output=$4
	nr_split=${5-"2"}
	turn=${6:-"1"}

	name=${class}_`basename $input`_${nr_split}_${turn}

	set -x
	[ -e $OUTPUT_DIR/${name} ] && rm -rf $OUTPUT_DIR/${name}
	mkdir -p $OUTPUT_DIR/${name} || exit 1

	ssh -n $MR_TEST_USER@$MR_TEST_M "source /etc/profile;cd $MR_TEST_DIR;hadoop jar $MR_TEST_DIR/mr_test.jar $MR_BASE_PACKAGE.${class} $input $output $nr_split $turn" >> $OUTPUT_DIR/${name}/dout 2>&1

	set +x
elif [ "x$action" = "xsummary" ];then
	class=${2:-"PageRank"}
	input=$3
	output=$4
	nr_split=${5-"2"}
	turn=${6:-"1"}

	name=${class}_`basename $input`_${nr_split}_${turn}

    set -x
    $BASE_DIR/bin/mr_analyze_result.sh $OUTPUT_DIR/$name $nr_split $input
    set +x
else
	printf "action $action is unknown \n"
	exit 1
fi
