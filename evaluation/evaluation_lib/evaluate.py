#!/usr/bin/env python3
import subprocess
import re
from typing import Iterable

perf_real_time_regex = re.compile("(?P<seconds>\d+[\.,]\d+) seconds time elapsed")
perf_user_time_regex = re.compile("(?P<seconds>\d+[\.,]\d+) seconds user")
perf_sys_time_regex = re.compile("(?P<seconds>\d+[\.,]\d+) seconds sys")

time_stats_regex = re.compile("TimeStats\((?P<stats>[^)]+)\)")

activation_delay_regex = re.compile("\[SessionTypeABS\] Scheduler of class [^\s]+ could not find a viable activation\.")
scheduler_call_regex = re.compile("\[SessionTypeABS\] Scheduler of class [^\s]+ has been called\.")

def runTime(*args):
    proc = subprocess.run(["/usr/bin/time", "-f", "TimeStats(%e,%U,%S,%M)", *args],capture_output=True)
    stderr = proc.stderr.decode()
    stdout = proc.stdout.decode()

    time_result = time_stats_regex.search(stderr).group('stats').split(',')
    return {
            "real": float(time_result[0].replace(',', '.')),
            "sys": float(time_result[1].replace(',', '.')),
            "user": float(time_result[2].replace(',', '.')),
            "maximum_rss": float(time_result[3]),
            "stdout": stdout
    }

def runPerf(*args):
    proc = subprocess.run(["perf", "stat", *args],capture_output=True)
    stderr = proc.stderr.decode()
    stdout = proc.stdout.decode()
    return {
        'real': float(perf_real_time_regex.search(stderr).group('seconds').replace(',', '.')),
        'user': float(perf_user_time_regex.search(stderr).group('seconds').replace(',', '.')),
        'sys': float(perf_sys_time_regex.search(stderr).group('seconds').replace(',', '.')),
        'stdout': stdout
    }

def evaluateCommand(times: int, *args):
    accum = {
            'real': 0, # seconds
            'sys': 0,
            'user': 0,
            'maximum_rss': 0, # KB
            'stdouts': []
    }
    for i in range(1,times+1):
        perfResults = runPerf(*args)
        timeResults = runTime(*args)

        accum['real'] = accum['real'] + perfResults['real']
        accum['sys'] = accum['sys'] + perfResults['sys']
        accum['user'] = accum['user'] + perfResults['user']
        accum['maximum_rss'] = accum['maximum_rss'] + timeResults['maximum_rss']
        accum['stdouts'] = accum['stdouts'] + [perfResults['stdout'], timeResults['stdout']]

    accum['real'] = accum['real'] / times
    accum['sys'] = accum['sys'] / times
    accum['user'] = accum['user'] / times
    accum['maximum_rss'] = accum['maximum_rss'] / times

    return accum

def evaluateSchedulerLog(times: int, *args):
    schedulerCallSum = 0
    delaysSum = 0
    for i in range(0, times):
        proc = subprocess.run(args, capture_output=True)
        stdout = proc.stdout.decode()

        schedulerCallSum += len(scheduler_call_regex.findall(stdout))
        delaysSum += len(activation_delay_regex.findall(stdout))

    return {
        'scheduler_calls': schedulerCallSum / times,
        'delays': delaysSum / times
    }
