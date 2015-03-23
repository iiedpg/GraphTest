#!/usr/bin/env bash


function set_classpath(){
	local lib=$1
	if [ -z "$lib" ] || [ ! -d "$lib" ];then
		return 1	
	fi

	CLASSPATH='.'
	for f in `ls $lib`;do
		CLASSPATH=$lib/$f:$CLASSPATH
	done
}


function compile_eclipse_java(){
	local dir=$1	
	if [ -z "$dir" ] || [ ! -d "$dir" ];then
		return 1
	fi

	set_classpath $dir/lib
	set -x

	if [ -n "$2" ];then
		for f in `ls $2/`;do		
			javac -sourcepath $dir/src -d $dir/bin -classpath $CLASSPATH $2/$f
		done
	else
		for f in `grep -lir "void main(" $dir/src/`;do	 
			javac -sourcepath $dir/src -d $dir/bin -classpath $CLASSPATH $f
		done
	fi

	jar -cvf $dir/deploy.jar -C $dir/bin .
	set +x
}


function compile_eclipse_scala(){
	local dir=$1	
	if [ -z "$dir" ] || [ ! -d "$dir" ];then
		return 1
	fi

	set_classpath $dir/lib
	set -x

	if [ -n "$2" ];then
		for f in `ls $2/`;do		
			scalac -sourcepath $dir/src -d $dir/bin -classpath $CLASSPATH $2/$f
		done
	else
		for f in `grep -lir "void main(" $dir/src/`;do	 
			scalac -sourcepath $dir/src -d $dir/bin -classpath $CLASSPATH $f
		done
	fi

	jar -cvf $dir/deploy.jar -C $dir/bin .
	set +x
}
