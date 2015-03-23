#!/usr/bin/env bash

. `dirname $0`/config.sh
. `dirname $0`/utils.sh

set -x
compile_transformer
download_dataset $1
transform_dataset $1
set +x
