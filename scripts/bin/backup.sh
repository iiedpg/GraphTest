#!/usr/bin/env bash

. `dirname $0`/config.sh

cd $BASE_DIR/
d=`date +"%Y%m%d%H%M"`


set -x
tar cvzf "backup/perf_${d}.tgz" bin conf job_def
cd backup

scpforall.sh perf_${d}.tgz

set +x
