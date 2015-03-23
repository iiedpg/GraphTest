#!/usr/bin/env bash


BASE_DIR=`dirname $BASH_SOURCE`/..;
BASE_DIR=`cd $BASE_DIR;pwd`;

WEBGRAPH_DIR=${BASE_DIR}/webgraph/webgraph-3.4.0

TRANSFORMER_PROJECT="$BASE_DIR/transformer"
WEBGRAPH_CP=$TRANSFORMER_PROJECT/bin:$WEBGRAPH_CP
TRANSFORMER_CLASS="main.Transformer"

SUFFICES_FILE="$BASE_DIR/bin/suffices"

WEBGRAPH_CP="$WEBGRAPH_DIR/webgraph-3.4.0.jar"
for f in `ls $WEBGRAPH_DIR/dep/*.jar`;do
	WEBGRAPH_CP=$f:$WEBGRAPH_CP
done
WEBGRAPH_CP=$TRANSFORMER_PROJECT/bin:$WEBGRAPH_CP

DOWALOAD_BASE="http://data.law.di.unimi.it/webdata"
