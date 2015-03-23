/*
 * =====================================================================================
 *
 *       Filename:  mrr.c
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  09/07/2014 09:33:06 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  GaoYun (GY), gaoyunhenhao@gmail.com
 *   Organization:  
 *
 * =====================================================================================
 */
#include <stdlib.h>
#include<stdio.h>
void main(int argc,char *argv[])
{
	int i;
	double result=0.0;
	for(i=1;i<argc;i++){
		result+=(1.0/atof(argv[i]));
	}
	printf("%.9lf\n",result/10);
}

