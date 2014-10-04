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
package io.fabric8.process.manager.support;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Executor;

import io.fabric8.process.manager.support.command.Command;
import io.fabric8.process.manager.support.command.Duration;
import io.fabric8.process.manager.support.command.CommandFailedException;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
    private static final transient Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public static void extractArchive(File archiveFile, File targetDirectory, String extractCommand, Duration timeLimit, Executor executor)
            throws CommandFailedException {
        Preconditions.checkNotNull(archiveFile, "archiveFile is null");
        Preconditions.checkNotNull(targetDirectory, "targetDirectory is null");
        Preconditions.checkArgument(targetDirectory.isDirectory(), "targetDirectory is not a directory: " + targetDirectory.getAbsolutePath());

        final String[] commands = splitCommands(extractCommand);
        final String[] args = Arrays.copyOf(commands, commands.length + 1);
        args[args.length - 1] = archiveFile.getAbsolutePath();
        LOG.info("Extracting archive with commands: " + Arrays.asList(args));

        new Command(args)
                .setDirectory(targetDirectory)
                .setTimeLimit(timeLimit)
                .execute(executor);
    }

    /**
     * Splits the given command into an array of arguments.
     *
     * NOTE does not deal with quoted strings!
     */
    public static String[] splitCommands(String extractCommand) {
        return extractCommand.split(" ");
    }
}
