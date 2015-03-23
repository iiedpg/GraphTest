/*
 * =====================================================================================
 *
 *       Filename:  mst.cpp
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  05/29/2014 01:24:25 PM
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
#include <utility>
#include <vector>
#include <algorithm>
#include "config.h"

using std::vector;
using std::pair;
using std::make_pair;

typedef std::numeric_limits<int> INT_LIMITS;
typedef std::numeric_limits<double> DOUBLE_LIMITS;

struct picking_min_message : graphlab::IS_POD_TYPE{
public:
	picking_min_message(int _src = INT_LIMITS::max(), int _dest = INT_LIMITS::max(), 
							int _dest_root = INT_LIMITS::max(), double _value = DOUBLE_LIMITS::max()):
						src(_src), dest(_dest), dest_root(_dest_root), value(_value){}	

	picking_min_message& operator+=(const picking_min_message& other){
		if(other < *this){
			src = other.src;
			dest = other.dest;
			dest_root = other.dest_root;
			value = other.value;
		}

		return *this;
	}

	picking_min_message& operator=(const picking_min_message& other){
		src = other.src;
		dest = other.dest;
		dest_root = other.dest_root;
		value = other.value;			

		return *this;
	}

	friend bool operator<(const picking_min_message& a, const picking_min_message& b){
		if(a.value != b.value){
			return a.value < b.value;
		}

		//now a.value == b.value
		if(a.dest_root != b.dest_root){
			return a.dest_root < b.dest_root;
		}

		if(a.dest != b.dest){
			return a.dest < b.dest;
		}	

		if(a.src != b.src){
			return a.src < b.src;	
		}

		//now they are the same edge
		return false;
	}

public:
	int src;
	int dest;
	int dest_root;
	double value;
};


struct picked_edge_type : graphlab::IS_POD_TYPE{
public:
	picked_edge_type(int _s = INT_LIMITS::max(), int _d = INT_LIMITS::max(), 
						double _v = DOUBLE_LIMITS::max()):
				src(_s), dest(_d), value(_v){}
public:
	int src;
	int dest;
	double value;
};

struct vertex_data{
public:
	vertex_data(int vid = 0):
			tree_root(vid), picked(false),
			gathered(false), new_tree_root(INT_LIMITS::max()),
			is_super(true), know_super(false), is_last_super(true){}

	void save(graphlab::oarchive& oarc) const{
		oarc << tree_root;
		oarc << picked;

		oarc << min_mess.src;
		oarc << min_mess.dest;
		oarc << min_mess.dest_root;
		oarc << min_mess.value;

		oarc << gathered;
		oarc << new_tree_root;

		oarc << is_super;
		oarc << know_super;
		oarc << is_last_super;
	
		oarc << edges.size();
		vector<picked_edge_type>::const_iterator vi = edges.begin();
		for(;vi != edges.end();++vi){
			oarc << vi->src << vi->dest << vi->value;
		}
	}

	void load(graphlab::iarchive& iarc){
		iarc >> tree_root;
		iarc >> picked;

		iarc >> min_mess.src;
		iarc >> min_mess.dest;
		iarc >> min_mess.dest_root;
		iarc >> min_mess.value;

		iarc >> gathered;
		iarc >> new_tree_root;

		iarc >> is_super;
		iarc >> know_super;
		iarc >> is_last_super;

		edges.clear();
		size_t size;
		picked_edge_type pet;
		iarc >> size;
		for(size_t i = 0;i < size;++i){
			iarc >> pet.src >> pet.dest >> pet.value;
			edges.push_back(pet);
		}
	}
public:
	int tree_root;
/*  for picking min message */
	bool picked;
	picking_min_message min_mess;
/*  for getting the new super vertex */
	bool gathered;
	int new_tree_root;

	bool is_super;
	bool know_super;
	bool is_last_super;

	vector<picked_edge_type> edges;
};

struct edge_data : public graphlab::IS_POD_TYPE{
public:
	edge_data(double _v = DOUBLE_LIMITS::max()):value(_v){}
	void save(graphlab::oarchive& oarc) const{
		oarc << value;
	}

	void load(graphlab::iarchive& iarc){
		iarc >> value;
	}
public:
	double value;
};

typedef graphlab::distributed_graph<vertex_data, edge_data> graph_type;

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

	graph.add_vertex(id, vertex_data(id));

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

		if(id < to_id){
			graph.add_edge(id, to_id, edge_data(value));
		}

		for(;*end && *end != FIRST_SPLIT;++end);

		if(!*end || (*end == FIRST_SPLIT && !*(end + 1))){
			break;
		}
	}

	return true;
}


inline graph_type::vertex_type other_side(const graph_type::edge_type& edge, const graph_type::vertex_type& one_side){
	if(edge.source().id() == one_side.id()){
		return edge.target();
	}

	return edge.source();
}

