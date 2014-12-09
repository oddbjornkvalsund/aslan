parser grammar IslandParser;

options { tokenVocab=IslandLexer; }

string  : (cs | vs | text)* ;
text : TEXT ;
cs  : '$(' CS_TEXT ')' ;
vs  : '${' VS_VARIABLE '}' ;