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
     | WORD
     ;

variable_subst : '${' WORD '}'
               ;

command_subst : '$(' (pipeline | command) ')'
              ;

PIPE    : '|' ;
SEP     : ';' ;
WORD    : [a-zA-Z0-9]+ ; // TODO: This should be "everything except special characters"
WS      : [ \t]+ -> skip ;
NEWLINE : '\r'? '\n' ;