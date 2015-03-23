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
#include <limits>
#include "config.h"

struct vertex_data : public graphlab::IS_POD_TYPE{
	int value;
	int father;

	vertex_data(int _d = std::numeric_limits<int>::max(), int _father = -1):value(_d), father(_father){}
	void save(graphlab::oarchive& oarc) const{
		oarc << value << father;
	}

	void load(graphlab::iarchive& iarc){
		iarc >> value >> father;
	}
};

typedef graphlab::distributed_graph<vertex_data, graphlab::empty> graph_type;

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

		if(!*end){
			break;
		}

		graphlab::vertex_id_type to_id;
		to_id = (graphlab::vertex_id_type)strtol(start, NULL, 10);

		graph.add_edge(id, to_id);
		
		for(end = end + 1;*end && *end != FIRST_SPLIT;++end);

		if(!*end || (*end == FIRST_SPLIT && !*(end + 1))){
			break;
		}
	}

	return true;
}


class scatter_type : public graphlab::IS_POD_TYPE{
public:
	scatter_type(int _v = std::numeric_limits<int>::max(), int _father = -1):value(_v), father(_father){}
	scatter_type& operator+=(const scatter_type& other){
		if(value > other.value){
			value = other.value;
			father = other.father;
		}

		return *this;
	}
public:
	int value;
	int father;
};

class bfs_program:
		public graphlab::ivertex_program<graph_type, graphlab::empty, scatter_type>,
		public graphlab::IS_POD_TYPE{
private:
	int value;
	int father;
	bool changed;	
public:
	void init(icontext_type& context, const vertex_type& vertex, const scatter_type& msg){
		value = msg.value;		
		father = msg.father;
	}						

	edge_dir_type gather_edges(icontext_type& context, const vertex_type& vertex) const{
		return graphlab::NO_EDGES;
	}

	void apply(icontext_type& context, vertex_type& vertex, const graphlab::empty& empty){
		changed = false;
		if(vertex.data().value > value){
			changed = true;	
			vertex.data().value = value;
			vertex.data().father = father;
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
		double newd = vertex.data().value + 1;		
		if(target.data().value > newd){
			const scatter_type msg(newd, vertex.id());
			context.signal(target, msg);
		}
	}
};

class graph_writer{
public:
	std::string save_vertex(graph_type::vertex_type v) {
		std::ostringstream oss;
		oss << v.id() << "\t" << v.data().value << "\t" << v.data().father << "\n";		
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
	dc.cout() << "bfs\n";
	graph_type graph(dc);
	graph.load(input.c_str(), line_parser);
	graph.finalize();

	//std::string prefix("/tmp/a");
	//std::string format("tsv");
	//graph.save_format(prefix, format, false, 1);
	
	graphlab::omni_engine<bfs_program> engine(dc, graph, "sync");	

	engine.signal(START_VERTEX, scatter_type(0, START_VERTEX));

	engine.start();

	graph.save(output.c_str(), graph_writer(), false, true, false);

	graphlab::mpi_tools::finalize();
	return 0;
}				/* ----------  end of function main  ---------- */
