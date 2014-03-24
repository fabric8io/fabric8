package io.fabric8.process.fabric.child.tasks;

import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.config.ProcessConfig;

import java.io.File;

public class CompositeTask implements InstallTask {

    private final InstallTask[] subTasks;

    public CompositeTask(InstallTask... subTasks) {
        this.subTasks = subTasks;
    }

    @Override
    public void install(ProcessConfig config, int id, File installDir) throws Exception {
        for (InstallTask task : subTasks) {
            task.install(config, id, installDir);
        }
    }
}
