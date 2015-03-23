#!/usr/bin/env python

import sys

if __name__ == "__main__":

    ds = {}

    for line in sys.stdin:
        line = line.strip()
        parts = line.split(':')
        id = int(parts[0])

        for i in xrange(1, len(parts)):
            p = parts[i] 

            if p.strip() == "":
                continue

            parts_2 = p.split(',')

            to_id = int(parts_2[0])
            
            if id <= to_id:
                a = (id, to_id)
            else:
                a = (to_id, id)

            oa = ds.get(a)
            
            if oa is not None:
                continue
            
            ds[a] = 1

            print "%d,%d" % (id, to_id)

