/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 FuseSource Corporation, a Progress Software company. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").
 * You may not use this file except in compliance with the License. You can obtain
 * a copy of the License at http://www.opensource.org/licenses/CDDL-1.0.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at resources/META-INF/LICENSE.txt.
 *
 */

grammar Insight;
@header {
package org.fusesource.fabric.eca.parser;
import java.util.HashMap;

import org.apache.camel.Exchange;
import org.fusesource.fabric.eca.engine.*;
import org.fusesource.fabric.eca.eventcache.*;
import org.fusesource.fabric.eca.expression.*;
}

@lexer::header {package org.fusesource.fabric.eca.parser;}

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