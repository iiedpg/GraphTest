/*
 * =====================================================================================
 *
 *       Filename:  gaoyun.hpp
 *
 *    Description:  To define a list of measurement method
 * *        Version:  1.0
 *        Created:  05/27/2014 05:53:30 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  GaoYun (GY), gaoyunhenhao@gmail.com
 *   Organization:  
 *
 * =====================================================================================
 */

#ifndef  GRAPHLAB_GAOYUN_HPP_INC
#define  GRAPHLAB_GAOYUN_HPP_INC

#include <string>
#include <sstream>
#include <fstream>
#include <iostream>
#include <sys/types.h>
#include <unistd.h>
#include <time.h>
#include <vector>
#include <pthread.h>
#include <tr1/unordered_map>
#include <sys/time.h>
#include <sys/resource.h>

inline std::string& get_base_dir(){
	static std::string base_dir;

	return base_dir;
}

template <class T>
inline void atomic_incre(T* from, T value){
	__sync_fetch_and_add(from, value);	
}

inline int& get_nr_iter(){
	static int nr_iter = 0;
	return nr_iter;
}

inline void incre_nr_iter(int to_incre = 1){
	int& nr_iter = get_nr_iter();
	atomic_incre(&nr_iter, to_incre);
}

inline long get_time_millis(){
    struct timespec t;  
    clock_gettime(CLOCK_REALTIME, &t);
    return t.tv_sec * 1000 + t.tv_nsec / 1000000;
}

inline long get_time_us(){
    struct timespec t;  
    clock_gettime(CLOCK_REALTIME, &t);
    return t.tv_sec * 1000000 + t.tv_nsec / 1000;
}


class TimerContext{
public:
	TimerContext():last_start(0), last_accum(0){
	}

	void multiStart(){
		last_accum = 0;
	}

	void multiPause(){
		long current = get_time_millis();
		last_accum += (current - last_start);
	}

	void multiContinue(){
		last_start = get_time_millis();
	}

	void multiEnd(){
		//assume pause and continue is paired		
		times.push_back(last_accum);	
	}

	void pushValue(long value){
		times.push_back(value);
	}

	std::vector<long> times;
	long last_start;
	long last_accum;
};

inline std::tr1::unordered_map<std::string, TimerContext>& get_timer_contexts(){
	static std::tr1::unordered_map<std::string, TimerContext> contexts;

	return contexts;
}

inline TimerContext& get_timer_context(const char* name){
	std::tr1::unordered_map<std::string, TimerContext>& contexts = get_timer_contexts();

	TimerContext& context = contexts[name];	
	return context;
}

inline size_t& get_mess_count(){
	static size_t mess_count = 0;
	return mess_count;
}

inline void incre_mess_count(size_t count = 1){
	atomic_incre<size_t>(&get_mess_count(), count);	
}


inline std::ostream& get_gaoyun_out(){
	static std::ofstream output;
	static bool inited = false;

	if(!inited){
		std::ostringstream oss;
		pid_t pid = getpid();
		oss << get_base_dir() << "/" << pid;

		output.open(oss.str().c_str());

		inited = true;
	}

	return output;
}


inline void close_gaoyun_out(){
	std::ofstream& out = dynamic_cast<std::ofstream&>(get_gaoyun_out());
	out.close();
}


//the definition of thread_local
namespace bsp{namespace multi_thread{

typedef void (*TLDestroyFunc)(void*);

template <class T>
void delete_destroy(void*);

template <class T>
class ThreadLocal{
public:
	ThreadLocal(TLDestroyFunc func = delete_destroy<T>);
	~ThreadLocal();

	T* get();
	void set(T* t);
private:
	pthread_key_t key;
};

}}

template <class T>
inline void bsp::multi_thread::delete_destroy(void* obj){
	printf("tls destroy called \n");
	fflush(stdout);
	delete (T*)obj;
}

template <class T>
inline bsp::multi_thread::ThreadLocal<T>::ThreadLocal(TLDestroyFunc func){
	pthread_key_create(&key, func);
}

template <class T>
inline bsp::multi_thread::ThreadLocal<T>::~ThreadLocal(){
	pthread_key_delete(key);
}

template <class T>
inline T* bsp::multi_thread::ThreadLocal<T>::get(){
	T* data = (T*)pthread_getspecific(key);
	return data;
}

