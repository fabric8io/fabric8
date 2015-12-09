/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.cdi.bean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ProducerBean<X> extends BaseBean<X> {

    private final Producer<X> producer;

    public ProducerBean(String name, Type type, Producer<X> producer, Annotation... annotations) {
        super(name, type, annotations);
        this.producer = producer;
    }

    public Producer<X> getProducer() {
        return producer;
    }

    @Override
    public X create(CreationalContext<X> creationalContext) {
        if (producer == null) {
            throw new IllegalStateException("No producer has been specified");
        }
        return producer.produce(creationalContext);
    }

    @Override
    public void destroy(X instance, CreationalContext<X> creationalContext) {
        producer.dispose(instance);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProducerBean that = (ProducerBean) o;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getBeanClass() != null ? !getBeanClass().equals(that.getBeanClass()) : that.getBeanClass() != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getBeanClass() != null ? getBeanClass().hashCode() : 0);
        return result;
    }
}
