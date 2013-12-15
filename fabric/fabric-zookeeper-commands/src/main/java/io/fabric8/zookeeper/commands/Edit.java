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
package io.fabric8.zookeeper.commands;

import jline.Terminal;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.zookeeper.ZooDefs;
import io.fabric8.commands.support.ZookeeperContentManager;
import org.jledit.ConsoleEditor;
import org.jledit.EditorFactory;

import java.nio.charset.Charset;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;

@Command(name = "edit", scope = "zk", description = "Edits a znode's data", detailedDescription = "classpath:edit.txt")
public class Edit extends ZooKeeperCommandSupport {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @Argument(description = "Path of the znode to get")
    String path;

    private EditorFactory editorFactory;

    @Override
    protected void doExecute(CuratorFramework curator) throws Exception {
        if (exists(curator, path) == null) {
            curator.create().creatingParentsIfNeeded().withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath(path);
        }
        //Call the editor
        ConsoleEditor editor = editorFactory.create("simple",getTerminal(), System.in, System.out);
        editor.setTitle("Znode");
        editor.setContentManager(new ZookeeperContentManager(curator));
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
