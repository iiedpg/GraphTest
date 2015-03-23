#!/usr/bin/env bash

. `dirname $0`/config.sh
action=${1:-"jar"}

OUTPUT_DIR=$BASE_DIR/output/giraph

if [ "x$action" = "xjar" ];then
	compile_eclipse_java $GIRAPH_TEST_WS $GIRAPH_TEST_WS/src/graph
elif [ "x$action" = "xupload" ];then
	scp $GIRAPH_TEST_WS/deploy.jar $GIRAPH_TEST_USER@$GIRAPH_TEST_M:$GIRAPH_TEST_DIR/giraph_test.jar
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

	hadoop fs -rm -r ${output}
	time HADOOP_HOME=$MR_DIR $GIRAPH_DIR/bin/giraph $GIRAPH_TEST_DIR/giraph_test.jar ${GIRAPH_BASE_PACKAGE}.${class} -vif input.${class}AdjVertexTextInputFormat -vip ${input} -of org.apache.giraph.io.formats.IdWithValueTextOutputFormat -op ${output} -w ${nr_split} -ca nr.turns=$turn > $OUTPUT_DIR/${name}/dout 2>&1
	#time HADOOP_HOME=$MR_DIR $GIRAPH_DIR/bin/giraph $GIRAPH_TEST_DIR/giraph_test.jar ${GIRAPH_BASE_PACKAGE}.${class} -vif input.MyAdjVertexTextInputFormat -vip ${input} -of org.apache.giraph.io.formats.IdWithValueTextOutputFormat -op ${output} -w ${nr_split} -c ${GIRAPH_BASE_PACKAGE}.${class}\$SimplePageRankVertexCombiner -ca nr.turns=$turn > $OUTPUT_DIR/${name}/dout 2>&1
	#time HADOOP_HOME=$MR_DIR $GIRAPH_DIR/bin/giraph $GIRAPH_TEST_DIR/giraph_test.jar ${GIRAPH_BASE_PACKAGE}.${class} -vif input.MSTAdjVertexTextInputFormat -vip ${input} -of graph.MST\$MSTOutputFormat -op ${output} -w ${nr_split} -mc graph.MST\$MSTMasterCompute -c graph.MST\$MSTCombiner -wc graph.MST\$MSTWorkerContext -ca nr.turns=$turn > $OUTPUT_DIR/${name}/dout 2>&1
	#time HADOOP_HOME=$MR_DIR $GIRAPH_DIR/bin/giraph $GIRAPH_TEST_DIR/giraph_test.jar ${GIRAPH_BASE_PACKAGE}.${class} -vif input.MSTAdjVertexTextInputFormat -vip ${input} -of graph.MST\$MSTOutputFormat -op ${output} -w ${nr_split} -mc graph.MST\$MSTMasterCompute -wc graph.MST\$MSTWorkerContext -ca nr.turns=$turn > $OUTPUT_DIR/${name}/dout 2>&1
	set +x

elif [ "x$action" = "xsummary" ];then
	class=${2:-"PageRank"}
	input=$3
	output=$4
	nr_split=${5-"2"}
	turn=${6:-"1"}

	name=${class}_`basename $input`_${nr_split}_${turn}

    set -x
    $BASE_DIR/bin/giraph_analyze_result.sh $OUTPUT_DIR/$name $nr_split $input
    set +x
else
	printf "action $action is unknown \n"
	exit 1
fi
