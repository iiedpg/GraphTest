#!/usr/bin/env python

import sys

if __name__ == "__main__":
    tips = []
    no = sys.argv[1]

    for line in sys.stdin:
        parts = line[4:].split()
        name = parts[0].split(":")[0]

        tips.append((name, parts[1]))
        tips.append((name + "_t", parts[2]))

    print "#",
    for i in xrange(len(no)):
        print ' ',

    
    tips.sort(lambda a, b: cmp(a,b))
    for t in tips:
        width = max(len(t[0]), len(t[1]))
        print ("%-" + str(width) + "s") % t[0],
    print ""

    print no, ' ',
    for t in tips:
        width = max(len(t[0]), len(t[1]))
        print ("%-" + str(width) + "s") % t[1],
    print ""