template <class T>
inline void bsp::multi_thread::ThreadLocal<T>::set(T* d){
	pthread_setspecific(key, d);
}


class ThreadContext{
public:
	ThreadContext(pthread_t _id = 0):myid(_id), first(0), last(0),
			total_eval(0), total_lock(0), eval_times(0),
			phil_not_ready_times(0), lock_time(0),
			ds(0), ds_time(0),
			ds_start(0), ds_for_reply(0),
			ds_for_reply_2(0)
	{}

	pthread_t myid;
	long first;
	long last;
	long total_eval;
	long total_lock;
	long eval_times;

	long last_start;
	long last_add;

	long phil_not_ready_times;
	long lock_time;

	long ds;

	long ds_time;
	long ds_start;
	long ds_for_reply;
	long ds_for_reply_2;

	long utime;
	long stime;
};

inline std::vector<ThreadContext*>& get_all_thread_contexts(){
	static std::vector<ThreadContext*> all_contexts;
	return all_contexts;
}

inline pthread_mutex_t* get_thread_contexts_lock(){
	static pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
	return &lock;
}

static inline void delete_thread_context(void* obj){
	ThreadContext* tc = (ThreadContext*)obj;
	//get_gaoyun_out() << tc->myid << ": eval = " << tc->total_eval << ",lock = " 
	//		<< tc->total_lock << ",eval_times = " << tc->eval_times 
	//		<< "\n";
	delete tc;
}

inline ThreadContext& get_my_thread_context(){	
	static bsp::multi_thread::ThreadLocal<ThreadContext> local(delete_thread_context);
	ThreadContext* tc = local.get();
	if(tc == NULL){
		tc = new ThreadContext(pthread_self());	
		local.set(tc);

		pthread_mutex_t* lock = get_thread_contexts_lock();
		pthread_mutex_lock(lock);
		get_all_thread_contexts().push_back(tc);
		pthread_mutex_unlock(lock);
	}

	return *tc;
}

inline void update_usgae(){
	struct rusage us;
	getrusage(RUSAGE_THREAD, &us);
	ThreadContext& tc = get_my_thread_context();
	tc.utime = us.ru_utime.tv_sec * 1000 + us.ru_utime.tv_usec / 1e3;
	tc.stime = us.ru_stime.tv_sec * 1000 + us.ru_stime.tv_usec / 1e3;
}

inline void do_gaoyun_out(){
	std::ostream& gy_out = get_gaoyun_out();

	gy_out << "message_count = " << get_mess_count() << std::endl; 

	//contexts
	std::tr1::unordered_map<std::string, TimerContext>& contexts = get_timer_contexts();
	std::tr1::unordered_map<std::string, TimerContext>::iterator mi = contexts.begin();
	for(;mi != contexts.end();++mi){
		std::vector<long>& times = (mi->second).times;
		for(size_t i = 0;i < times.size();++i){
			gy_out << "===" << mi->first << ":" << i << ":" << times[i] << std::endl;
		}
	}

	std::vector<ThreadContext*> all_contexts = get_all_thread_contexts();
	get_gaoyun_out() << "total thread contexts: " << all_contexts.size() << "\n";

	std::vector<ThreadContext*>::iterator vi = all_contexts.begin();
	for(;vi != all_contexts.end();++vi){
		ThreadContext* tc = *vi;
		get_gaoyun_out() << tc->myid << ": eval = " << tc->total_eval << ",lock = " 
				<< tc->total_lock << ",eval_times = " << tc->eval_times  
				<< ", first = " << tc->first << ", last = " << tc->last
				<< ",interval = " << (tc->last - tc->first)
				<< ",not_ready = " << tc->phil_not_ready_times
				<< ",lock_time = " << tc->lock_time
				<< ",ds = " << tc->ds
				<< ",ds_time = " << tc->ds_time
				<< ",ds_for_reply = " << tc->ds_for_reply
				<< ",ds_for_reply_2 = " << tc->ds_for_reply_2
				<< ",utime = " << tc->utime
				<< ",stime = " << tc->stime
				<< "\n";	
	}
}


#endif   /* ----- #ifndef GRAPHLAB_GAOYUN_HPP_INC  ----- */
