#!/usr/bin/env python3
from typing import TypeVar, Sequence, Optional

T = TypeVar('T')

def lev(lhs: Sequence[T], rhs: Sequence[T], i: Optional[int] = None, j: Optional[int] = None) -> int:
    if i is None or j is None:
        return lev(lhs, rhs, len(lhs), len(rhs))
    else:
        m = min([i, j])
        if m == 0:
            return max([i, j])
        else:
            return min([
                    lev(lhs, rhs, i-1, j) + 1,
                    lev(lhs, rhs, i, j-1) + 1,
                    lev(lhs, rhs, i-1, j-1) + (1 if lhs[i-1] != rhs[j-1] else 0)
                ])
