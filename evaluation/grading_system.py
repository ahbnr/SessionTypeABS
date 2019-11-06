#!/usr/bin/env python3

import os
from subprocess import run
from evaluation_lib.compile import compileModel

num_executions = 100

answer = input('Evaluate with enforcement? [Y/N] ')

source_files = ['GradingSystem.abs']

if answer == 'Y':
    source_files += ['GradingSystem.st']
elif answer == 'N':
    pass
else:
    print('Cant parse answer.')
    exit(-1)

os.chdir('models/complex/grading_system')

compileModel(source_files)

fail_count = 0
success_count = 0
for i in range(0, num_executions):
    process = run(['gen/erl/run'], capture_output=True)
    
    stdout: str = process.stdout.decode()
    publishIdx = stdout.find("publish")
    requestIdx = stdout.find("request")

    if publishIdx < 0 or requestIdx < 0:
        print("Model did not execute correctly, aborting.")
        exit(-1)

    if publishIdx < requestIdx:
        success_count += 1
    else:
        fail_count += 1

print('Invocation order failed {} out of {} times.'.format(fail_count, num_executions))
