0 -mainTask-> Caller:main.
(
    Caller{
        Caller -aTask-> Callee:a.
            Callee resolves aTask.
            Caller -bTask-> Callee:b.
            Callee resolves bTask,
        Caller -cTask-> Callee:c.
            Callee resolves cTask
    }
)*.
Caller resolves mainTask