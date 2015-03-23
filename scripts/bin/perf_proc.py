#!/usr/bin/env python

import sys
import time

if __name__ == "__main__":
    pid = int(sys.argv[1])
    ticks = int(sys.argv[2])
    HZ = int(sys.argv[3])
    network_line = sys.argv[4]

    cpu_usage = float(sys.argv[5])

    #initial in advance in case IOError
    time_elapsed = 0
    utime = 0
    stime = 0
    cutime = 0
    cstime = 0
    nr_thread = 0
    vsize = 0
    rss = 0
    data_size = 0
    rchar = 0
    wchar = 0
    read_bytes = 0
    write_bytes = 0
    net_read = 0
    net_write = 0
    #cpu_usage
    abs_time = int(time.time() * 1000);

    try:
        #read stat
        file_name = "/proc/%d/stat" % pid
        fil = open(file_name)
        with fil:
            line = fil.readline()
            parts = line.split()
            
            utime = int(parts[13]) / ticks
            stime = int(parts[14]) / ticks
            cutime = int(parts[15]) / ticks
            cstime = int(parts[16]) /ticks
            nr_thread = int(parts[19])

            start_time = int(parts[21])

            vsize = int(parts[22]) 

        file_name = "/proc/%d/status" % pid
        fil = open(file_name)
        with fil:
            for line in fil:
                if line.startswith("VmData"):
                    data_size = int(line.split(":")[1].strip().split()[0]) * 1000
                    break #note vmdata comes after vmrss
                elif line.startswith("VmRSS"):
                    rss = int(line.split(":")[1].strip().split()[0]) * 1000

        file_name = "/proc/%d/io" % pid
        fil = open(file_name)
        io_status = {}
        with fil:
            for line in fil:
                parts = line.split(":") 
                _k, _v = parts[0].strip(), parts[1].strip() 
                io_status[_k] = int(_v)

        rchar = io_status.get("rchar")
        wchar = io_status.get("wchar")
        read_bytes = io_status.get("read_bytes")
        write_bytes = io_status.get("write_bytes")

        if network_line.strip() != "":
            parts = network_line.split("#")
            net_write = float(parts[3])
            net_read = float(parts[4])
        else:
            net_write = 0
            net_read = 0
        
        #jiffies
        fil = open("/proc/uptime")
        with fil:
            line = fil.readline()
            parts = line.split()
            uptime = float(parts[0].strip())

        time_elapsed = uptime - start_time / ticks
    except IOError, e:
        pass

    print time_elapsed, utime, stime, cutime, cstime, nr_thread, 7, vsize, rss, data_size, 11, rchar, wchar, read_bytes, write_bytes, 16, net_read, net_write, 19, cpu_usage, 21, abs_time
