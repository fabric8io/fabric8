/**
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
package org.fusesource.insight.metrics.support;

import org.fusesource.insight.metrics.model.Query;
import org.fusesource.insight.metrics.model.QueryResult;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Renderer {

    private Map<Query, String> sources = new ConcurrentHashMap<Query, String>();
    private Map<String, CompiledTemplate> templates = new ConcurrentHashMap<String, CompiledTemplate>();
    private ParserContext context;

    public Renderer() {
        context = new ParserContext();
        try {
            context.addImport("toJson", ScriptUtils.class.getMethod("toJson", Object.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find method toJson", e);
        }
    }

    public String render(QueryResult qrs) throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("result", qrs);

        return TemplateRuntime.execute(getTemplate(qrs.getQuery()), context, vars).toString();
    }

    private CompiledTemplate getTemplate(Query set) throws IOException {
        String source = getTemplateSource(set);
        CompiledTemplate template = templates.get(source);
        if (template == null) {
            template = TemplateCompiler.compileTemplate(source, context);
            templates.put(source, template);
        }
        return template;
    }

    private String getTemplateSource(Query set) throws IOException {
        String source = sources.get(set);
        if (source == null) {
            if (set.getTemplate() != null) {
                source = IoUtils.loadFully(new URL(set.getTemplate()));
            }
            if (source == null) {
                URL url = getClass().getResource("/org/fusesource/insight/metrics/" + set.getName() + ".mvel");
                if (url == null) {
                    url = getClass().getResource("/org/fusesource/insight/metrics/default.mvel");
                }
                if (url != null) {
                    source = IoUtils.loadFully(url);
                } else {
                    throw new IllegalStateException("Could not find default template");
                }
            }
            sources.put(set, source);
        }
        return source;
    }


}
