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
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.fusesource.fabric.commands.support.ZookeeperContentManager;
import org.jledit.ConsoleEditor;
import org.jledit.EditorFactory;
import org.fusesource.fabric.zookeeper.IZKClient;

import java.nio.charset.Charset;

@Command(name = "edit", scope = "zk", description = "Edits a znode's data", detailedDescription = "classpath:edit.txt")
public class Edit extends ZooKeeperCommandSupport {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Argument(description = "Path of the znode to get")
    String path;

    private EditorFactory editorFactory;

    @Override
    protected void doExecute(IZKClient zk) throws Exception {
        if (zk.exists(path) == null) {
            zk.createWithParents(path,"", ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        //Call the editor
        ConsoleEditor editor = editorFactory.create(getTerminal());
        editor.setTitle("Znode");
        editor.setContentManager(new ZookeeperContentManager(zk));
        editor.open(path);
        editor.setOpenEnabled(false);
        editor.start();
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
