
0 -fStart-> UI:start<intern == Expect>.
    UI -fCmp-> Backend:cmp<result > 0>.
UI resolves fStart.
    Backend -fResume-> UI:resume.
    Backend resolves fCmp.
      UI resolves fResume
