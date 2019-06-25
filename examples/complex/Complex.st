  0 -f-> Complex.P:main
. (
     Complex.P -f2-> Complex.O:m1
        .Complex.O -f3-> Complex.D:d
        .Rel(Complex.O, f3)
    .Complex.P -f4-> Complex.O:m3
    .Rel(Complex.P, f2)
        .Complex.O resolves f4
                .Complex.D resolves f3
        .Complex.O fetches f3
        .Complex.O resolves f2
  )*
.Complex.P resolves f
