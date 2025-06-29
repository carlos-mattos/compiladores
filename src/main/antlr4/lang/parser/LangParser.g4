parser grammar LangParser;

options { tokenVocab = LangLexer; }

prog        : def* EOF ;
def         : dataDecl | funDecl ;

dataDecl    : ABSTRACT? DATA TYID LBRACE (decl | funDecl)* RBRACE ;

decl        : ID COLON type SEMIC ;

funDecl     : ID LPAREN params? RPAREN (COLON typeList)? cmd ;

params      : param (COMMA param)* ;
param       : ID DCOLON type ;
typeList    : type (COMMA type)* ;

type        : TYID                            #simpleType
            | INT_TYPE                        #intType
            | FLOAT_TYPE                      #floatType
            | BOOL_TYPE                       #boolType
            | CHAR_TYPE                       #charType
            | LBRACK type RBRACK              #arrayType
            | type STAR type                  #productType
            ;

cmd         : block                           #blockCmd
            | IF expr THEN cmd ELSE cmd       #ifCmd
            | iterateCmd                      #iterate
            | PRINT expr SEMIC?               #printCmd
            | READ lvalue SEMIC?              #readCmd
            | RETURN exprList? SEMIC?         #returnCmd
            | assign SEMIC?                   #assignCmd
            | call SEMIC?                     #callCmd
            | callWithRet SEMIC?              #callWithRetCmd
            | expr                            #exprCmd
            ;

iterateCmd  : ITERATE LPAREN (lvalue COLON expr | expr) RPAREN cmd ;

lvalue      : ID (LBRACK expr RBRACK | DOT ID)* ;
assign      : lvalue ASSIGN expr ;
call        : ID LPAREN exprList? RPAREN ;
callWithRet : ID LPAREN exprList? RPAREN LT lvalue (COMMA lvalue)* GT ;
exprList    : expr (COMMA expr)* ;

block       : LBRACE (cmd (SEMIC)?)* RBRACE ;

expr        : expr '.' ID                     #fieldAccess
            | expr '[' expr ']'               #arrayAccess
            | expr '(' exprList? ')'          #callExpr
            | '(' expr ')'                    #paren
            | op=('+'|'-'|'!') expr           #unary
            | expr op=('*'|'/'|'%') expr      #mult
            | expr op=('+'|'-') expr          #add
            | expr op=('=='|'!='|'<'|'>'|'<='|'>=') expr #rel
            | expr op=('&&'|'||') expr        #bool
            | ID                              #var
            | INT                             #intLit
            | FLOAT                           #floatLit
            | TRUE                            #trueLit
            | FALSE                           #falseLit
            | CHAR                            #charLit
            | NULL                            #nullLit
            | NEW TYID                        #newRecord
            | NEW TYID LBRACK expr RBRACK     #newArray
            | LBRACK exprList? RBRACK         #arrayLit
            ; 