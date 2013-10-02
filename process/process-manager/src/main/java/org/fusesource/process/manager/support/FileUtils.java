/*
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
package org.fusesource.process.manager.support;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Executor;
import org.fusesource.process.manager.support.command.Command;
import org.fusesource.process.manager.support.command.CommandFailedException;
import org.fusesource.process.manager.support.command.Duration;

import com.google.common.base.Preconditions;

public class FileUtils {

    public static void extractArchive(File archiveFile, File targetDirectory, String extractCommand, Duration timeLimit, Executor executor)
            throws CommandFailedException {
        Preconditions.checkNotNull(archiveFile, "archiveFile is null");
        Preconditions.checkNotNull(targetDirectory, "targetDirectory is null");
        Preconditions.checkArgument(targetDirectory.isDirectory(), "targetDirectory is not a directory: " + targetDirectory.getAbsolutePath());

        final String[] commands = extractCommand.split(" ");
        final String[] args = Arrays.copyOf(commands, commands.length + 1);
        args[args.length - 1] = archiveFile.getAbsolutePath();
        new Command(args)
                .setDirectory(targetDirectory)
                .setTimeLimit(timeLimit)
                .execute(executor);
    }
}
