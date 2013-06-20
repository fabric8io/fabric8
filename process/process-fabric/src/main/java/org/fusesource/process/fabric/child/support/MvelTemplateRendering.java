package org.fusesource.process.fabric.child.support;

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
