#!/usr/bin/env bash

function compile_transformer(){
	set -x
	javac -classpath $WEBGRAPH_CP -sourcepath $TRANSFORMER_PROJECT/src -d $TRANSFORMER_PROJECT/bin $TRANSFORMER_PROJECT/src/main/Transformer.java
	javac -classpath $WEBGRAPH_CP -sourcepath $TRANSFORMER_PROJECT/src -d $TRANSFORMER_PROJECT/bin $TRANSFORMER_PROJECT/src/main/SizeTransformer.java
	set +x
}

function download_dataset(){
	local ds_name=$1
	
	set -x
	if [ -e "$BASE_DIR/$ds_name" ];then 
		echo "dir $BASE_DIR/$ds_name exists, please check.";exit 1;
	fi

	mkdir -p $BASE_DIR/$ds_name
	pushd $BASE_DIR/$ds_name > /dev/null 2>&1
	for s in `cat $SUFFICES_FILE`;do
		wget ${DOWALOAD_BASE}/${ds_name}/${ds_name}.${s}
	done
	set +x
	popd > /dev/null 2>&1
}

function transform_dataset(){
	local ds_name=$1
	set -x
	pushd $BASE_DIR/$ds_name > /dev/null 2>&1
	java -classpath $WEBGRAPH_CP it.unimi.dsi.webgraph.BVGraph -o -O -L $BASE_DIR/$ds_name/$ds_name
	java -classpath $WEBGRAPH_CP main.Transformer ${BASE_DIR}/${ds_name}/${ds_name} > ${BASE_DIR}/${ds_name}/${ds_name}.txt
	popd > /dev/null 2>&1
	set +x
}

function transform_size(){
	local ds_name=$1		
	set -x
	pushd $BASE_DIR/$ds_name > /dev/null 2>&1
	java -classpath $WEBGRAPH_CP main.SizeTransformer ${BASE_DIR}/${ds_name}
	popd > /dev/null 2>&1
	set +x
}
