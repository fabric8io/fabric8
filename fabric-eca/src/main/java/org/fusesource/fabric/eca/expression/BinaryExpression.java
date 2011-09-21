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
package org.fusesource.fabric.eca.expression;


import org.apache.camel.CamelContext;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.util.ServiceHelper;

/**
 * An expression which performs an operation on two expression values.
 *
 * @version $Revision: 1.2 $
 */
public abstract class BinaryExpression extends ServiceSupport implements Expression {
    protected Expression left;
    protected Expression right;


    public BinaryExpression(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }


    /**
     * @see Object#toString()
     */
    public String toString() {
        return "(" + left.toString() + " " + getExpressionSymbol() + " " + right.toString() + ")";
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
     * Returns the symbol that represents this binary expression.  For example, addition is
     * represented by "+"
     */
    public abstract String getExpressionSymbol();

    /**
     * @param expression
     */
    public void setRight(Expression expression) {
        right = expression;
    }

    /**
     * @param expression
     */
    public void setLeft(Expression expression) {
        left = expression;
    }


    public void validate(CamelContext context) {
        Expression left = getLeft();
        if (left != null) {
            left.validate(context);
        }
        Expression right = getRight();
        if (right != null) {
            right.validate(context);
        }
    }

    /**
     * @return the key used to create the expression, or multiple, comma separated keys
     */
    public String getFromIds() {
        String result = "";
        if (left != null) {
            result += left.getFromIds();
        }

        if (right != null) {
            result += ",";
            result += right.getFromIds();
        }
        return result;
    }


    protected void doStart() throws Exception {
        ServiceHelper.startService(left);
        ServiceHelper.startService(right);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopService(left);
        ServiceHelper.stopService(right);
    }

}
