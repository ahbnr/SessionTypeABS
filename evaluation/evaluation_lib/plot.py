#!/usr/bin/env python3
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

plt.close('all')

from evaluate import evaluateCommand

def measurementsToDataFrame(measurements):
    indices = [x[0] for x in measurements]
    asDataArray = map(
            lambda m: [m['real'], m['sys'], m['user'], m['maximum_rss']],
            [x[1] for x in measurements]
        )

    return pd.DataFrame(asDataArray, index=indices, columns=['real', 'sys', 'user', 'maximum_rss'])

def barChart(dataframe):
    dataframe.plot.bar()
    plt.show()