class mst_picking_min_program : 
		public graphlab::ivertex_program<graph_type, picking_min_message, picking_min_message>,
		public graphlab::IS_POD_TYPE{
public:
	picking_min_message min_mess;
	bool need_scatter;
public:
	void init(icontext_type& context, const vertex_type& vertex, const picking_min_message& msg){
		min_mess = msg;	
		need_scatter = false;
	}

	edge_dir_type gather_edges(icontext_type& context, const vertex_type& vertex) const{
		if(vertex.data().picked || vertex.data().tree_root == INT_LIMITS::max()){
			return graphlab::NO_EDGES;			
		}
		else{
			return graphlab::ALL_EDGES;	
		}
	}

	picking_min_message gather(icontext_type& context, const vertex_type& vertex, 
						edge_type& edge) const{
		picking_min_message msg;
		vertex_type dest_vertex = other_side(edge, vertex);	
		if(dest_vertex.data().tree_root != vertex.data().tree_root){
			msg.src = vertex.id();
			msg.dest = dest_vertex.id();
			msg.dest_root = dest_vertex.data().tree_root;
			msg.value = edge.data().value;
		}

		return msg;
	}

	void apply(icontext_type& context, vertex_type& vertex, const picking_min_message& my_edges){
		if(vertex.data().tree_root == INT_LIMITS::max()){
			need_scatter = false;
			return;
		}

		if(!vertex.data().picked){
			min_mess += my_edges;
		}

		need_scatter = min_mess < vertex.data().min_mess;
		
		vertex.data().min_mess += min_mess;
		vertex.data().picked = true;
	}

	edge_dir_type scatter_edges(icontext_type& context, const vertex_type& vertex) const{
		if(need_scatter){
			return graphlab::ALL_EDGES;
		}
		else{
			return graphlab::NO_EDGES;
		}
	}

	void scatter(icontext_type& context, const vertex_type& vertex, edge_type& edge) const{
		const vertex_type dest = other_side(edge, vertex);
		if(vertex.data().tree_root == dest.data().tree_root){
			context.signal(dest, vertex.data().min_mess);
		}
	}
};

class broadcast_root_message : public graphlab::IS_POD_TYPE{
public:
	broadcast_root_message():know_super(false), new_tree_root(INT_LIMITS::max()){}
	broadcast_root_message& operator+=(const broadcast_root_message& other){
		if(!know_super && other.know_super){
			know_super = other.know_super;
			new_tree_root = other.new_tree_root;
		}

		return *this;
	}
public:	
	bool know_super;
	int new_tree_root;
};

class broadcast_root_program 
			:public graphlab::ivertex_program<graph_type, broadcast_root_message, broadcast_root_message>,
			 public graphlab::IS_POD_TYPE{
private:
	broadcast_root_message bro_message;
	bool need_scatter;
public:
	void init(icontext_type& context, const vertex_type& vertex, const broadcast_root_message& msg){
		bro_message += msg;
		need_scatter = false;
	}

	edge_dir_type gather_edges(icontext_type& context, const vertex_type& vertex) const{
		if(!vertex.data().gathered && vertex.data().tree_root != INT_LIMITS::max()){
			return graphlab::ALL_EDGES;
		}
		else{
			return graphlab::NO_EDGES;
		}
	}

	broadcast_root_message gather(icontext_type& context, const vertex_type& vertex, 
						edge_type& edge) const{
		broadcast_root_message mess;
		vertex_type dest = other_side(edge, vertex);
		if(dest.data().min_mess.dest_root == vertex.data().tree_root 
						&& vertex.data().min_mess.dest_root == dest.data().tree_root){
			mess.know_super = true;
			mess.new_tree_root = std::min(dest.data().tree_root, vertex.data().tree_root);
		}

		return mess;
	}

	void apply(icontext_type& context, vertex_type& vertex, const broadcast_root_message& mess){
		if(vertex.data().tree_root == INT_LIMITS::max()){
			need_scatter = false;
			return;
		}

		if(!vertex.data().gathered){
			if(mess.know_super){
				vertex.data().new_tree_root = mess.new_tree_root;
				vertex.data().know_super = true;
				need_scatter = true;
			}
			vertex.data().gathered = true;	
		}
		else{
			if(bro_message.know_super){
				vertex.data().new_tree_root = bro_message.new_tree_root;
				vertex.data().know_super = true;
				need_scatter = true;
			}
		}
	}

	edge_dir_type scatter_edges(icontext_type& context, const vertex_type& vertex) const{
		if(need_scatter){
			return graphlab::ALL_EDGES;
		}
		else{
			return graphlab::NO_EDGES;
		}
	}

	void scatter(icontext_type& context, const vertex_type& vertex, 
						edge_type& edge) const{
		vertex_type dest = other_side(edge, vertex);
		if(!dest.data().know_super 
				&& (dest.data().tree_root == vertex.data().tree_root || 
						dest.data().min_mess.dest_root == vertex.data().tree_root)){
			broadcast_root_message new_mess;
			new_mess.know_super = vertex.data().know_super;
			new_mess.new_tree_root = vertex.data().new_tree_root;

			context.signal(dest, new_mess);
		}
	}
};

