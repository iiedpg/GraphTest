/*
 * =====================================================================================
 *
 *       Filename:  ticks.cpp
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  12/13/2013 09:19:38 AM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  GaoYun (GY), gaoyunhenhao@gmail.com
 *   Organization:  
 *
 * =====================================================================================
 */

#include <unistd.h>
#include <time.h>
#include <limits.h>
#include <iostream>
using namespace std;

/* 
 * ===  FUNCTION  ======================================================================
 *         Name:  main
 *  Description:  
 * =====================================================================================
 */
int main ( int argc, char *argv[] ) {
	long ticks = sysconf(_SC_CLK_TCK);
	cout << ticks << endl;
	return 0;
}				/* ----------  end of function main  ---------- */
