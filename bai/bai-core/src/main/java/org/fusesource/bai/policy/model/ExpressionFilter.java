/*
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
package org.fusesource.bai.policy.model;

import org.fusesource.bai.policy.model.Constants.ScopeElement;

/**
 * A {@link Filter} whose filtering condition is an expression.
 *
 * @author Raul Kripalani
 */
public class ExpressionFilter extends Filter {

    private String language;
    private String expression;

    public ExpressionFilter(ScopeElement e, Policy p) {
        super(e, p);
    }

    public ExpressionFilter(ScopeElement e, Policy p, String language, String expression) {
        super(e, p);
        this.language = language;
        this.expression = expression;
    }

    public ExpressionFilter language(String language) {
        this.setLanguage(language);
        return this;
    }

    public ExpressionFilter expression(String expression) {
        this.setExpression(expression);
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String expressionLanguage) {
        this.language = expressionLanguage;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    // TODO: Implement
    public boolean matches(Object o) {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExpressionFilter [language=");
        builder.append(language);
        builder.append(", expression=");
        builder.append(expression);
        builder.append(", element=");
        builder.append(element);
        builder.append("]");
        return builder.toString();
    }

}