void clear_for_next_iter(graph_type::vertex_type& vertex){
	//the min end of the min_mess record it		
	graph_type::vertex_id_type src = vertex.data().min_mess.src;
	graph_type::vertex_id_type dest = vertex.data().min_mess.dest;
	double value = vertex.data().min_mess.value;

	bool found_edge = value != DOUBLE_LIMITS::max();

	//The ring of the disjoined tree should be break when saving the edges
	if(found_edge && vertex.id() == src && vertex.data().tree_root != vertex.data().new_tree_root){
		vertex.data().edges.push_back(picked_edge_type(src, dest, value));
	}

	//clear data
	vertex.data().is_last_super = vertex.data().is_super;
	vertex.data().is_super = (vertex.id() == vertex.data().new_tree_root);
	vertex.data().tree_root = vertex.data().new_tree_root;

	vertex.data().gathered = false;
	vertex.data().know_super = false;
	vertex.data().new_tree_root = INT_LIMITS::max();

	//clear min_mess
	vertex.data().picked = false;
	picking_min_message empty_message;
	vertex.data().min_mess = empty_message;
}

void count_root(const graph_type::vertex_type& vertex, int& root_count){
	root_count += vertex.data().is_super ? 1 : 0; 
}

class graph_writer{
public:
	std::string save_vertex(graph_type::vertex_type v) {
		std::ostringstream oss;
		oss << v.id() <<"(" << v.data().tree_root << ")";
		oss<< "\t" << v.data().min_mess.src << "->" << v.data().min_mess.dest << "(";
		oss << v.data().min_mess.dest_root << ")," << v.data().min_mess.value;
		oss << "\t" << v.data().know_super << "," << v.data().new_tree_root;
		oss << "\t" << (v.data().is_last_super ? "is_last_super" : "");
		oss << "\t" << (v.data().is_super ? "is_super" : "");
		oss << '\n';
		return oss.str();
	}

	std::string save_edge(graph_type::edge_type e){
		return "";
	}
};

class graph_edges_writer{
public:
	std::string save_vertex(graph_type::vertex_type v){
		std::ostringstream oss;	
		vector<picked_edge_type>::iterator vi = v.data().edges.begin();
		for(;vi != v.data().edges.end();++vi){
			oss << vi->src << "\t" << vi->dest << '\t' << vi->value << "\n";	
		}

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
    dc.cout() << "mst\n";
    graph_type graph(dc);

    long input_start = get_time_millis();
    graph.load(input.c_str(), line_parser);
    graph.finalize();
    long input_end = get_time_millis();


    //std::string prefix("/tmp/a");
    //std::string format("tsv");
    //graph.save_format(prefix, format, false, 1);
    int turn = 1;
    std::ostringstream oss;

    long start = get_time_millis();

    while(true){
        //picking min
        graphlab::omni_engine<mst_picking_min_program> engine(dc, graph, "sync");
        engine.signal_all(picking_min_message());
        engine.start();

        //oss.str("");
        //oss << "hdfs://192.168.255.118:9000/output/tmp/gl/" << turn << "/1/";
        //graph.save(oss.str().c_str(), graph_writer(), false, true, false);

        graphlab::omni_engine<broadcast_root_program> bro_engine(dc, graph, "sync");
        bro_engine.signal_all(broadcast_root_message());
        bro_engine.start();

        //oss.str("");
        //oss << "hdfs://192.168.255.118:9000/output/tmp/gl/" << turn << "/2/";
        //graph.save(oss.str().c_str(), graph_writer(), false, true, false);

        graph.transform_vertices(clear_for_next_iter);

        //oss.str("");
        //oss << "hdfs://192.168.255.118:9000/output/tmp/gl/" << turn << "/3/";
        //graph.save(oss.str().c_str(), graph_writer(), false, true, false);

        int root_count = graph.fold_vertices<int>(count_root);
        dc.cout() << "root_count = " << root_count << "\n";
        if(root_count <= 1){
            dc.cout() << "only one root or has to leave\n";
            break;
        }

        turn += 1;
    }

    long end = get_time_millis();
    dc.cout() << "time = " << (end - start) << '\n';


    graph.save(output.c_str(), graph_edges_writer(), false, true, false);

	do_gaoyun_out();

    graphlab::mpi_tools::finalize();
    return 0;
}               /* ----------  end of function main  ---------- */
