0 -mainTask-> Caller:main.
(
    Caller -aTask-> Callee:a.
    Rel(Caller, aTask).
        Callee -callbackTask1-> Caller:callback1.
            Caller resolves callbackTask1.
        Callee -callbackTask2-> Caller:callback2.
            Caller resolves callbackTask2.
        Callee resolves aTask
)*.
Caller resolves mainTask