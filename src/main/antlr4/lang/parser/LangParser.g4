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
            | LBRACK type RBRACK              #arrayType
            | type STAR type                  #productType
            ;

cmd         : block                           #blockCmd
            | IF expr THEN cmd ELSE cmd       #ifCmd
            | ITERATE expr cmd                #iterateCmd
            | PRINT expr SEMIC?               #printCmd
            | READ expr SEMIC?                #readCmd
            | RETURN exprList? SEMIC?         #returnCmd
            | assign SEMIC?                   #assignCmd
            | call SEMIC?                     #callCmd
            | expr                            #exprCmd
            ;

assign      : ID ASSIGN expr ;
call        : ID LPAREN exprList? RPAREN ;
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
            ; 