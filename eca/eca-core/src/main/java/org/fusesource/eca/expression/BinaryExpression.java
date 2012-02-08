/**
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

package org.fusesource.eca.expression;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.util.ServiceHelper;

/**
 * An expression which performs an operation on two expression values.
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
     * Returns the symbol that represents this binary expression.
     * For example, addition is represented by <tt>+</tt>
     */
    public abstract String getExpressionSymbol();

    public void setRight(Expression expression) {
        right = expression;
    }

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
        ServiceHelper.startServices(left, right);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopServices(left, right);
    }

}
