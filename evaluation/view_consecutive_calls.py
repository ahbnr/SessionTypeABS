#!/usr/bin/env python3

#exec(open('./CBcm.py').read())
import pickle
import os
import re
import pandas as pd
from statistics import mean
from nltk import edit_distance
from typing import Sequence, NewType, Tuple
import matplotlib.pyplot as plt
import matplotlib as mpl

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

def expectedSequentialInvocations(num_iterations: int, num_methods: int):
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

def stripParametersFromInvocations(invocations: Sequence[Tuple[str, int]]) -> Sequence[str]:
    def helper(invocation):
        method_name, parameter = invocation
        return method_name

    return list(map(
            helper,
            invocations
        ))

index_file = open(os.path.join(cache_dir, 'index'), 'rb')
run_configs = pickle.load(index_file)

to_files = False

for run_config in run_configs:
    print('Viewing {}'.format(run_config['name']))
    data_frame = pd.read_pickle(os.path.join(cache_dir, '{0}.frame'.format(run_config['name'])))
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
                lambda x: (x['enforcement']['user'] - x['plain']['user']) / x['plain']['user'],
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

    real_times_frame = data_frame.applymap(
            lambda x: x['real']
        )

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
                lambda x: (x['enforcement']['maximum_rss'] - x['plain']['maximum_rss']) / x['plain']['maximum_rss'],
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

    scheduler_log_frame = pd.DataFrame(
        data=data_frame.apply(
                lambda x: [
                        x['enforcement']['scheduler_log']['delays'],
                        x['enforcement']['scheduler_log']['scheduler_calls']
                    ],
                axis='columns'
            ).array,
        columns=['delays', 'calls of scheduler'],
        index=data_frame.index
    )
    scheduler_log_fig = {
            'name': 'SchedulerLog',
            'frame': scheduler_log_frame,
            'ylabel': '',
            'xlabel': 'repetitions'
        }

    data_frame = data_frame.drop([100,300,500])
    #levenshtein_sequential_frame = data_frame.applymap(
    #        lambda x: {
    #            'levenshtein': mean(
    #                map(
    #                    lambda actualInvocations: edit_distance(
    #                            expectedSequentialInvocations(num_iterations=x['times'],num_methods=run_config['num_methods']),
    #                            actualInvocations
    #                        ),
    #                    map(
    #                        extractInvocations,
    #                        x['stdouts']
    #                    )
    #                )
    #            ),
    #            'length': x['times'] * run_config['num_methods']
    #        }
    #    )
    #levenshtein_sequential_comparison_frame = pd.DataFrame(
    #    data=levenshtein_sequential_frame.apply(
    #            lambda row: [
    #                    row['plain']['levenshtein'],
    #                    row['enforcement']['levenshtein'],
    #                    row['plain']['length']
    #                ],
    #            axis=1
    #        ).array,
    #    columns=['plain', 'enforcement', 'length'],
    #    index=data_frame.index
    #)

    #levenshtein_sequential_delta_comparison_frame = pd.DataFrame(
    #    data=levenshtein_sequential_frame.apply(
    #            lambda row: row['plain']['levenshtein'] / row['plain']['length'],
    #            axis=1
    #        ).array,
    #    columns=['relative edits'],
    #    index=data_frame.index
    #)

    levenshtein_frame = data_frame.applymap(
            lambda x: {
                'levenshtein_sequential': mean(
                    map(
                        lambda actualInvocations: edit_distance(
                                expectedSequentialInvocations(num_iterations=x['times'],num_methods=run_config['num_methods']),
                                actualInvocations
                            ),
                        map(
                            extractInvocations,
                            x['stdouts']
                        )
                    )
                ),
                'levenshtein_alternating': mean(
                    map(
                        lambda actualInvocations: edit_distance(
                                stripParametersFromInvocations(
                                    expectedSequentialInvocations(num_iterations=x['times'],num_methods=run_config['num_methods'])
                                ),
                                stripParametersFromInvocations(actualInvocations)
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
                        row['plain']['levenshtein_sequential'],
                        row['plain']['levenshtein_alternating'],
                        row['enforcement']['levenshtein_sequential'],
                        row['enforcement']['levenshtein_alternating'],
                        row['plain']['length']
                    ],
                axis=1
            ).array,
        columns=['plain sequential', 'plain alternating', 'enforcement sequential', 'enforcement alternating', 'sequence length'],
        index=data_frame.index
    )

    levenshtein_delta_comparison_frame = pd.DataFrame(
        data=levenshtein_frame.apply(
                lambda row: row['plain']['levenshtein_alternating'] / row['plain']['length'],
                axis=1
            ).array,
        columns=['edits / sequence length'],
        index=data_frame.index
    )

    def levenshtein_adjustment(frame):
        adjusted_frame = pd.DataFrame(
                data = frame.apply(
                    lambda row: [
                            row['plain sequential'],
                            row['plain alternating'],
                            row['sequence length']
                        ],
                    axis='columns'
                ).array,
                columns=['sequential', 'alternating', 'sequence length'],
                index=data_frame.index
            )
        adjusted_frame.index.name = 'repetitions'
        return adjusted_frame

    levenshtein_comparison_fig = {
            'name': 'Levenshtein',
            'frame': levenshtein_comparison_frame,
            'ylabel': 'edits',
            'xlabel': 'repetitions',
            'adjustment': levenshtein_adjustment,
            'customplot': lambda frame: frame.plot.bar(stacked=True)
        }

    levenshtein_delta_fig = {
            'name': 'DeltaLevenshtein',
            'frame': levenshtein_delta_comparison_frame,
            'ylabel': 'edits / sequence length',
            'xlabel': 'repetitions',
        }

    figures = [scheduler_log_fig]#[user_times_fig, delta_user_times_fig, memory_fig, delta_memory_fig, levenshtein_comparison_fig, levenshtein_delta_fig, scheduler_log_fig]
    for fig in figures:
        print('Viewing figure {}'.format(fig['name']))

        if to_files:
            fig['frame'].to_csv(os.path.join(cache_dir, '{}_{}.csv'.format(run_config['name'], fig['name'])))

        if 'adjustment' in fig:
            fig['frame'] = fig['adjustment'](fig['frame'])

        ax = None
        if 'customplot' in fig:
            ax = fig['customplot'](fig['frame'])
        else:
            ax = fig['frame'].plot.bar()

        ax.set_ylabel(fig['ylabel'])
        ax.set_xlabel(fig['xlabel'])
        ax.autoscale()

        if to_files:
            plt.savefig(os.path.join(cache_dir, '{}_{}.pdf'.format(run_config['name'], fig['name'])))
        else:
            plt.show()
            input()

        plt.close()

