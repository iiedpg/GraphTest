#!/usr/bin/env python


import sys

INSERT_SQL='''
insert into `values` (frame, nr_split, total_turns,input, turns, tasks, property, value, is_map) values ("%s", %s, %s, "%s", %s, %s, "%s", %s, "%s");
'''


if __name__ == "__main__": 
    frame = sys.argv[1]
    nr_split = sys.argv[2]
    total_turn = sys.argv[3]

    turns = sys.argv[4]

    is_map = sys.argv[5]

    t_input = sys.argv[6]

    input = sys.argv[7] 

    in_f = open(input)
    
    while True:
        title_line = in_f.readline()
        
        if title_line is None or title_line.strip() == "":
            break

            
        value_line = in_f.readline()

        title_parts = title_line.split()
        value_parts = value_line.split()

        tasks = value_parts[0]

        for i in xrange(1, len(title_parts)):
            property = title_parts[i]
            value = value_parts[i]
            state = INSERT_SQL % (frame, nr_split, total_turn, t_input, turns, tasks, property, value, is_map)
            print state.strip()

    in_f.close()
