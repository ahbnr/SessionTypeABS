#!/usr/bin/env python3

import os
from subprocess import run
from evaluation_lib.compile import compileModel
from evaluation_lib.render_template import renderTemplate

num_repetitions = 10
source_files = ['NotificationService.abs', 'NotificationService.st']

os.chdir('notification_service')
renderTemplate(
        'NotificationService.template.abs',
        'NotificationService.abs',
        {
            'repetitions': num_repetitions
        }
    )
compileModel(source_files, logSchedulerCalls=['NotificationService', 'MailServer'])

run(['gen/erl/run'])
