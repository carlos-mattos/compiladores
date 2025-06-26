lexer grammar LangLexer;

fragment DIGIT      : [0-9] ;
fragment LETTER     : [a-z] ;
fragment LETTER_UP  : [A-Z] ;

ID      : LETTER (LETTER | DIGIT | '_')* ;
TYID    : LETTER_UP (LETTER_UP | LETTER | DIGIT | '_')* ;
INT     : DIGIT+ ;
FLOAT   : DIGIT* '.' DIGIT+ ;
CHAR    : '\'' ( '\\' . | ~['\\\r\n] ) '\'' ;
TRUE    : 'true' ;
FALSE   : 'false' ;
NULL    : 'null' ;

WS              : [ \t\r\n]+ -> skip ;
LINE_COMMENT    : '--' ~[\r\n]* -> skip ;
BLOCK_COMMENT   : '{-' .*? '-}' -> skip ;

/* Símbolos fixos (ordem importa para evita conflitos) */
EQ  : '==';
NEQ : '!=';
LE  : '<=';
GE  : '>=';
AND : '&&';
OR  : '||';
ARROW : '=>';

PLUS  : '+';
MINUS : '-';
STAR  : '*';
DIV   : '/';
LT    : '<';
GT    : '>';
ASSIGN: '=';

LPAREN: '(';
RPAREN: ')';
LBRACK: '[';
RBRACK: ']';
LBRACE: '{';
RBRACE: '}';
COLON : ':';
SEMIC : ';';
COMMA : ',';
DOT   : '.';

/* Símbolos adicionais */
ABSTRACT : 'abstract' ;
DATA     : 'data' ;
IF       : 'if' ;
THEN     : 'then' ;
ELSE     : 'else' ;
PRINT    : 'print' ;
ITERATE  : 'iterate' ;

/* Símbolos adicionais */ 