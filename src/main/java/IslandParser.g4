parser grammar IslandParser;

options { tokenVocab=IslandLexer; }

pipeline : cmd (pipe cmd)* ;
pipe : PIPE ;
cmd : arg+ ;
arg : (cs | vs | string | ARG) ;

string  : STR_START (cs | vs | text)* STR_STOP ;
cs  : '$(' CS_TEXT+ ')' ;
vs  : '${' VS_VARIABLE '}' ;
text : (TEXT|DOLLAR)+ ;
