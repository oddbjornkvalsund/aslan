lexer grammar IslandLexer;

tokens { DOLLAR, CS_START, VS_START }

STR_START   : '\"'  -> pushMode(STR)            ;
CS_START : '$(' -> pushMode(CS);
VS_START : '${' -> pushMode(VS);
PIPE        : '|'           ;
ARG         : ~["|$\t ]+ ;
WS          : [\t ]+ -> skip ;
DOLLAR      : '$' ;

// Default mode, space is preserved except around literal dollar symbols
mode STR;
STR_STOP    : '\"'  -> popMode   ;
STR_CS_START    : '$('  -> type(CS_START), pushMode(CS)             ;
STR_VS_START    : '${'  -> type(VS_START), pushMode(VS)             ;
TEXT        : ~["$]+        ;
STR_DOLLAR      : '$' -> type(DOLLAR)          ;

// CS: Command substitution
mode CS;
CS_STOP     : ')'   -> popMode      ;
CS_TEXT     : ~[)\t ]+                 ;
CS_WS       : [\t ]+ -> skip        ;

// VS: Variable substitution
mode VS;
VS_STOP     : '}'   -> popMode   ;
VS_VARIABLE : [A-Z]+             ;