#!/usr/bin/env python

import sys

class Stage:
    def __init__(self):
        self.time = 0
        self.rdd = 0
        self.id = 0

class Turn:
    def __init__(self):
        self.id = 0
        self.start = 0
        self.end = 0
        self.stages = []
        self.total_time = 0.0

if __name__ == "__main__":
    dep_list_f = sys.argv[1]
    stage_to_rdd_f = sys.argv[2]
    stage_time_f = sys.argv[3]

    statis_f = sys.argv[4]

    stages = {}
    with open(stage_to_rdd_f) as srdds:
        for srdd in srdds:
            parts = srdd.split()
            stage = Stage()            
            stage.id = int(parts[0])
            stage.rdd = int(parts[1])

            stages[stage.id] = stage

    with open(stage_time_f) as stimes:
        for stime in stimes:
            parts = stime.split()

            id = int(parts[0])
            time = float(parts[1])
            
            stages[id].time = time

    rdd_names = {}
    turns = [] 
    turn = Turn()
    next_id = 0
    last_rdd_id = 0
    with open(dep_list_f) as lines:
        for line in lines:
            parts = line.split('@')
            name_parts = parts[0].split(',')

            rdd_id = int(name_parts[0])
            last_rdd_id = rdd_id
            name = name_parts[1]

            if name != "null":
                rdd_names[rdd_id] = name    

            if name.startswith("message"):
                turn.end = rdd_id - 2 #regulations from reduceWithKey
                turns.append(turn)

                turn = Turn()
                turn.start = rdd_id - 1
                next_id += 1
                turn.id = next_id

        turn.end = last_rdd_id
        turns.append(turn)

    rdd_to_turns = {}
    stage_to_turns = {}
 
    for turn in turns: 
        for rdd_id in xrange(turn.start, turn.end + 1):
            rdd_to_turns[rdd_id] = turn.id
 
    for i, stage in stages.items():
        turn_id = rdd_to_turns[stage.rdd]
        turn = turns[turn_id]
        turn.stages.append(stage)

#    for i in xrange(1, len(rdd_to_turns)):
#        print i,rdd_to_turns[i]

    with open("/tmp/hehe", "w") as hehe:
        for turn in turns:
            for s in turn.stages:
                print>> hehe, turn.id, s.id, s.time
            print ""

    with open("/tmp/hehe2", "w") as hehe:
        for i, stage in stages.items():
            print >> hehe, i, stage.rdd, rdd_to_turns[stage.rdd]
        
    for turn in turns:
        for s in turn.stages:
            turn.total_time += s.time

        print "-1 %d\t%f\ta" % (turn.id, turn.total_time)
    with open(statis_f) as statises:
        for line in statises:
            parts = line.split()       

            name_parts = parts[1].split(',')

            try:
                task_id = int(name_parts[0])
                rdd_id = int(name_parts[1])
            except e:
                raise e

            turn = turns[rdd_to_turns[rdd_id]]

            name = rdd_names.get(rdd_id)
            if name is None:
                name = str(rdd_id - turn.start)

            if len(name_parts) >= 3:
                name = name + "_" + name_parts[2]
            
            if len(parts) >= 4:
                print "%d ====%s:%d\t%s\t%s" % (task_id, name, turn.id, parts[2], parts[3]) 


