#!/usr/bin/env bash

cd ./giraph-core
mvn clean
mvn -Phadoop_0.23 package -DskipTests -DskipTetst=true -Dmaven.test.skip=true
cd ..
cp ./giraph-core/target/giraph-1.0.0-for-hadoop-0.23.1-jar-with-dependencies.jar ./giraph-1.0.0-for-hadoop-0.23.1-jar-with-dependencies.jar
