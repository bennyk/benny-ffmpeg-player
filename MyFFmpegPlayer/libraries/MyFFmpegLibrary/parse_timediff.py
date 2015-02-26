
import sys
import re
import math
from statistics import mean, median, stdev

fh = open(sys.argv[1])
samples = {}
for line in fh:
    #print(line, end='')
    a = re.match("[^:]+:\s+MEASURE_TIME(.*)timediff:\s+(\d+)\.\s+(\d+)", line) 
    if a is not None:
        title = a.group(1).strip()
        secs = a.group(2)
        nsec = a.group(3)

        total_msec = int(secs) * 1000 + int(nsec)/1000000
        #print(title, secs, nsec, total_msec)
        if title not in samples: 
            samples[title] = []
        s = samples[title]
        s.append(total_msec)

def calculate_samples(title, a):
    b = sorted(a)
    avg = sum(a)/len(a)

    p = math.floor(len(a) / 2)
    if len(a) % 2 == 0:
        med = (a[p] + a[p-1])/2
    else:
        med = a[p]

    print("%25s %5.2f %5.2f %5.2f %5.2f %6.2f %5d" % (title, b[0], b[-1], median(a), mean(a), stdev(a), len(a))) 

#print(samples)

#print("#", end='')
print("%25s %5s %5s %5s %5s %6s %5s" % ("title", "min", "max", "med", "avg", "stdev", "N")) 
for t in samples.keys():
    calculate_samples(t, samples[t]) 

