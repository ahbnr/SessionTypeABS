
0 -f-> A:m.
A -f2-> B:m1.
Rel(A, f2).
B{
    B resolves f2 with Ok.
        A fetches f2 as Ok.
        A -f3-> B:m2.
        B resolves f3,
    B resolves f2 with Error.
        A fetches f2 as Error.
        A -f4-> B:m3.
        B resolves f4
}.
A resolves f