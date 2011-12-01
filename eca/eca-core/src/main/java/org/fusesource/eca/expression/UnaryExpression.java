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
package org.fusesource.eca.expression;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.util.ServiceHelper;

/**
 * An expression which performs an operation on one expression value.
 */
public abstract class UnaryExpression extends ServiceSupport implements Expression {
    protected Expression letter;

    public UnaryExpression(Expression letter) {
        this.letter = letter;
    }

    public Expression getLetter() {
        return letter;
    }

    public void setLetter(Expression letter) {
        this.letter = letter;
    }

    public String toString() {
        return "(" + letter.toString() + " " + getExpressionSymbol() + " " + letter.toString() + ")";
    }

    /**
     * TODO: more efficient hashCode()
     *
     * @see Object#hashCode()
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * TODO: more efficient hashCode()
     *
     * @see Object#equals(Object)
     */
    public boolean equals(Object o) {
        if (o == null || !this.getClass().equals(o.getClass())) {
            return false;
        }
        return toString().equals(o.toString());

    }

    /**
     * Returns the symbol that represents this binary expression.
     * For example, addition is represented by <tt>+</tt>
     */
    public abstract String getExpressionSymbol();

    public void validate(CamelContext context) {
        if (letter != null) {
            letter.validate(context);
        }
    }

    public String getFromIds() {
        String result = "";
        if (letter != null) {
            result = letter.getFromIds();
        }
        return result;
    }

    protected void doStart() throws Exception {
        ServiceHelper.startService(letter);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopService(letter);
    }

}
