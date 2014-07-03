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

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.springframework.data.domain.Sort.Direction.ASC;

@Data
public abstract class AbstractQuery {

    @NonNull
    private int page = 0;

    @NonNull
    private int size = 25;

    @NonNull
    private Sort.Direction sortDirection = ASC;

    @NonNull
    private String[] orderBy = {"id"};

    public PageRequest pageRequest() {
        return new PageRequest(page, size, sortDirection, orderBy);
    }

}
