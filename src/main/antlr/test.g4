// Define a grammar called Hello
grammar test;

r: FUNCTION ' ' METHOD;

LCASE: [a-z];
UCASE: [A-Z];

METHOD: LCASE+;
FUNCTION: (METHOD+|UCASE+);
