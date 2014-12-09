lexer grammar IslandLexer;

STR_START   : '\"'  -> pushMode(STR)            ;
PIPE        : '|'           ;
ARG         : ~["|\t ]+ ;
WS          : [\t ]+ -> skip ;

// Default mode, space is preserved except around literal dollar symbols
mode STR;
STR_STOP    : '\"'  -> popMode   ;
CS_START    : '$('  -> pushMode(CS)             ;
VS_START    : '${'  -> pushMode(VS)             ;
TEXT        : ~["$]+        ;
DOLLAR      : '$'           ;

// CS: Command substitution
mode CS;
CS_STOP     : ')'   -> popMode      ;
CS_TEXT     : ~[)\t ]+                 ;
CS_WS       : [\t ]+ -> skip        ;

// VS: Variable substitution
mode VS;
VS_STOP     : '}'   -> popMode   ;
VS_VARIABLE : [A-Z]+             ;