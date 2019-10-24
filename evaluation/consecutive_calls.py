#!/usr/bin/env python3

import pickle
import os
import re
import subprocess
import pandas as pd
import random
from typing import Sequence, NewType, Tuple
from evaluate import evaluateCommand, evaluateSchedulerLog
from render_template import renderTemplate
from compile import compileModel

from common_consecutive_calls import *

os.chdir(working_dir)

type_template_file = 'model.template.st'
type_target_file = 'model.st'
model_template_file = 'model.template.abs'
model_target_file = 'model.abs'

def buildModel(times: int, num_methods: int, shuffleMethods: bool = False, use_indirection=False, busywait_factor=0):
    methodNames = list(
            map(
                genMethod,
                range(0, num_methods)
            )
        )

    indirection_methods = []
    if use_indirection:
        indirection_methods = methodNames[1::2]

    renderTemplate(type_template_file, type_target_file, {'times': times, 'methods': methodNames, 'indirection_methods': indirection_methods, 'busywait_factor': busywait_factor})

    if shuffleMethods:
        random.shuffle(methodNames)

    renderTemplate(model_template_file, model_target_file, {'times': times, 'methods': methodNames, 'indirection_methods': indirection_methods, 'busywait_factor': busywait_factor})

run_configs = [
        {
            'name': 'Method2DirectNoShuffle',
            'num_methods': 2,
            'use_indirection': False,
            'shuffle_methods': False,
        }
        #{
        #    'name': 'Method2DirectNoShuffle',
        #    'num_methods': 2,
        #    'use_indirection': False,
        #    'shuffle_methods': False,
        #},
        #{
        #    'name': 'Method10DirectNoShuffle',
        #    'num_methods': 10,
        #    'use_indirection': False,
        #    'shuffle_methods': False,
        #},
        #{
        #    'name': 'Method2IndirectNoShuffle',
        #    'num_methods': 2,
        #    'use_indirection': True,
        #    'shuffle_methods': False,
        #},
        #{
        #    'name': 'Method10IndirectNoShuffle',
        #    'num_methods': 10,
        #    'use_indirection': True,
        #    'shuffle_methods': False,
        #},
        #{
        #    'name': 'Method2DirectShuffle',
        #    'num_methods': 2,
        #    'use_indirection': False,
        #    'shuffle_methods': True,
        #},
        #{
        #    'name': 'Method10DirectShuffle',
        #    'num_methods': 10,
        #    'use_indirection': False,
        #    'shuffle_methods': True,
        #},
        #{
        #    'name': 'Method2IndirectShuffle',
        #    'num_methods': 2,
        #    'use_indirection': True,
        #    'shuffle_methods': True,
        #},
        #{
        #    'name': 'Method10IndirectShuffle',
        #    'num_methods': 10,
        #    'use_indirection': True,
        #    'shuffle_methods': True,
        #}
    ]


if os.path.exists(cache_dir):
    print("Cache still present, aborting")
    exit(-1)
else:
    os.mkdir(cache_dir)

for run_config in run_configs:
    data_frame = None

    data = []
    for i in intervals:
        buildModel(times=i, num_methods=run_config['num_methods'], use_indirection=run_config['use_indirection'], busywait_factor=(busywait_factor if not only_busywait_when_shuffling else 0))

        compileModel([model_target_file])
        evaluation_no_enforcement = evaluateCommand(averaging_factor, 'gen/erl/run')

        compileModel([model_target_file, type_target_file])
        evaluation_with_enforcement = evaluateCommand(averaging_factor, 'gen/erl/run')

        buildModel(times=i, num_methods=run_config['num_methods'], use_indirection=run_config['use_indirection'], shuffleMethods=run_config['shuffle_methods'], busywait_factor=busywait_factor)
        compileModel([model_target_file, type_target_file], logSchedulerCalls=['Model.Q'], logActivationDelay=['Model.Q'], noStaticChecks=True)
        scheduler_log = evaluateSchedulerLog(averaging_factor, 'gen/erl/run')

        evaluation_no_enforcement.update({'times': i})
        evaluation_with_enforcement.update({'times': i, 'scheduler_log': scheduler_log})

        data += [[
                evaluation_no_enforcement,
                evaluation_with_enforcement
            ]]

    data_frame = pd.DataFrame(
            data=data,
            index=intervals,
            columns=['plain', 'enforcement']
        )

    data_frame.to_pickle(os.path.join(cache_dir, '{0}.frame'.format(run_config['name'])))
    print('Saved data frame')

index_file = open(os.path.join(cache_dir, 'index'), 'wb')
pickle.dump(
        run_configs,
        index_file
    )
