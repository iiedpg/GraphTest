#!/usr/bin/env bash


. `dirname $0`/config.sh


set -x
mysql -u root -p000000 < $BASE_DIR/bin/create_db.sql

for f in `find $BASE_DIR/output/ -name 'insert.sql'`;do
	mysql -u root -p000000 < $f
done
set +x
