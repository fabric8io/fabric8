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
package io.fabric8.process.manager.support.mvel;

import com.google.common.base.Function;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

import java.util.Map;

public class MvelTemplateRendering implements Function<String, String> {

    private final ParserContext parserContext = new ParserContext();
    private final Map<String, Object> variables;

    public MvelTemplateRendering(Map<String, Object> variables) {
        this.variables = variables;
    }

    @Override
    public String apply(java.lang.String template) {
        CompiledTemplate compiledTemplate = TemplateCompiler.compileTemplate(template, parserContext);
        return TemplateRuntime.execute(compiledTemplate, parserContext, variables).toString();
    }
}
