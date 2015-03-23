/*
 * =====================================================================================
 *
 *       Filename:  pagerank_test.cpp
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  05/19/2014 10:13:51 AM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  GaoYun (GY), gaoyunhenhao@gmail.com
 *   Organization:  
 *
 * =====================================================================================
 */
#include <iostream>
#include <graphlab.hpp>
#include <cstdlib>
#include <cassert>
#include <cmath>
#include <sstream>

#include "config.h"

int& get_max_turn(){
	static int turn;
	return turn;	
}

struct vertex_value : public graphlab::IS_POD_TYPE{
	vertex_value():sum(0){};
	int sum;
};
	
typedef graphlab::distributed_graph<vertex_value, graphlab::empty> graph_type;

const char FIRST_SPLIT = ':';
const char SECOND_SPLIT = ',';

bool line_parser(graph_type& graph, 
				const std::string& filename,
				const std::string& textline){
	const char* start = textline.c_str();
	const char* end = textline.c_str();
	for(;*end && *end != FIRST_SPLIT;++end);
	assert(*end);

	graphlab::vertex_id_type id;
	id = (graphlab::vertex_id_type)strtol(start, NULL, 10);

	graph.add_vertex(id);

	while(true){
		start = end + 1;
		end = start;
		for(;*end && *end != SECOND_SPLIT;++end);

		if(!*end){
			break;
		}

		graphlab::vertex_id_type to_id;

		to_id = (graphlab::vertex_id_type)strtol(start, NULL, 10);
		

		if(id != to_id){
			graph.add_edge(id, to_id);
		}
		
		for(end = end + 1;*end && *end != FIRST_SPLIT;++end);

		if(!*end || (*end == FIRST_SPLIT && !*(end + 1))){
			break;
		}
	}

	return true;
}

class standard_program:
			public graphlab::ivertex_program<graph_type, int>,
			public graphlab::IS_POD_TYPE{
public:		
	edge_dir_type gather_edges(icontext_type& context, const vertex_type& vertex) const{
		return graphlab::IN_EDGES;
	}

	int gather(icontext_type& context, const vertex_type& vertex, 
						edge_type& edge) const{
		return edge.source().id();
	}

	void apply(icontext_type& context, vertex_type& vertex, 
					const int& total){
		vertex.data().sum = total;

		if(context.iteration() < get_max_turn() - 1){
			context.signal(vertex);
		}
	}

	edge_dir_type scatter_edges(icontext_type& context, const vertex_type& vertex) const {
		return graphlab::NO_EDGES;
	}
};

class graph_writer{
public:
	std::string save_vertex(graph_type::vertex_type v) {
		std::ostringstream oss;
		oss << v.id() << "\t" << v.data().sum << "\n";		
		return oss.str();
	}

	std::string save_edge(graph_type::edge_type e){
		return "";
	}
};


/* 
 * ===  FUNCTION  ======================================================================
 *         Name:  main
 *  Description:  
 * =====================================================================================
 */
int main ( int argc, char *argv[] ) {
    string input;
    string output;
    int turns;

    parse_args(input, output, turns, argc, argv);

    graphlab::mpi_tools::init(argc, argv);
    get_max_turn() = turns;

    graphlab::distributed_control dc;
    dc.cout() << "standard " << get_max_turn() << "\n";
    graph_type graph(dc);

    std::string path;

    long input_start = get_time_millis();
    graph.load(input.c_str(), line_parser);
    graph.finalize();
    long input_end = get_time_millis();
    dc.cout() << "input time = " << (input_end - input_start) << '\n';

    //std::string prefix("/tmp/a");
    //std::string format("tsv");
    //graph.save_format(prefix, format, false, 1);

    graphlab::omni_engine<standard_program> engine(dc, graph, "sync");
    engine.signal_all();


    long start = get_time_millis();
    engine.start();
    long end = get_time_millis();

    dc.cout() << "time = " << (end - start) << '\n';

    graph.save(output.c_str(), graph_writer(), false, true, false);

	do_gaoyun_out();

    graphlab::mpi_tools::finalize();
    return 0;
}               /* ----------  end of function main  ---------- */
