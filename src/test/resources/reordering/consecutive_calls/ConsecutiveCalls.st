0 -mainTask-> Caller:main.
(
    Caller -aTask-> Callee:a.
        Callee resolves aTask.
    Caller -bTask-> Callee:b.
        Callee resolves bTask
)*.
Caller resolves mainTask