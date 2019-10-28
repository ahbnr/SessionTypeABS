
0 -fStart-> UI:start<this.intern == Expect>.
    UI -fCmp-> Backend:cmp<result > 0>.
UI resolves fStart.
    Backend -fResume-> UI:resume.
    Backend resolves fCmp.
      UI resolves fResume
