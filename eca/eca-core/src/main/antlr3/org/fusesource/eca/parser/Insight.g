/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

grammar Insight;
@header {
package org.fusesource.eca.parser;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.fusesource.eca.engine.*;
import org.fusesource.eca.eventcache.*;
import org.fusesource.eca.expression.*;
}

@lexer::header {package org.fusesource.eca.parser;}

@members {

protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
   throw new MismatchedTokenException(ttype, input);
}

public Object recoverFromMismatchedSet(IntStream input,RecognitionException e, BitSet follow)throws RecognitionException {
  throw e;
}
}

@rulecatch {
  catch (RecognitionException e) {
      throw e;
  }
}




/* This will be the entry point of our parser. */
evaluate[EventEngine eventEngine, String defaultWindow, String threshold]  returns [Expression value]


    :   exp=beforeArfterExp[$eventEngine, $defaultWindow, $threshold] {$value = $exp.value;}
    ;

beforeArfterExp[EventEngine eventEngine, String defaultWindow, String threshold] returns [Expression value]
    : a1 = andOrExp[$eventEngine, $defaultWindow,$threshold] {$value = $a1.value;}
    ( BEFORE a2=andOrExp[$eventEngine, $defaultWindow,$threshold] {$value = LogicExpression.createBEFORE($threshold,$a1.value,$a2.value);}
    |  AFTER a2=andOrExp[$eventEngine, $defaultWindow,$threshold] {$value = LogicExpression.createAFTER($threshold,$a1.value,$a2.value);}
    )*
    ;


andOrExp [EventEngine eventEngine, String defaultWindow, String threshold] returns [Expression value]
    :    a1=atomExp [$eventEngine, $defaultWindow,$threshold]  {$value = $a1.value;}
         ( AND a2=atomExp[$eventEngine, $defaultWindow,$threshold] {$value = LogicExpression.createAND($threshold,$a1.value,$a2.value);}
         | OR  a2=atomExp[$eventEngine, $defaultWindow,$threshold] {$value = LogicExpression.createOR($threshold,$a1.value,$a2.value);}
         | NOT  a2=atomExp[$eventEngine, $defaultWindow,$threshold] {$value = LogicExpression.createNOT($threshold,$a1.value,$a2.value);}
         )* 
    ;


atomExp[EventEngine eventEngine, String defaultWindow,String threshold]  returns [Expression value]
    :    id=ID {
            String fromId = $id.text;
            $value=new CepExpression($eventEngine,fromId,$defaultWindow);
            }
    |    '(' exp=andOrExp[$eventEngine, $defaultWindow,$threshold] ')' {$value=$exp.value;}
    ;

AND  : 'AND' ;
OR   : 'OR' ;
BEFORE : 'BEFORE';
AFTER : 'AFTER';
NOT : 'NOT';


ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|':'|'//'|'?'|'='|'&')*
    ;



/* We're going to ignore all white space characters */
WS  
    :   (' ' | '\t' | '\r'| '\n') {$channel=HIDDEN;}
    ;