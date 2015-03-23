#!/usr/bin/env python

import sys

def put_in_map(m, k, v):
    vs = m.get(k)
    if vs is None:
        m[k] = [v]
    else:
        vs.append(v)

if __name__ == "__main__":
    map = {}
    #for line in sys.stdin:
    #    print line

    kline = None
    for line in sys.stdin:
        if kline is None:
            kline = line
            continue

        vline = line 

        ks = kline.split()
        vs = vline.split()

        for i in xrange(len(ks)):
            put_in_map(map, (ks[i], i), vs[i])

        kline = None

    for k, vs in map.iteritems():
        print k,
        for v in vs:
            print v,
        print ""
