/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.cdi.eager;

import io.fabric8.annotations.Eager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Eagerly loads any beans which have the {@link Eager} annotation which also
 * have the {@link Singleton} or {@link javax.enterprise.context.ApplicationScoped} annotations.
 */
public class EagerCDIExtension implements Extension {

    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        AnnotationLiteral<Eager> annotationLiteral = new AnnotationLiteral<Eager>() {};
        Set<Bean<?>> beans = beanManager.getBeans(Object.class, annotationLiteral);
        for (Bean<?> bean : beans) {
            Class<?> beanClass = bean.getBeanClass();
            if (beanClass.isAnnotationPresent(ApplicationScoped.class) || beanClass.isAnnotationPresent(Singleton.class)) {
                beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
            }
        }
    }
}
