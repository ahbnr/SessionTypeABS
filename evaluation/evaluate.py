#!/usr/bin/env python3
import subprocess
import re

perf_real_time_regex = re.compile("(?P<seconds>\d+\.\d+) seconds time elapsed")
perf_user_time_regex = re.compile("(?P<seconds>\d+\.\d+) seconds user")
perf_sys_time_regex = re.compile("(?P<seconds>\d+\.\d+) seconds sys")

time_stats_regex = re.compile("TimeStats\((?P<stats>[^)]+)\)")

def runTime(*args):
    output = subprocess.run(["/usr/bin/time", "-f", "TimeStats(%e,%U,%S,%M)", *args],capture_output=True).stderr.decode()
    result = time_stats_regex.search(output).group('stats').split(',')
    return {
            "real": float(result[0]),
            "sys": float(result[1]),
            "user": float(result[2]),
            "maximum_rss": float(result[3])
    }

def extract_perf_stats(rawstats: str):
    return {
            'real': float(perf_real_time_regex.search(rawstats).group('seconds')),
            'user': float(perf_user_time_regex.search(rawstats).group('seconds')),
            'sys': float(perf_sys_time_regex.search(rawstats).group('seconds'))
    }

def runPerf(*args):
    result = subprocess.run(["perf", "stat", *args],capture_output=True).stderr.decode()
    return extract_perf_stats(result)

def evaluateCommand(times: int, *args):
    accum = {
            'real': 0, # seconds
            'sys': 0,
            'user': 0,
            'maximum_rss': 0, # KB
            'times': times
    }
    for i in range(1,times+1):
        perfResults = runPerf(*args)
        timeResults = runTime(*args)

        accum['real'] = accum['real'] + perfResults['real']
        accum['sys'] = accum['sys'] + perfResults['sys']
        accum['user'] = accum['user'] + perfResults['user']
        accum['maximum_rss'] = accum['maximum_rss'] + timeResults['maximum_rss']

    accum['real'] = accum['real'] / times
    accum['sys'] = accum['sys'] / times
    accum['user'] = accum['user'] / times
    accum['maximum_rss'] = accum['maximum_rss'] / times

    return accum
