parser grammar AslanPipelineParser;

options { tokenVocab=AslanPipelineLexer; }

pipeline    : cmd (pipe cmd)*;
pipe        : PIPE;
cmd         : (arg|space)+;
arg         : (cs | vs | string | literal | ARG);
space       : WS+;

literal : LT_START LT_TEXT LT_STOP ;
string  : STR_START (cs | vs | text)* STR_STOP ;
cs      : CS_START pipeline CS_STOP ;
vs      : VS_START VS_VARIABLE VS_STOP ;
text    : (STR_TEXT | DOLLAR)+ ;