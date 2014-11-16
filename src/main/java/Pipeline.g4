grammar Pipeline;

prog: command | pipeline  ;

pipeline    : command PIPE pipeline
            | command PIPE command
            ;

command : token arguments*
        ;

arguments   : token+
            ;

token   : variable_subst
        | command_subst
        | WORD
        ;

variable_subst  : '${' WORD '}'
                ;

command_subst   : '$(' (pipeline | command) ')'
                ;

PIPE    : '|' ;
WORD    : [a-zA-Z0-9]+ ;
WS      : [ \t]+ -> skip ;
NEWLINE : '\r'? '\n' ;