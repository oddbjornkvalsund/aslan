lexer grammar IslandLexer;

// Default mode, space is preserved
CS_START    : '$('  -> mode(CS)             ;
VS_START    : '${'  -> mode(VS)             ;
TEXT        : ~'$'+        ;

// CS: Command substitution
mode CS;
CS_STOP     : ')'   -> mode(DEFAULT_MODE)   ;
CS_WS       : [\t ]+ -> skip                ;
CS_TEXT     : ~')'+                         ;

// VS: Variable substitution
mode VS;
VS_STOP     : '}'   -> mode(DEFAULT_MODE)   ;
VS_VARIABLE : [A-Z]+                        ;