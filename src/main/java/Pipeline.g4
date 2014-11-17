grammar Pipeline;

prog : (pipeline | command)+ ;

pipeline : command PIPE pipeline
         | command PIPE command
         ;

command : expr arguments* SEP*
        ;

arguments : expr+
          ;

expr : variable_subst
     | command_subst
     | sq_string
     | fq_string
     | WORD
     ;

variable_subst : '${' WORD '}'
               ;

command_subst : '$(' (pipeline | command) ')'
              ;

fq_string : '"' fq_string_part* '"'
              ;

fq_string_part : variable_subst
               | command_subst
               | WORD
               ;

sq_string : SINGLE_QUOTED
          ;

// Desse går som eitt token

SINGLE_QUOTED : '\'' (~('\'' | '\\' | '\r' | '\n') | '\\' ('\'' | '\\'))* '\'' ;
//DOUBLE_QUOTED : '\"' (~('\"' | '\\' | '\r' | '\n') | '\\' ('\"' | '\\'))* '\"' ;
PIPE    : '|' ;
SEP     : ';' ;
WORD    : [a-zA-Z0-9]+ ; // TODO: This should be "everything except special characters": | ; " ' $
// WORD       : ~('|' | ';' | '\"' | '\'' | '$' | ' ' | '\t' )+ ;
// WS      : [ \t]+ -> skip ;
WS      : [ \t]+ -> channel(HIDDEN) ;
NEWLINE : '\r'? '\n' ;