#!/usr/bin/env bash

. `dirname $0`/config.sh

action=${1:-"make"}

OUTPUT_DIR=$BASE_DIR/output/graphchi
if [ "x$action" = "xgen_data" ];then
	input_list=$2	
	set -x
	cat $input_list |
		while read f to;do
			to=$GRAPHCHI_INPUT_DIR/$to/$to
			mkdir -p `dirname $to`;
			hadoop fs -cat $f | python $BASE_DIR/bin/trans_graphchi_data.py > $to
		done
	set +x
elif [ "x$action" = "xrun" ];then
	app=$GRAPHCHI_DIR/bin/example_apps/$2
	input=$GRAPHCHI_INPUT_DIR/$3/$3
	output=$GRAPHCHI_OUTPUT_DIR/$4/$4
    nr_split=$5
    max_turn=$6

	name=`basename ${app}`_`basename $input`_${nr_split}_${max_turn}

    [ -e $OUTPUT_DIR/${name} ] && rm -rf $OUTPUT_DIR/${name}
	mkdir -p $OUTPUT_DIR/${name} || exit 1

	set -x
	echo $OUTPUT_DIR/${name}/dout
	time ssh -n $GRAPHCHI_TEST_USER@$GRAPHCHI_TEST_M "source /etc/profile;cd $GRAPHCHI_DIR;$app file $input niters $max_turn filetype adjlist > $OUTPUT_DIR/${name}/dout 2>&1"
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
