parser grammar IslandParser;

options { tokenVocab=IslandLexer; }

pipeline : cmd (pipe cmd)* ;
pipe : PIPE ;
cmd : arg+ ;
arg : (cs | vs | string | ARG) ;

string  : STR_START (cs | vs | text)* STR_STOP ;
cs  : CS_START CS_TEXT+ CS_STOP ;
vs  : VS_START VS_VARIABLE VS_STOP ;
text : (TEXT|DOLLAR)+ ;
