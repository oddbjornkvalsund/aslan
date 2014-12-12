lexer grammar IslandLexer;

tokens { ARG, CS_START, DOLLAR, PIPE, STR_START, VS_START }

// Default mode
STR_START   : '\"'  -> pushMode(STR);
CS_START    : '$(' -> pushMode(CS);
VS_START    : '${' -> pushMode(VS);
PIPE        : '|';
ARG         : ~["|$\t ]+;
WS          : [\t ]+ -> skip;
DOLLAR      : '$';

// STR: String mode, space is preserved
mode STR;
STR_STOP        : '\"' -> popMode;
STR_CS_START    : '$(' -> type(CS_START), pushMode(CS);
STR_VS_START    : '${' -> type(VS_START), pushMode(VS);
TEXT            : ~["$]+;
STR_DOLLAR      : '$' -> type(DOLLAR);

// CS: Command substitution
mode CS;
CS_STOP     : ')' -> popMode;
CS_STR_START: '\"' -> type(STR_START), pushMode(STR);
CS_CS_START : '$(' -> type(CS_START), pushMode(CS);
CS_VS_START : '${' -> type(VS_START), pushMode(VS);
CS_PIPE     : '|' -> type(PIPE);
CS_ARG      : ~[)"$|\t ]+ -> type(ARG);
CS_DOLLAR   : '$' -> type(DOLLAR);
CS_WS       : [\t ]+ -> skip;

// VS: Variable substitution
mode VS;
VS_STOP     : '}' -> popMode;
VS_VARIABLE : [A-Z]+;