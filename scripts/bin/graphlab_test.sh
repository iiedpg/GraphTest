#!/usr/bin/env bash

. `dirname $0`/config.sh

action=${1:-"make"}

OUTPUT_DIR=$BASE_DIR/output/graphlab

if [ "x$action" = "xmake" ];then
	ssh $GRAPHLAB_TEST_USER@GRAPHLAB_TEST_M "cd $GRAPHLAB_TEST_DIR;make"
elif [ "x$action" = "xrun" ];then
	app=$GRAPHLAB_TEST_DIR/$2
	input=$3
	output=$4
    nr_split=$5
    max_turn=$6

	sync="async"

	name=`basename ${app}`_`basename $input`_${nr_split}_${max_turn}_${sync}

    [ -e $OUTPUT_DIR/${name} ] && rm -rf $OUTPUT_DIR/${name}
	mkdir -p $OUTPUT_DIR/${name} || exit 1
	doforall.sh "if [ -e \"$OUTPUT_DIR/${name}/stat\" ];then rm -rf $OUTPUT_DIR/${name}/stat;fi;mkdir -p $OUTPUT_DIR/${name}/stat;"

	set -x
	echo $OUTPUT_DIR/${name}/dout
	time ssh -n $GRAPHLAB_TEST_USER@$GRAPHLAB_TEST_M "source /etc/profile;cd $GRAPHLAB_TEST_DIR;. $BSP_DIR/sbin/bsp-config.sh;mpiexec -n ${nr_split} -hostfile ~/programer/mpi/hosts $app $input $output $max_turn $OUTPUT_DIR/${name}/stat" $sync> $OUTPUT_DIR/${name}/dout 2>&1
	#time ssh -n $GRAPHLAB_TEST_USER@$GRAPHLAB_TEST_M "source /etc/profile;cd $GRAPHLAB_TEST_DIR;. $BSP_DIR/sbin/bsp-config.sh;mpiexec -n ${nr_split} -hostfile ~/programer/mpi/hosts $app $input $output $max_turn $OUTPUT_DIR/${name}/stat" > $OUTPUT_DIR/${name}/dout 2>&1
	set +x

elif [ "x$action" = "xsummary" ];then
	app=$BSP_TEST_DIR/$2
	input=$3
	output=$4
    nr_split=$5
    max_turn=$6

	name=`basename ${app}`_`basename $input`_${nr_split}_${max_turn}

	set -x
    #$BASE_DIR/bin/graphlab_analyze_result.sh $OUTPUT_DIR/${name} $nr_split $input
    set +x


	exit 0
fi
