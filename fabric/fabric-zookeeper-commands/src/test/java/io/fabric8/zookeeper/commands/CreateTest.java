package io.fabric8.zookeeper.commands;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.ACL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CreateTest extends Assert {

    CreateBuilder createBuilder = mock(CreateBuilder.class);

    CuratorFramework curator = mock(CuratorFramework.class);

    Create createCommand = new Create();

    @Before
    public void setUp() {
        createCommand.path = "/foo/bar";

        given(createBuilder.withMode(any(CreateMode.class))).willReturn(createBuilder);
        given(createBuilder.withACL(anyListOf(ACL.class))).willReturn(createBuilder);
        given(curator.create()).willReturn(createBuilder);
    }

    @Test
    public void shouldCreatePathWithoutData() throws Exception {
        // When
        createCommand.doExecute(curator);

        // Then
        verify(createBuilder).forPath(createCommand.path);
    }

    @Test
    public void shouldCreatePathWithData() throws Exception {
        // Given
        createCommand.data = "node data";

        // When
        createCommand.doExecute(curator);

        // Then
        verify(createBuilder).forPath(createCommand.path, createCommand.data.getBytes());
    }

}
