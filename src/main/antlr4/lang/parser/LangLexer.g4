lexer grammar LangLexer;

/* Palavras-chave primeiro (ordem importa!) */
IF       : 'if' ;
THEN     : 'then' ;
ELSE     : 'else' ;
PRINT    : 'print' ;
READ     : 'read' ;
RETURN   : 'return' ;
ITERATE  : 'iterate' ;
ABSTRACT : 'abstract' ;
DATA     : 'data' ;
NEW      : 'new' ;
TRUE     : 'true' ;
FALSE    : 'false' ;

/* Tipos básicos */
INT_TYPE : 'Int' ;
FLOAT_TYPE : 'Float' ;
BOOL_TYPE : 'Bool' ;
CHAR_TYPE : 'Char' ;

fragment DIGIT      : [0-9] ;
fragment LETTER     : [a-z] ;
fragment LETTER_UP  : [A-Z] ;

ID      : LETTER (LETTER | DIGIT | '_')* ;
TYID    : LETTER_UP (LETTER_UP | LETTER | DIGIT | '_')* ;
INT     : DIGIT+ ;
FLOAT   : DIGIT* '.' DIGIT+ ;
CHAR    : '\'' ( '\\' [b] | '\\' DIGIT DIGIT DIGIT | ~['\\\r\n] ) '\'' ;
NULL    : 'null' ;

WS              : [ \t\r\n]+ -> skip ;
LINE_COMMENT    : '--' ~[\r\n]* -> skip ;
BLOCK_COMMENT   : '{-' (~[-] | '-' ~[}])* '-}' -> skip ;

/* Símbolos fixos (ordem importa para evita conflitos) */
EQ  : '==';
NEQ : '!=';
LE  : '<=';
GE  : '>=';
AND : '&&';
OR  : '||';
ARROW : '=>';
DCOLON : '::';

PLUS  : '+';
MINUS : '-';
STAR  : '*';
DIV   : '/';
PERCENT : '%';
NOT   : '!';
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