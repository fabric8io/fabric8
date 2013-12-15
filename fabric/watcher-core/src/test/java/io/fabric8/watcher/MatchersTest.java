package io.fabric8.watcher;

import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import io.fabric8.watcher.matchers.Logical;
import io.fabric8.watcher.matchers.Matchers;
import io.fabric8.watcher.matchers.Maven;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatchersTest {

    @Test
    public void testLogical() {
        PathMatcher matcher = Logical.parse("log:(org/{apache,fuse}/** or **) and(not **example**)");

        assertTrue(matcher.matches(Paths.get("org/fuse/foo.java")));
        assertFalse(matcher.matches(Paths.get("org/fuse/example.java")));
    }

    @Test
    public void testMavenGroupId() {
        PathMatcher matcher = Maven.parse("mvn:groupId:org.apache,org.fusesource");

        assertTrue(matcher.matches(Paths.get("org")));
        assertTrue(matcher.matches(Paths.get("org/apache")));
        assertTrue(matcher.matches(Paths.get("org/apache/foo.jar")));
        assertTrue(matcher.matches(Paths.get("org/fusesource")));
        assertTrue(matcher.matches(Paths.get("org/fusesource/foo/bar.jar")));
        assertFalse(matcher.matches(Paths.get("org/jboss")));
        assertFalse(matcher.matches(Paths.get("org/jboss/foo.jar")));
        assertFalse(matcher.matches(Paths.get("com/jboss/foo.jar")));
    }

    @Test
    public void testComplete() {
        PathMatcher matcher = Matchers.parse("log:mvn:groupId:org.apache,org.fusesource and glob:**/*.jar and not **example**");

        assertFalse(matcher.matches(Paths.get("org")));
        assertFalse(matcher.matches(Paths.get("org/apache")));
        assertTrue(matcher.matches(Paths.get("org/apache/foo.jar")));
        assertFalse(matcher.matches(Paths.get("org/fusesource")));
        assertTrue(matcher.matches(Paths.get("org/fusesource/foo/bar.jar")));
        assertFalse(matcher.matches(Paths.get("org/fusesource/foo/bar.xml")));
        assertFalse(matcher.matches(Paths.get("org/fusesource/example/bar.jar")));
        assertFalse(matcher.matches(Paths.get("org/jboss")));
        assertFalse(matcher.matches(Paths.get("org/jboss/foo.jar")));
        assertFalse(matcher.matches(Paths.get("com/jboss/foo.jar")));
    }

}
