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

import org.apache.camel.Predicate;
import org.apache.camel.spi.Language;
import org.springframework.beans.factory.FactoryBean;

/**
 * A factory bean of Predicate objects for easier configuration in Spring XML
 */
public class PredicateFactoryBean extends ExpressionFactoryBeanSupport implements FactoryBean<Predicate> {

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Class<?> getObjectType() {
        return Predicate.class;
    }

    @Override
    public Predicate getObject() throws Exception {
        Language languageImpl = validateLanguage();
        return languageImpl.createPredicate(getExpression());
    }

}
