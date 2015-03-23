#!/usr/bin/env bash

BASE_DIR=`dirname $0`
BASE_DIR=`cd $BASE_DIR;pwd;`


set -x
jar -cvf $BASE_DIR/test.jar -C bin .
#/home/owner-pc/env/hadoop-2.2.0/bin/hadoop jar $BASE_DIR/test.jar graph.SSSP /input/2.txt /mr/med/sssp/2 2 0
/home/owner-pc/env/hadoop-2.2.0/bin/hadoop jar $BASE_DIR/test.jar graph.DirectToUnDirect /input/2.txt /undirect/tmp/2 2 0
set +x
