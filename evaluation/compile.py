import subprocess
from typing import Iterable

def compileModel(
        args,
        logActivationDelay: Iterable[str] = [],
        logSchedulerCalls: Iterable[str] = [],
        noStaticChecks: bool = False
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
    if noStaticChecks:
        options += ["--noStaticChecks"]

    subprocess.run(["../sdstool.sh", "compile", *options, *args])
