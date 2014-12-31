lexer grammar WingPipelineLexer;

tokens { ARG, CS_START, DOLLAR, LT_START, PIPE, STR_START, VS_START, WS }

// Default mode
LT_START    : '\'' -> pushMode(LT);
STR_START   : '\"' -> pushMode(STR);
CS_START    : '$(' -> pushMode(CS);
VS_START    : '${' -> pushMode(VS);
PIPE        : '|';
ARG         : ~["'|$\t ]+;
WS          : [\t ]+;
DOLLAR      : '$';

// LT: Literal mode, space is preserved, nothing is expanded
mode LT;
LT_STOP     : '\'' -> popMode;
LT_TEXT     : ~[']+;

// STR: String mode, space is preserved, command- and variable- substitutions are expanded
mode STR;
STR_STOP        : '\"' -> popMode;
STR_CS_START    : '$(' -> type(CS_START), pushMode(CS);
STR_VS_START    : '${' -> type(VS_START), pushMode(VS);
STR_TEXT        : ~["$]+;
STR_DOLLAR      : '$' -> type(DOLLAR);

// CS: Command substitution
mode CS;
CS_STOP     : ')' -> popMode;
CS_LT_START : '\'' -> type(LT_START), pushMode(LT);
CS_STR_START: '\"' -> type(STR_START), pushMode(STR);
CS_CS_START : '$(' -> type(CS_START), pushMode(CS);
CS_VS_START : '${' -> type(VS_START), pushMode(VS);
CS_PIPE     : '|' -> type(PIPE);
CS_ARG      : ~[)'"$|\t ]+ -> type(ARG);
CS_DOLLAR   : '$' -> type(DOLLAR);
CS_WS       : [\t ]+ -> type(WS);

// VS: Variable substitution
mode VS;
VS_STOP     : '}' -> popMode;
VS_VARIABLE : [A-Z0-9_]+;