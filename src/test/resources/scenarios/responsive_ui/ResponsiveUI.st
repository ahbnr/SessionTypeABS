

0 -startTask-> U:start<intern == Expect>.
    U -cmpITask-> I:cmp.
U resolves startTask.
        I -cmpSTask-> S:cmp<result > 0>.
        Rel(I, cmpSTask).
            S resolves cmpSTask.
        I fetches cmpSTask.
        I -resumeTask-> U:resume.
        I resolves cmpITask.
            U resolves resumeTask