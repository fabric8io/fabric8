package org.fusesource.process.fabric.child.tasks;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import org.fusesource.process.fabric.child.support.MvelTemplateRendering;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.config.ProcessConfig;
import org.jledit.utils.Files;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ApplyConfigurationTask implements InstallTask {

    private final Map<String, Object> variables;
    private final Map<String, String> configuration;

    public ApplyConfigurationTask(Map<String, String> configuration, Map<String, Object> variables) {
        this.configuration = configuration;
        this.variables = variables;
    }

    @Override
    public void install(ProcessConfig config, int id, File installDir) throws Exception {
        Map<String, String> rendered = Maps.transformValues(configuration, new MvelTemplateRendering(variables));
        for (Map.Entry<String, String> entry : rendered.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            copyToContent(installDir, path, content);
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
        Files.writeToFile(target, content, Charsets.UTF_8);
    }
}
