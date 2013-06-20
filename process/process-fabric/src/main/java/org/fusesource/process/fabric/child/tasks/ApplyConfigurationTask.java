package org.fusesource.process.fabric.child.tasks;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import org.fusesource.process.fabric.child.support.MvelPredicate;
import org.fusesource.process.fabric.child.support.MvelTemplateRendering;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.config.ProcessConfig;
import org.fusesource.process.manager.support.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ApplyConfigurationTask implements InstallTask {

    private final Map<String, Object> variables;
    private final Map<String, String> configuration;

    private final MvelPredicate isTemplate = new MvelPredicate();


    public ApplyConfigurationTask(Map<String, String> configuration, Map<String, Object> variables) {
        this.configuration = configuration;
        this.variables = variables;
    }

    @Override
    public void install(ProcessConfig config, int id, File installDir) throws Exception {
        Map<String, String> templates = Maps.filterKeys(configuration, isTemplate);
        Map<String, String> plainFiles = Maps.difference(configuration, templates).entriesOnlyOnLeft();
        Map<String, String> renderedTemplates = Maps.transformValues(templates, new MvelTemplateRendering(variables));
        File baseDir = ProcessUtils.findInstallDir(installDir);
        applyTemplates(renderedTemplates, baseDir);
        applyPlainConfiguration(plainFiles, baseDir);

    }

    private void applyTemplates(Map<String, String> templates, File installDir) throws IOException {
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            String resourcePath = path.substring(path.indexOf("/"));
            resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf(MvelPredicate.MVEN_EXTENTION));
            copyToContent(installDir, resourcePath, content);
        }
    }

    private void applyPlainConfiguration(Map<String, String> configuration, File installDir) throws IOException {
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            int slashIndex = path.indexOf("/");
            String resourcePath = slashIndex > 0 ? path.substring(slashIndex): path;
            copyToContent(installDir, resourcePath, content);
        }
    }

    private void copyToContent(File baseDir, String name, String content) throws IOException {
        File target = new File(baseDir, name);
        if (!target.exists() && !target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
            throw new IOException("Directory: " + target.getParentFile().getAbsolutePath() + " can't be created");
        } else if (target.isDirectory()) {
            throw new IOException("Can't write to : " + target.getAbsolutePath() + ". It's a directory");
        } else if (!target.exists() && !target.createNewFile()) {
            throw new IOException("Failed to create file: " + target.getAbsolutePath() + ".");
        }
        Files.write(content.getBytes(Charsets.UTF_8), target);
    }
}
