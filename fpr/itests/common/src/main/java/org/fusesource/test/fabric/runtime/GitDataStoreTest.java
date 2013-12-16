/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.fusesource.test.fabric.runtime;

import io.fabric8.api.FabricService;
import io.fabric8.git.GitNode;
import io.fabric8.git.internal.GitDataStore;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.internal.ZooKeeperGroup;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.Constants;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test basic {@link GitDataStore} functionality
 *
 * @author thomas.diesler@jbos.com
 * @since 09-Dec-2013
 */
@RunWith(Arquillian.class)
public class GitDataStoreTest  {

    static final CredentialsProvider CREDENTIALS_PROVIDER = new UsernamePasswordCredentialsProvider("admin", "admin");

    private Runtime runtime;
    private ModuleContext syscontext;
    private FabricService fabricService;
    private CuratorFramework curatorFramework;
    private GitDataStore gitDataStore;

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("git-data-store");
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(archive.getName());
                    builder.addBundleVersion("1.0.0");
                    builder.addManifestHeader(Constants.GRAVIA_ENABLED, Boolean.TRUE.toString());
                    builder.addImportPackages(RuntimeLocator.class, FabricService.class, GitDataStore.class, GroupListener.class, CuratorFramework.class);
                    // [FIXME] access to internal ZooKeeperGroup
                    builder.addImportPackages(ZooKeeperGroup.class, ZkPath.class, GitNode.class, ZooKeeperUtils.class, Git.class, UsernamePasswordCredentialsProvider.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(archive.getName(), "1.0.0");
                    builder.addManifestHeader("Dependencies", "org.jboss.gravia,io.fabric8.api,io.fabric8.git,io.fabric8.groups,io.fabric8.zookeeper");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Before
    public void before() throws Exception {
        runtime = RuntimeLocator.getRequiredRuntime();
        syscontext = runtime.getModule(0).getModuleContext();
        fabricService = syscontext.getService(syscontext.getServiceReference(FabricService.class));
        curatorFramework = syscontext.getService(syscontext.getServiceReference(CuratorFramework.class));
        gitDataStore = (GitDataStore) fabricService.getDataStore();
    }

    @Test
    public void testGitDataStoreAvailable() throws Exception {
        Assert.assertNotNull("GitDataStore not null", gitDataStore);
    }

    @Test
    public void testMasterAccess() throws Exception {
        URL masterURL = new URL(getMasterUrl(curatorFramework));
        Assert.assertNotNull("Master URL connected", masterURL);
        Git git = gitDataStore.getGit();
        Iterable<Ref> refs = git.lsRemote().setCredentialsProvider(CREDENTIALS_PROVIDER).call();
        Assert.assertTrue("Remote refs available", refs.iterator().hasNext());
    }

    // Waits until the master url becomes available & returns it.
    private String getMasterUrl(CuratorFramework curator) throws InterruptedException, URISyntaxException {
        Group<GitNode> group = new ZooKeeperGroup<GitNode>(curator, ZkPath.GIT.getPath(), GitNode.class);
        final CountDownLatch latch = new CountDownLatch(1);
        group.add(new GroupListener<GitNode>() {
            @Override
            public void groupEvent(Group<GitNode> group, GroupEvent event) {
                if (group.master() != null && group.master().getUrl() != null) {
                    latch.countDown();
                }
            }
        });
        group.start();
        Assert.assertTrue("Master URL connected", latch.await(10, TimeUnit.SECONDS));
        return ZooKeeperUtils.getSubstitutedData(curator, group.master().getUrl());
    }
}
