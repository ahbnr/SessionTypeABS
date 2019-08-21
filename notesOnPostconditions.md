* since postconditions are translated into assertions, my first idea was to
  directly parse them using the ABS parser as a pure_exp.

  This caused some problems, since the ABS compiler has not been built for this
  kind of usage:

  1. The ANTLR components of the parser would be able to parse it without
     problems
  2. Issues appear during the application of JastAdd:
    * CreateJastAddASTListener is not built to process a single pure_exp without
      a compilation unit as context.
    * JastAdd nodes modify and search their (parent) tree for definitions etc.,
      parsing a pure_exp out of context of its method would thus probably be
      ultimately futile or require extensive modification of the ABS compiler
* Building a custom expression grammar/parser provides the benefit of being able
  to extend / restrict the space of possible expressions at will and also gives
  complete control on how it will be translated into ABS code.

  Drawback: Redundant parsing / compilation code


