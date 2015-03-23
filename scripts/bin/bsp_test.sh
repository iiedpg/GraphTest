#!/usr/bin/env bash

. `dirname $0`/config.sh

action=${1:-"make"}

OUTPUT_DIR=$BASE_DIR/output/bsp

if [ "x$action" = "xmake" ];then
	ssh $BSP_TEST_USER@$BSP_TEST_M "cd $BSP_TEST_DIR/build;make";
elif [ "x$action" = "xrun" ];then
	head_file=$BSP_TEST_DIR/$2
	so_file=$BSP_TEST_DIR/$3
	input=$4
	output=$5
    nr_split=$6
    max_turn=$7

	name=`basename ${so_file}`_`basename $input`_${nr_split}_${max_turn}

    [ -e $OUTPUT_DIR/${name} ] && rm -rf $OUTPUT_DIR/${name}
	mkdir -p $OUTPUT_DIR/${name} || exit 1

	set -x
	echo $OUTPUT_DIR/${name}/dout
	ssh -n $BSP_TEST_USER@$BSP_TEST_M "cd $BSP_TEST_DIR/bin;. $BSP_DIR/sbin/bsp-config.sh;./hao_client -h $head_file -s $so_file -i $input -o $output -p $nr_split" > $OUTPUT_DIR/${name}/dout 2>&1
	set +x

elif [ "x$action" = "xsummary" ];then
	head_file=$BSP_TEST_DIR/$2
	so_file=$BSP_TEST_DIR/$3
	input=$4
	output=$5
    nr_split=$6
    max_turn=$7

	name=`basename ${so_file}`_`basename $input`_${nr_split}_${max_turn}

	set -x
    $BASE_DIR/bin/bsp_analyze_result.sh $OUTPUT_DIR/${name} $nr_split $input
    set +x


	exit 0
fi
