#!/usr/bin/env bash

BASE_DIR=`dirname $0`;
BASE_DIR=`cd $BASE_DIR;pwd`;

set -x

jar cvf test.jar -C bin .

$h2_bin/hadoop fs -rm -r hdfs://localhost:9000/giraph/output/1
HADOOP_HOME=/home/owner-pc/env/hadoop-2.2.0 /home/owner-pc/src/giraph-1.0.0/bin/giraph test.jar graph.PageRank -vif input.MyAdjVertexTextInputFormat -vip /input/1.txt -of org.apache.giraph.io.formats.IdWithValueTextOutputFormat -op /giraph/output/1 -mc graph.PageRank\$SimplePageRankVertexMasterCompute -c graph.PageRank\$SimplePageRankVertexCombiner -w 2 -ca nr.turns=4
#HADOOP_HOME=/home/owner-pc/env/hadoop-2.2.0 /home/owner-pc/src/giraph-1.0.0/bin/giraph test.jar graph.MST -vif input.MSTAdjVertexTextInputFormat -vip /uinput/1.txt -of org.apache.giraph.io.formats.IdWithValueTextOutputFormat -op /giraph/output/1 -mc graph.MST\$MSTMasterCompute -w 2 -ca nr.turns=3
#HADOOP_HOME=/home/owner-pc/env/hadoop-2.2.0 /home/owner-pc/src/giraph-1.0.0/bin/giraph test.jar graph.MST -vif input.MSTAdjVertexTextInputFormat -vip /uinput/2.txt -of graph.MST\$MSTOutputFormat -op /giraph/output/1.5 -mc graph.MST\$MSTMasterCompute -w 2 -c graph.MST\$MSTCombiner -wc graph.MST\$MSTWorkerContext -ca nr.turns=3

set +x
