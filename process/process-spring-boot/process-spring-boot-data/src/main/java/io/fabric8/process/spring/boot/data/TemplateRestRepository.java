/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.process.spring.boot.data;

import com.google.common.base.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;

public class TemplateRestRepository<T, ID extends java.io.Serializable> implements RestRepository<T, ID> {

    private final Class<T> entityClass;

    private final Class<T[]> arrayOfEntityClass;

    private final URL url;

    private final RestTemplate restTemplate;

    public TemplateRestRepository(Class<T> entityClass, String url) throws MalformedURLException {
       this(entityClass, new URL(url));
    }

    public TemplateRestRepository(Class<T> entityClass, URL url) {
        this.entityClass = entityClass;
        this.arrayOfEntityClass = (Class<T[]>) Array.newInstance(entityClass, 0).getClass();
        this.url = url;
        restTemplate = new RestTemplate();
    }

    @Override
    public Iterable<T> findByQuery(AbstractQuery query) {
        return asList(restTemplate.postForObject(url + "-ops/searchByQuery", query, arrayOfEntityClass));
    }

    @Override
    public long countByQuery(AbstractQuery query) {
        return restTemplate.postForObject(url + "-ops/countByQuery", query, Long.class);
    }

    @Override
    public Iterable<T> findAll(Sort orders) {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public <S extends T> S save(S s) {
        try {
            URI location = restTemplate.postForLocation(url.toURI(), s);
            Serializable id = Long.parseLong(location.toString().replace(url + "/", ""));
            updateId(s, (ID) id);
            return s;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        return transform(entities, new Function<S, S>() {
            @Override
            public S apply(S entity) {
                return save(entity);
            }
        });
    }

    @Override
    public T findOne(ID id) {
        T s = restTemplate.getForObject(url + "/" + id, entityClass);
        updateId(s, id);
        return s;
    }

    @Override
    public boolean exists(ID id) {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public Iterable<T> findAll() {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public void delete(ID id) {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public void delete(T t) {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public void delete(Iterable<? extends T> ts) {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Not *yet* supported.");
    }

    // Helpers

    protected void updateId(T entity, ID id) {
        try {
            Field idField = entityClass.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
