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
import org.apache.camel.Predicate;
import org.apache.camel.builder.ExpressionClause;
import org.apache.camel.model.language.ExpressionDefinition;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
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

    @XmlElementRef
    public void setFilter(ExpressionDefinition filter) {
        this.filter = filter;
        this.unwrappedFilter = false;
    }
}
