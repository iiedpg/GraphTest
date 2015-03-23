/*
 * =====================================================================================
 *
 *       Filename:  config.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  05/19/2014 05:19:51 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  GaoYun (GY), gaoyunhenhao@gmail.com
 *   Organization:  
 *
 * =====================================================================================
 */


#ifndef  CONFIG_INC
#define  CONFIG_INC

#include <string>
#include "graphlab.hpp"

using std::string;

const int ARGV_INDEX_INPUT = 1;
const int ARGV_INDEX_OUTPUT = 2;
const int ARGV_INDEX_TURNS = 3;
const int ARGV_INDEX_BASE_DIR = 4;
const int ARGV_INDEX_SYNC = 5;

void get_hdfs_path(string& re, const char* path){
	re = "hdfs://localhost:9000/";
	re += path;
}

const int START_VERTEX = 1;

string& get_sync(){
	static string sync;
	return sync;
}

void parse_args(string& input, string& output, int& turns, int argc, char* argv[]){
	get_hdfs_path(input, argv[ARGV_INDEX_INPUT]);	
	get_hdfs_path(output, argv[ARGV_INDEX_OUTPUT]);
	turns = strtol(argv[ARGV_INDEX_TURNS], NULL, 10);

	get_base_dir() = argv[ARGV_INDEX_BASE_DIR];

	get_sync() = argv[ARGV_INDEX_SYNC];
}

#endif   /* ----- #ifndef CONFIG_INC  ----- */
