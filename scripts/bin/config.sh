#!/usr/bin/env bash 

BASE_DIR=`dirname $BASH_SOURCE`/..
BASE_DIR=`cd $BASE_DIR;pwd;`
. $BASE_DIR/bin/utils.sh

#parameters for upload and run spark testsets.
SPARK_TEST_WS="/home/bsp/zhanghao/gaoyun/SparkTest0.9"
SPARK_TEST_M="192.168.255.118"
SPARK_TEST_USER="bsp"
SPARK_TEST_DIR="/home/bsp/zhanghao/test_result/spark_test"
SPARK_DIR="/home/bsp/programer/spark-0.9.0-incubating"

MR_TEST_WS="/home/bsp/zhanghao/gaoyun/Hadoop2.2Test"
MR_TEST_M="192.168.255.118"
MR_TEST_USER="bsp"
MR_TEST_DIR="/home/bsp/zhanghao/test_result/mr2_test"
MR_BASE_PACKAGE="graph"
MR_DIR="/home/bsp/programer/hadoop-2.2.0"

GIRAPH_TEST_WS="/home/bsp/zhanghao/gaoyun/Giraph1.0Test"
GIRAPH_TEST_M="192.168.255.118"
GIRAPH_TEST_USER="bsp"
GIRAPH_TEST_DIR="/home/bsp/zhanghao/test_result/giraph_test"
GIRAPH_BASE_PACKAGE="graph"
GIRAPH_DIR="/home/bsp/programer/giraph-1.0.0"


#graphlab
GRAPHLAB_DIR="/home/bsp/programer/graphlab-master"
GRAPHLAB_TEST_WS="$GRAPHLAB_DIR/apps/all_test"
GRAPHLAB_TEST_DIR="$GRAPHLAB_DIR/release/apps/all_test"
GRAPHLAB_TEST_M="192.168.255.118"
GRAPHLAB_TEST_USER="bsp"

#graphchi
GRAPHCHI_DIR="/home/bsp/programer/graphchi"
GRAPHCHI_INPUT_DIR="/home/bsp/programer/graphchi_env/input"
GRAPHCHI_OUTPUT_DIR="/home/bsp/programer/graphchi_env/output"
GRAPHCHI_TEST_M="192.168.255.118"
GRAPHCHI_TEST_USER="bsp"


##performance test
PERF_TEST_USER=bsp
PERF_TEST_DIR=/home/bsp/zhanghao/perf_test
PERF_TEST_DIR_BIN=$PERF_TEST_DIR/bin
PERF_TEST_DIR_NETHOGS=$PERF_TEST_DIR/nethogs
PERF_TEST_DIR_TMPOUT=$PERF_TEST_DIR/tmp_out
PERF_TEST_DIST_SH=$BASE_DIR/bin/perf_dist.sh
PERF_TEST_TICKS_CPP=$BASE_DIR/bin/ticks.cpp
PERF_TEST_PROC_PY=$BASE_DIR/bin/perf_proc.py
PERF_TEST_MACHINE_FILE=$BASE_DIR/conf/ms

#scala
SCALA_HOME=/usr/local/scala-2.10.3
PATH=$SCALA_HOME/bin:$PATH

BSP_TEST_USER="bsp"
BSP_TEST_M="192.168.255.118"
BSP_DIR="/home/bsp/programer/bsp_gaoyun"
BSP_TEST_DIR="/home/bsp/zhanghao/hao_codes/hao_test"
#BSP_TEST_DIR="/home/bsp/programer/friend_recommend"
BSP_OUTPUT_DIR="/home/bsp/zhanghao/bsp_dir/slave_num/running"
