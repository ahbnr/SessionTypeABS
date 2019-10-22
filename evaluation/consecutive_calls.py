#!/usr/bin/env python3

import os
import subprocess
import pandas as pd
import matplotlib.pyplot as plt

from evaluate import evaluateCommand
from render_template import renderTemplate
from compile import compileModel

os.chdir('consecutive_calls')

cache_dir = 'cache'
data_frame_cache = os.path.join(cache_dir, 'data_frame')

type_file = 'model.st'
template_file = 'model.template.abs'
target_file = 'model.abs'

def buildModel(times: int):
    renderTemplate(template_file, target_file, {'times': times})

averaging_factor = 20
intervals = [1, 5, 10, 50, 100, 300, 500]

data_frame = None

if not os.path.exists(cache_dir):
    os.mkdir(cache_dir)

    data = []
    for i in intervals:
        buildModel(i)

        compileModel(target_file)
        evaluation_no_enforcement = evaluateCommand(averaging_factor, 'gen/erl/run')

        compileModel(target_file, type_file)
        evaluation_with_enforcement = evaluateCommand(averaging_factor, 'gen/erl/run')

        data += [[
                evaluation_no_enforcement,
                evaluation_with_enforcement
            ]]

    data_frame = pd.DataFrame(
            data=data,
            index=intervals,
            columns=['plain', 'enforcement']
        )

    data_frame.to_pickle(data_frame_cache)
else:
    data_frame = pd.read_pickle(data_frame_cache)

user_times_frame = data_frame.applymap(
        lambda x: x['user']
    )
delta_user_times_frame = pd.DataFrame(
        data=data_frame.apply(
            lambda x: (x['enforcement']['user'] - x['plain']['user']) / x['plain']['user'],
            axis=1
        ),
        columns=['relative increase']
    )
real_times_frame = data_frame.applymap(
        lambda x: x['real']
    )
memory_frame = data_frame.applymap(
        lambda x: x['maximum_rss']
    )
delta_memory_frame = pd.DataFrame(
        data=data_frame.apply(
            lambda x: (x['enforcement']['maximum_rss'] - x['plain']['maximum_rss']) / x['plain']['maximum_rss'],
            axis=1
        ),
        columns=['relative increase']
    )

frames = [user_times_frame, real_times_frame, delta_user_times_frame, memory_frame, delta_memory_frame]
for frame in frames:
    frame.plot.bar()
    plt.show()
input()

