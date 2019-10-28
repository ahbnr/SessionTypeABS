#!/usr/bin/env python3

import os
import random
from subprocess import run
from evaluation_lib.compile import compileModel
from evaluation_lib.render_template import renderTemplate

source_files = ['ResponsiveUI.abs', 'ResponsiveUI.st']
os.chdir('responsive_ui')

set_expect = True if input('Should the expect-flag be set? [Y/N] ') == 'Y' else False
start_value = int(input('Enter the value "start" shall be called with: '))

renderTemplate(
    'ResponsiveUI.template.abs',
    'ResponsiveUI.abs',
    {
        'set_expect': set_expect,
        'start_value': start_value
    }
)
compileModel(source_files)

run(['gen/erl/run'])
