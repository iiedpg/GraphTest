#!/usr/bin/env python

import sys


if __name__ == "__main__":
    
    for line in sys.stdin:
        line = line.strip()
        parts = line.split(":")
        size = len(parts) - 1

        if parts[-1].strip() == "":
            size -= 1 

        sys.stdout.write(parts[0] + " " + str(size))
        for i in xrange(1, size + 1):
            sub_parts = parts[i].split(",")
            sys.stdout.write(" " + sub_parts[0])

        sys.stdout.write("\n") 
