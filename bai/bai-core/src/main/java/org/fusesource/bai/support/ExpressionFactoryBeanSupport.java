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
package org.fusesource.bai.support;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.spi.Language;
import org.apache.camel.util.ObjectHelper;

/**
 */
public abstract class ExpressionFactoryBeanSupport implements CamelContextAware {
    private String language;
    private String expression;
    private CamelContext camelContext;

    protected Language validateLanguage() {
        ObjectHelper.notNull(camelContext, "camelContext");
        ObjectHelper.notNull(language, "language");
        ObjectHelper.notNull(expression, "expression");

        Language languageImpl = camelContext.resolveLanguage(language);
        ObjectHelper.notNull(languageImpl, "language " + language + " could not be resolved");
        return languageImpl;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
