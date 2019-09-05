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

init: '0' '-' future=IDENTIFIER '->' classId=qualified_class_identifier ':' method=IDENTIFIER ('<' postcondition=pure_exp '>')?;
interact: caller=qualified_class_identifier '-' future=IDENTIFIER '->' callee=qualified_class_identifier ':' method=IDENTIFIER ('<' postcondition=pure_exp '>')?;
releaseR: 'Rel('classId=qualified_class_identifier ',' future=IDENTIFIER ')';
repeat: '(' repeatedType=globalType ')*';
branch: classId=qualified_class_identifier '{' (globalType (',' globalType)*)? '}';
fetch: classId=qualified_class_identifier 'fetches' future=IDENTIFIER;
resolve: classId=qualified_class_identifier 'resolves' future=IDENTIFIER;

qualified_class_identifier: (TYPE_IDENTIFIER '.')* TYPE_IDENTIFIER ;

WS : [ \t\f\r\n]+ -> channel(HIDDEN);

// Taken from ABS grammar
fragment LETTER : [A-Za-z] ;
fragment DIGIT : [0-9] ;
IDENTIFIER : [a-z] (LETTER | DIGIT | '_')*;
TYPE_IDENTIFIER : [A-Z] (LETTER | DIGIT | '_')*;

// ADL
// Also taken from ABS grammar
qualified_identifier : (TYPE_IDENTIFIER '.')* IDENTIFIER ;
qualified_type_identifier : (TYPE_IDENTIFIER '.')* TYPE_IDENTIFIER ;
var_or_field_ref : ('this' '.')? IDENTIFIER ;

NEGATION : '!' ;
MINUS : '-' ;
MULT : '*' ;
DIV : '/' ;
MOD : '%' ;
PLUS : '+' ;
LTEQ : '<=' ;
GTEQ : '>=' ;
LT : '<' ;
GT : '>' ;
ANDAND : '&&' ;
OROR : '||' ;
EQEQ : '==' ;
NOTEQ : '!=' ;
IMPLIES : '->' ;
EQUIV : '<->' ;

fragment EXPONENT : ('e' | 'E' | 'e+' | 'E+' | 'e-' | 'E-') DIGIT+;
INTLITERAL : '0' | [1-9] DIGIT* ;
FLOATLITERAL : INTLITERAL? '.' DIGIT+ EXPONENT? ;
STRINGLITERAL
  :  '"' (STR_ESC | ~('\\' | '"' | '\r' | '\n'))* '"'
  ;
fragment STR_ESC
  :  '\\' ('\\' | '"' | 't' | 'n' | 'r')
  ;

pure_exp :
      qualified_type_identifier ('(' pure_exp_list ')')?   # ConstructorExp
    | op=(NEGATION | MINUS) pure_exp                       # UnaryExp
    | l=pure_exp op=(MULT | DIV | MOD) r=pure_exp          # MultExp
    | l=pure_exp op=(PLUS | MINUS) r=pure_exp              # AddExp
    | l=pure_exp op=(LT | GT | LTEQ | GTEQ) r=pure_exp     # GreaterExp
    | l=pure_exp op=(EQEQ | NOTEQ) r=pure_exp              # EqualExp
    | l=pure_exp op='&&' r=pure_exp                        # AndExp
    | l=pure_exp op='||' r=pure_exp                        # OrExp
    | var_or_field_ref                                     # VarOrFieldExp
    | FLOATLITERAL                                         # FloatExp
    | INTLITERAL                                           # IntExp
    | STRINGLITERAL                                        # StringExp
    | 'this'                                               # ThisExp
    | 'null'                                               # NullExp
    ;

pure_exp_list : (pure_exp (',' pure_exp)*)? ;
