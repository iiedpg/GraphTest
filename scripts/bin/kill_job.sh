#!/usr/bin/env bash

. `dirname $0`/config.sh

job=$1

bd=$BASE_DIR/output/bsp
job_output=`ls -rt $bd | tail -1`
if [ -z "$job" ];then
	job=`cat $BASE_DIR/output/bsp/$job_output/dout | grep "job id = " | awk '{print $NF}'`
fi
echo $job

for m in `cat $BSP_DIR/conf/slaves`;do
	if [ $m != \#* ];then
		ssh -n $m "ps aux | grep task$job | grep -v grep"
		ssh -n $m "ps aux | grep task$job | grep -v grep | awk '{print \$2}' | xargs kill -9"
		ssh -n $m "ps aux | grep task$job | grep -v grep"
		echo =========
	fi
done
