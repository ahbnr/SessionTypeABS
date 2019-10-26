import subprocess
from typing import Iterable

def compileModel(
        args,
        logActivationDelay: Iterable[str] = [],
        logSchedulerCalls: Iterable[str] = [],
        no_static_checks: bool = False
):
    options = []
    options += sum(
            map(
                lambda qualifiedName: ['--logActivationDelay', qualifiedName],
                logActivationDelay
            ),
            []
        )
    options += sum(
            map(
                lambda qualifiedName: ['--logSchedulerCalls', qualifiedName],
                logSchedulerCalls
            ),
            []
        )
    if no_static_checks:
        options += ["--noStaticChecks"]

    subprocess.run(["../sdstool.sh", "compile", *options, *args])
