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
package org.fusesource.fabric.zookeeper.commands;

import jline.Terminal;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.jledit.ConsoleEditor;
import org.jledit.EditorFactory;
import org.jledit.utils.Files;
import org.linkedin.zookeeper.client.IZKClient;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

@Command(name = "edit", scope = "zk", description = "Edits a znode's data", detailedDescription = "classpath:edit.txt")
public class Edit extends ZooKeeperCommandSupport {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Argument(description = "Path of the znode to get")
    String path;

    private EditorFactory editorFactory;

    @Override
    protected void doExecute(IZKClient zk) throws Exception {
        String data = zk.getStringData(path);
        File tmpFile = createTemporaryFile();
        Files.writeToFile(tmpFile, data, UTF_8);
        //Call the editor
        ConsoleEditor editor = editorFactory.create(getTerminal());
        editor.setTitle("Znode");
        editor.open(tmpFile, path);
        editor.setOpenEnabled(false);
        editor.start();
        data = Files.toString(tmpFile, UTF_8);
        zk.setData(path, data);
    }

    private File createTemporaryFile() throws IOException {
        File f = new File(System.getProperty("karaf.data") + "/editor/" + UUID.randomUUID());
        if (!f.exists() && !f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
            throw new IOException("Can't create file:" + f.getAbsolutePath());
        }
        return f;
    }

    /**
     * Gets the {@link jline.Terminal} from the current session.
     *
     * @return
     * @throws Exception
     */
    private Terminal getTerminal() throws Exception {
        Object terminalObject = session.get(".jline.terminal");
        if (terminalObject instanceof Terminal) {
            return (Terminal) terminalObject;

        }
        throw new IllegalStateException("Could not get Terminal from CommandSession.");
    }

    public EditorFactory getEditorFactory() {
        return editorFactory;
    }

    public void setEditorFactory(EditorFactory editorFactory) {
        this.editorFactory = editorFactory;
    }

}
