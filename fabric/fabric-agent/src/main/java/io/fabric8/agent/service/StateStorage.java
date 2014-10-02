/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.fabric8.common.util.json.JsonReader;
import io.fabric8.common.util.json.JsonWriter;

public abstract class StateStorage {

    public void load(State state) throws IOException {
        state.repositories.clear();
        state.requirements.clear();
        state.installedFeatures.clear();
        state.managedBundles.clear();
        try (
                InputStream is = getInputStream()
        ) {
            if (is != null) {
                Map json = (Map) JsonReader.read(is);
                state.bootDone.set((Boolean) json.get("bootDone"));
                state.repositories.addAll(toStringSet((Collection) json.get("repositories")));
                state.requirements.putAll(toStringStringSetMap((Map) json.get("features")));
                state.installedFeatures.putAll(toStringStringSetMap((Map) json.get("installed")));
                state.stateFeatures.putAll(toStringStringStringMapMap((Map) json.get("state")));
                state.managedBundles.putAll(toStringLongSetMap((Map) json.get("managed")));
                state.bundleChecksums.putAll(toLongLongMap((Map) json.get("checksums")));
            }
        }
    }

    public void save(State state) throws IOException {
        try (
                OutputStream os = getOutputStream()
        ) {
            if (os != null) {
                Map<String, Object> json = new HashMap<>();
                json.put("bootDone", state.bootDone.get());
                json.put("repositories", state.repositories);
                json.put("features", state.requirements);
                json.put("installed", state.installedFeatures);
                json.put("state", state.stateFeatures);
                json.put("managed", state.managedBundles);
                json.put("checksums", toStringLongMap(state.bundleChecksums));
                JsonWriter.write(os, json);
            }
        }
    }

    protected abstract InputStream getInputStream() throws IOException;

    protected abstract OutputStream getOutputStream() throws IOException;

    static Map<String, Map<String, String>> toStringStringStringMapMap(Map<?, ?> map) {
        Map<String, Map<String, String>> nm = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), toStringStringMap((Map) entry.getValue()));
        }
        return nm;
    }

    static Map<String, String> toStringStringMap(Map<?, ?> map) {
        Map<String, String> nm = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return nm;
    }

    static Map<String, Set<String>> toStringStringSetMap(Map<?, ?> map) {
        Map<String, Set<String>> nm = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), toStringSet((Collection) entry.getValue()));
        }
        return nm;
    }

    static Map<String, Set<Long>> toStringLongSetMap(Map<?, ?> map) {
        Map<String, Set<Long>> nm = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), toLongSet((Collection) entry.getValue()));
        }
        return nm;
    }

    static Set<String> toStringSet(Collection<?> col) {
        Set<String> ns = new TreeSet<>();
        for (Object o : col) {
            ns.add(o.toString());
        }
        return ns;
    }

    static Set<Long> toLongSet(Collection<?> set) {
        Set<Long> ns = new TreeSet<>();
        for (Object o : set) {
            ns.add(toLong(o));
        }
        return ns;
    }

    static Map<Long, Long> toLongLongMap(Map<?, ?> map) {
        Map<Long, Long> nm = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(toLong(entry.getKey()), toLong(entry.getValue()));
        }
        return nm;
    }

    static Map<String, Long> toStringLongMap(Map<?, ?> map) {
        Map<String, Long> nm = new HashMap<>();
        for (Map.Entry entry : map.entrySet()) {
            nm.put(entry.getKey().toString(), toLong(entry.getValue()));
        }
        return nm;
    }

    static long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        } else {
            return Long.parseLong(o.toString());
        }
    }

}
