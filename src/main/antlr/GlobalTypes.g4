// Define a grammar called Hello
grammar GlobalTypes;

globalType:
      init                              # Initialization
    | interact                          # Interaction
    | releaseR                          # Release
    | lhs=globalType '.' rhs=globalType # Concatenation
    | repeat                            # Repetition
    | branch                            # Branching
    | fetch                             # Fetching
    | resolve                           # Resolution
    ;

init: '0' '-' future=IDENTIFIER '->' classId=qualified_class_identifier '.' method=IDENTIFIER;
interact: caller=qualified_class_identifier '-' future=IDENTIFIER '->' callee=qualified_class_identifier '.' method=IDENTIFIER;
releaseR: 'Rel('classId=qualified_class_identifier ',' future=IDENTIFIER ')';
repeat: '(' repeatedType=globalType ')*';
branch: classId=qualified_class_identifier '{' (globalType (',' globalType)*)? '}';
fetch: classId=qualified_class_identifier 'fetch' future=IDENTIFIER;
resolve: classId=qualified_class_identifier 'resolve' future=IDENTIFIER;

qualified_class_identifier: (TYPE_IDENTIFIER '.')* TYPE_IDENTIFIER ;

WS : [ \t\f\r\n]+ -> channel(HIDDEN);

// Taken from ABS grammar
fragment LETTER : [A-Za-z] ;
fragment DIGIT : [0-9] ;
IDENTIFIER : [a-z] (LETTER | DIGIT | '_')*;
TYPE_IDENTIFIER : [A-Z] (LETTER | DIGIT | '_')*;
