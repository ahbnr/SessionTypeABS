#!/usr/bin/env python3

import pickle
import os
import re
import subprocess
import pandas as pd
import random
from typing import Sequence, NewType, Tuple
from evaluation_lib.evaluate import evaluateCommand, evaluateSchedulerLog
from evaluation_lib.render_template import renderTemplate
from evaluation_lib.compile import compileModel

from common_notification_service_perf import *

os.chdir(working_dir)

type_file = 'NotificationService.st'
model_template_file = 'NotificationService.template.abs'
model_target_file = 'NotificationService.abs'

def buildModel(repetitions: int):
    renderTemplate(
            'NotificationService.template.abs',
            'NotificationService.abs',
            {
                'repetitions': repetitions
            }
        )

if os.path.exists(cache_dir):
    print("Cache still present, aborting")
    exit(-1)
else:
    os.mkdir(cache_dir)

data_frame = None
data = []
for i in intervals:
    buildModel(
            repetitions=i,
        )

    compileModel([model_target_file])
    evaluation_no_enforcement = evaluateCommand(averaging_factor, 'gen/erl/run')

    compileModel([model_target_file, type_file])
    evaluation_with_enforcement = evaluateCommand(averaging_factor, 'gen/erl/run')

    evaluation_no_enforcement.update({'times': i})
    evaluation_with_enforcement.update({'times': i})

    data += [[
            evaluation_no_enforcement,
            evaluation_with_enforcement
        ]]

data_frame = pd.DataFrame(
        data=data,
        index=intervals,
        columns=['plain', 'enforcement']
    )

data_frame.to_pickle(os.path.join(cache_dir, 'NotificationService.frame'))
print('Saved data frame')
