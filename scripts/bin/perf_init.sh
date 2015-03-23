#!/usr/bin/env bash

#set -x
. `dirname $0`/config.sh

if [ $# -lt 2 ];then
	echo "Usage: $0 PATTERN_FILE OUTPUT_DIR"
	exit 1
fi

PATTERN_FILE="$BASE_DIR/conf/$1"
OUTPUT_DIR="$BASE_DIR/perf_test/$2"

PERF_START_DATE=`date +"%s"`;
PERF_RAND_KEY=${PERF_START_DATE}_`cat /dev/urandom | tr -dc A-Za-z0-9_ | head -c 6`
PERF_OUTPUT_DIR="$PERF_TEST_DIR_TMPOUT/$PERF_RAND_KEY"

echo "Task key: $PERF_RAND_KEY"

declare -a TICKS
declare -a HZS
declare -a SLAVE_IDS

#1. prepare to collect outputs

if [ -e "$OUTPUT_DIR" ];then
	echo "$OUTPUT_DIR exists, please choose another."
	exit 1
fi

#2. submit pattern files & test scripts
COUNT=1
while read mac;do
	scp $PERF_TEST_DIST_SH $PERF_TEST_USER@$mac:$PERF_TEST_DIR_BIN/perf_dist.sh
	scp $PATTERN_FILE $PERF_TEST_USER@$mac:$PERF_TEST_DIR_BIN/patterns
	#scp $PERF_TEST_TICKS_CPP $PERF_TEST_USER@$mac:$PERF_TEST_DIR_BIN/ticks.cpp
	scp $PERF_TEST_PROC_PY $PERF_TEST_USER@$mac:$PERF_TEST_DIR_BIN/perf_proc.py
	TICKS[${COUNT}]=`ssh -n $PERF_TEST_USER@$mac "getconf CLK_TCK"`
	HZS[${COUNT}]=`ssh -n $PERF_TEST_USER@$mac 'grep -ir "^CONFIG_HZ=" /boot/config-* | cut -f 2 -d = | head -1'`
	device=`ssh -n $PERF_TEST_USER@$mac 'source /etc/profile;ifconfig  | grep ^eth.*' | awk '{print $1}'`

	ssh -n $PERF_TEST_USER@$mac "mkdir -p $PERF_TEST_DIR_TMPOUT/$PERF_RAND_KEY"

	#start up nethogs, need root
	echo $device
	ssh -n root@$mac "$PERF_TEST_DIR_NETHOGS/nethogs $device > $PERF_OUTPUT_DIR/nethogs 2>&1 &"
	COUNT=$((COUNT + 1))
done< <(cat $PERF_TEST_MACHINE_FILE)

echo ${#TICKS[@]} ${HZS[@]}

COUNT=1
while read mac;do
	#echo ssh -n $PERF_TEST_USER@$mac "$PERF_TEST_DIR_BIN/perf_dist.sh $PERF_TEST_DIR_BIN/patterns $PERF_OUTPUT_DIR ${TICKS[$COUNT]} ${HZS[$COUNT]} $PERF_OUTPUT_DIR/nethogs 1& echo \$!"
	echo $mac
	echo ${HZS[$COUNT]}
	SLAVE_IDS[$COUNT]=`ssh -n $PERF_TEST_USER@$mac "source /etc/profile;nohup $PERF_TEST_DIR_BIN/perf_dist.sh $PERF_TEST_DIR_BIN/patterns $PERF_OUTPUT_DIR ${TICKS[$COUNT]} ${HZS[$COUNT]} $PERF_OUTPUT_DIR/nethogs 1 </dev/null >/dev/null 2>&1 & echo \\$!"`
	COUNT=$((COUNT + 1))
done< <(cat $PERF_TEST_MACHINE_FILE)

echo ${SLAVE_IDS[@]}
#set +x
