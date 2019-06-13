// Define a grammar called Hello
grammar LocalTypes;


localType:
      invocREv                        # InvocREvT
    | reactEv                         # ReactEvT
    | lhs=localType '.' rhs=localType # ConcatT
    | repeat                          # RepeatT
    | branch                          # BranchT
    ;

typeAssignment: className=qualified_class_identifier ':' localType;

typeAssignments: typeAssignment*;

invocREv: '?' future=IDENTIFIER method=IDENTIFIER;
reactEv: 'React(' future=IDENTIFIER ')';
repeat: '(' repeatedType=localType ')*';
branch: '{' (localType (',' localType)*)? '}';

qualified_class_identifier: (TYPE_IDENTIFIER '.')* TYPE_IDENTIFIER ;

WS : [ \t\f\r\n]+ -> channel(HIDDEN);

// Taken from ABS grammar
fragment LETTER : [A-Za-z] ;
fragment DIGIT : [0-9] ;
IDENTIFIER : [a-z] (LETTER | DIGIT | '_')*;
TYPE_IDENTIFIER : [A-Z] (LETTER | DIGIT | '_')*;
