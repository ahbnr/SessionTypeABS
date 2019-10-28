#!/usr/bin/env python3

import pickle
import os
import re
import pandas as pd
from statistics import mean
from nltk import edit_distance
from typing import Sequence, NewType, Tuple
import matplotlib.pyplot as plt
import matplotlib as mpl

from common_notification_service_perf import *

os.chdir(working_dir)

to_files = True

data_frame = pd.read_pickle(os.path.join(cache_dir, 'NotificationService.frame'))
print('Loaded data frame')

user_times_frame = data_frame.applymap(
        lambda x: x['user']
    )
user_times_frame.index.name = 'repetitions'
user_times_fig = {
        'name': 'UserTimes',
        'frame': user_times_frame,
        'ylabel': 'user mode execution time [s]',
        'xlabel': 'repetitions'
    }

delta_user_times_frame = pd.DataFrame(
        data=data_frame.apply(
            lambda x: ((x['enforcement']['user'] - x['plain']['user']) / x['plain']['user']) * 100,
            axis=1
        ),
        columns=['relative increase']
    )
delta_user_times_frame.index.name = 'repetitions'
delta_user_times_fig = {
        'name': 'DeltaUserTimes',
        'frame': delta_user_times_frame,
        'ylabel': 'relative increase [%]',
        'xlabel': 'repetitions'
    }

memory_frame = data_frame.applymap(
        lambda x: x['maximum_rss']
    )
memory_frame.index.name = 'repetitions'
memory_fig = {
        'name': 'Memory',
        'frame': memory_frame,
        'ylabel': 'maximum memory resident set size [KB]',
        'xlabel': 'repetitions'
    }

delta_memory_frame = pd.DataFrame(
        data=data_frame.apply(
            lambda x: ((x['enforcement']['maximum_rss'] - x['plain']['maximum_rss']) / x['plain']['maximum_rss']) * 100,
            axis=1
        ),
        columns=['relative increase']
    )
delta_memory_frame.index.name = 'repetitions'
delta_memory_fig = {
        'name': 'DeltaMemory',
        'frame': delta_memory_frame,
        'ylabel': 'relative increase [%]',
        'xlabel': 'repetitions'
    }

figures = [user_times_fig, delta_user_times_fig, memory_fig, delta_memory_fig]
for fig in figures:
    print('Viewing figure {}'.format(fig['name']))

    if to_files:
        fig['frame'].to_csv(os.path.join(cache_dir, 'NotificationService_{}.csv'.format(fig['name'])))

    ax = None
    if 'customplot' in fig:
        ax = fig['customplot'](fig['frame'])
    else:
        ax = fig['frame'].plot.bar()

    ax.set_ylabel(fig['ylabel'])
    ax.set_xlabel(fig['xlabel'])
    ax.autoscale()

    if to_files:
        plt.savefig(os.path.join(cache_dir, 'NotificationService_{}.pdf'.format(fig['name'])))
    else:
        plt.show()
        input()

    plt.close()

