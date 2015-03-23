/*
 * =====================================================================================
 *
 *       Filename:  sssp_test.cpp
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
#include <limits>
#include "config.h"

int& get_nr_combined(){
	static int nr_combined = 0;
	return nr_combined;
}

struct vertex_data : public graphlab::IS_POD_TYPE{
	double value;

	vertex_data(double _d = std::numeric_limits<double>::max()):value(_d){}
	void save(graphlab::oarchive& oarc) const{
		oarc << value;
	}

	void load(graphlab::iarchive& iarc){
		iarc >> value;
	}
};

typedef graphlab::distributed_graph<vertex_data, double> graph_type;

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

	graph.add_vertex(id, vertex_data());

	while(true){
		start = end + 1;
		end = start;
		for(;*end && *end != SECOND_SPLIT;++end);

		if(!*end){ //specially for those has no edges
			break;
		}


		//id first
		graphlab::vertex_id_type to_id;
		to_id = (graphlab::vertex_id_type)strtol(start, NULL, 10);

		
		//then the value	
		start = end + 1;
		for(end = start;*end && *end != FIRST_SPLIT && *end != SECOND_SPLIT;++end);
		double value = strtod(start, NULL);


		if(id != to_id){
			graph.add_edge(id, to_id, value);
		}

		for(;*end && *end != FIRST_SPLIT;++end);

		if(!*end || (*end == FIRST_SPLIT && !*(end + 1))){
			break;
		}
	}

	return true;
}

class scatter_type : public graphlab::IS_POD_TYPE{
public:
	scatter_type(double _v = std::numeric_limits<double>::max()):value(_v){}
	scatter_type& operator+=(const scatter_type& other){
		value = std::min(value, other.value);
		return *this;
	}
public:
	double value;
};

class sssp_program:
		public graphlab::ivertex_program<graph_type, graphlab::empty, scatter_type>,
		public graphlab::IS_POD_TYPE{
private:
	double value;
	bool changed;	
public:
	void init(icontext_type& context, const vertex_type& vertex, const scatter_type& msg){
		value = msg.value;		
	}

	edge_dir_type gather_edges(icontext_type& context, const vertex_type& vertex) const{
		return graphlab::NO_EDGES;
	}

	void apply(icontext_type& context, vertex_type& vertex, const graphlab::empty& empty){
		changed = false;
		if(vertex_data().value > value){
			changed = true;	
			vertex.data().value = value;
		}
	}

	edge_dir_type scatter_edges(icontext_type& context, const vertex_type& vertex) const{
		if(changed){
			return graphlab::OUT_EDGES;
		}

		return graphlab::NO_EDGES;
	}

	void scatter(icontext_type& context, const vertex_type& vertex, edge_type& edge) const{
		const vertex_type& target = edge.target();	
		double newd = vertex.data().value + edge.data();		
		if(target.data().value > newd){
			const scatter_type msg(newd);
			context.signal(target, msg);
		}
		else{
			int& nr_combined = get_nr_combined();
			atomic_incre<int>(&nr_combined, 1);
		}
	}
};

class graph_writer{
public:
	std::string save_vertex(graph_type::vertex_type v) {
		std::ostringstream oss;
		oss << v.id() << "\t" << v.data().value << "\n";		
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

    graphlab::distributed_control dc;
    dc.cout() << "sssp\n";
    graph_type graph(dc);

    long input_start = get_time_millis();
    graph.load(input.c_str(), line_parser);
    graph.finalize();
    long input_end = get_time_millis();


    dc.cout() << "input time = " << (input_end - input_start) << '\n';

    //std::string prefix("/tmp/a");
    //std::string format("tsv");
    //graph.save_format(prefix, format, false, 1);

    graphlab::omni_engine<sssp_program> engine(dc, graph, get_sync());

    engine.signal(START_VERTEX, scatter_type(0));

    long start = get_time_millis();
    engine.start();
    long end = get_time_millis();

    dc.cout() << "time = " << (end - start) << '\n';

    graph.save(output.c_str(), graph_writer(), false, true, false);

	do_gaoyun_out();
	get_gaoyun_out() << "omitted_message = " << get_nr_combined() << std::endl;

    graphlab::mpi_tools::finalize();
    return 0;
}               /* ----------  end of function main  ---------- */
