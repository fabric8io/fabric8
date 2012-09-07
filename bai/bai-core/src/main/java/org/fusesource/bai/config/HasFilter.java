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
package org.fusesource.bai.config;

import org.apache.camel.Expression;
import org.apache.camel.builder.ExpressionClause;
import org.apache.camel.model.language.ConstantExpression;
import org.apache.camel.model.language.ELExpression;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.model.language.GroovyExpression;
import org.apache.camel.model.language.HeaderExpression;
import org.apache.camel.model.language.JXPathExpression;
import org.apache.camel.model.language.JavaScriptExpression;
import org.apache.camel.model.language.LanguageExpression;
import org.apache.camel.model.language.MethodCallExpression;
import org.apache.camel.model.language.MvelExpression;
import org.apache.camel.model.language.OgnlExpression;
import org.apache.camel.model.language.PhpExpression;
import org.apache.camel.model.language.PropertyExpression;
import org.apache.camel.model.language.PythonExpression;
import org.apache.camel.model.language.RefExpression;
import org.apache.camel.model.language.RubyExpression;
import org.apache.camel.model.language.SimpleExpression;
import org.apache.camel.model.language.SpELExpression;
import org.apache.camel.model.language.SqlExpression;
import org.apache.camel.model.language.TokenizerExpression;
import org.apache.camel.model.language.XPathExpression;
import org.apache.camel.model.language.XQueryExpression;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;

/**
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class HasFilter extends HasIdentifier {
    protected ExpressionDefinition filter;
    @XmlTransient
    private boolean unwrappedFilter;

    public HasFilter() {
    }

    public HasFilter(ExpressionDefinition filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        ExpressionDefinition exp = getFilter();
        return getClass().getSimpleName() + "(" + (exp != null ? exp.getLanguage() + ":" + exp.getExpression() : "") + ")";
    }

    public ExpressionDefinition getFilter() {
        if (filter != null && !unwrappedFilter) {
            unwrappedFilter = true;
            // lets unwrap the expression clause if used
            Expression exp = filter;
            if (filter != null && filter.getExpressionValue() != null) {
                exp = filter.getExpressionValue();
            }
            if (exp instanceof ExpressionClause) {
                ExpressionClause<?> clause = (ExpressionClause<?>) exp;
                if (clause.getExpressionType() != null) {
                    filter = clause.getExpressionType();
                }
            }
        }
        return filter;
    }

    @XmlElements({
            @XmlElement(required = false, name = "constant", type = ConstantExpression.class),
            @XmlElement(required = false, name = "el", type = ELExpression.class),
            @XmlElement(required = false, name = "expression", type = ExpressionDefinition.class),
            @XmlElement(required = false, name = "groovy", type = GroovyExpression.class),
            @XmlElement(required = false, name = "header", type = HeaderExpression.class),
            @XmlElement(required = false, name = "javaScript", type = JavaScriptExpression.class),
            @XmlElement(required = false, name = "jxpath", type = JXPathExpression.class),
            @XmlElement(required = false, name = "language", type = LanguageExpression.class),
            @XmlElement(required = false, name = "method", type = MethodCallExpression.class),
            @XmlElement(required = false, name = "mvel", type = MvelExpression.class),
            @XmlElement(required = false, name = "ognl", type = OgnlExpression.class),
            @XmlElement(required = false, name = "php", type = PhpExpression.class),
            @XmlElement(required = false, name = "property", type = PropertyExpression.class),
            @XmlElement(required = false, name = "python", type = PythonExpression.class),
            @XmlElement(required = false, name = "ref", type = RefExpression.class),
            @XmlElement(required = false, name = "ruby", type = RubyExpression.class),
            @XmlElement(required = false, name = "simple", type = SimpleExpression.class),
            @XmlElement(required = false, name = "spel", type = SpELExpression.class),
            @XmlElement(required = false, name = "sql", type = SqlExpression.class),
            @XmlElement(required = false, name = "tokenize", type = TokenizerExpression.class),
            // TODO
            // @XmlElement(required = false, name = "vtdxml", type = VtdXmlExpression.class),
            @XmlElement(required = false, name = "xpath", type = XPathExpression.class),
            @XmlElement(required = false, name = "xquery", type = XQueryExpression.class)
    })
    public void setFilter(ExpressionDefinition filter) {
        this.filter = filter;
        this.unwrappedFilter = false;
    }
}
