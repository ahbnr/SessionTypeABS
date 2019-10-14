
0 -f-> ComputationServer:compute.
ComputationServer -publishTask-> ServiceDesk:publish.
ComputationServer -announceTask-> Student:announce.
    ServiceDesk resolves publishTask.
    Student -requestTask-> ServiceDesk:request.
    Rel(Student, requestTask).
        ServiceDesk resolves requestTask.
    Student fetches requestTask.
    Student resolves announceTask.
ComputationServer resolves f
