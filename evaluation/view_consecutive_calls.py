#!/usr/bin/env python3

import pickle
import os
import re
import pandas as pd
from statistics import mean
from nltk import edit_distance
from typing import Sequence, NewType, Tuple
import matplotlib.pyplot as plt

from common_consecutive_calls import *

os.chdir(working_dir)

Invocation = NewType('Invocation', Tuple[str, int])
invocation_regex = re.compile("(?P<method>m\d+)\((?P<iteration>\d+)\)")
def extractInvocations(stdout: str) -> Sequence[Invocation]:
    def helper(x):
        (method, iteration_str) = x
        return (method, int(iteration_str))

    return list(map(
            helper,
            invocation_regex.findall(stdout)
        ))

def expectedInvocations(num_iterations: int, num_methods: int):
    return sum(
        map(
            lambda iteration_idx: sum(
                    map(
                        lambda method_id: [(genMethod(method_id), iteration_idx)],
                        range(0, num_methods)
                    ),
                    []
                ),
            range(0, num_iterations)
        ),
        []
    )

index_file = open(os.path.join(cache_dir, 'index'), 'rb')
run_configs = pickle.load(index_file)

for run_config in run_configs:
    print('Viewing {}'.format(run_config['name']))
    data_frame = pd.read_pickle(os.path.join(cache_dir, '{0}.frame'.format(run_config['name'])))
    print('Loaded data frame')

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


    scheduler_log_frame = pd.DataFrame(
        data=data_frame.apply(
                lambda x: [
                        x['enforcement']['scheduler_log']['delays'],
                        x['enforcement']['scheduler_log']['scheduler_calls']
                    ],
                axis='columns'
            ).array,
        columns=['delays', 'scheduling events'],
        index=data_frame.index
    )

    #data_frame = data_frame.drop([100,300,500])
    levenshtein_frame = data_frame.applymap(
            lambda x: {
                'levenshtein': mean(
                    map(
                        lambda actualInvocations: edit_distance(
                                expectedInvocations(num_iterations=x['times'],num_methods=run_config['num_methods']),
                                actualInvocations
                            ),
                        map(
                            extractInvocations,
                            x['stdouts']
                        )
                    )
                ),
                'length': x['times'] * run_config['num_methods']
            }
        )
    levenshtein_comparison_frame = pd.DataFrame(
        data=levenshtein_frame.apply(
                lambda row: [
                        row['plain']['levenshtein'],
                        row['enforcement']['levenshtein'],
                        row['plain']['length']
                    ],
                axis=1
            ).array,
        columns=['plain', 'enforcement', 'length'],
        index=data_frame.index
    )

    levenshtein_delta_comparison_frame = pd.DataFrame(
        data=levenshtein_frame.apply(
                lambda row: row['plain']['levenshtein'] / row['plain']['length'],
                axis=1
            ).array,
        columns=['relative edits'],
        index=data_frame.index
    )

    frames = [levenshtein_comparison_frame,scheduler_log_frame]#[user_times_frame, real_times_frame, delta_user_times_frame, memory_frame, delta_memory_frame, levenshtein_comparison_frame]
    for frame in frames:
        frame.plot.bar()
        plt.show()
    input()

