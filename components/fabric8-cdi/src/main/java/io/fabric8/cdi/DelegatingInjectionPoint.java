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
package io.fabric8.cdi;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

public class DelegatingInjectionPoint  implements InjectionPoint {
    
    private final InjectionPoint delegate;

    public DelegatingInjectionPoint(InjectionPoint delegate) {
        this.delegate = delegate;
    }

    public Type getType() {
        return delegate.getType();
    }

    public Bean<?> getBean() {
        return delegate.getBean();
    }

    public boolean isDelegate() {
        return delegate.isDelegate();
    }

    public Member getMember() {
        return delegate.getMember();
    }

    public Set<Annotation> getQualifiers() {
        return delegate.getQualifiers();
    }

    public boolean isTransient() {
        return delegate.isTransient();
    }

    public Annotated getAnnotated() {
        return delegate.getAnnotated();
    }
}
