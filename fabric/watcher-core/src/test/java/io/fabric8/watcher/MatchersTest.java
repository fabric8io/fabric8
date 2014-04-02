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
        PathMatcher matcher = Logical.parse("log:(org/{apache,foo}/** or **) and(not **example**)");

        assertTrue(matcher.matches(Paths.get("org/foo/foo.java")));
        assertFalse(matcher.matches(Paths.get("org/foo/example.java")));
    }

    @Test
    public void testMavenGroupId() {
        PathMatcher matcher = Maven.parse("mvn:groupId:org.apache,org.foo");

        assertTrue(matcher.matches(Paths.get("org")));
        assertTrue(matcher.matches(Paths.get("org/apache")));
        assertTrue(matcher.matches(Paths.get("org/apache/foo.jar")));
        assertTrue(matcher.matches(Paths.get("org/foo")));
        assertTrue(matcher.matches(Paths.get("org/foo/foo/bar.jar")));
        assertFalse(matcher.matches(Paths.get("org/jboss")));
        assertFalse(matcher.matches(Paths.get("org/jboss/foo.jar")));
        assertFalse(matcher.matches(Paths.get("com/jboss/foo.jar")));
    }

    @Test
    public void testComplete() {
        PathMatcher matcher = Matchers.parse("log:mvn:groupId:org.apache,org.foo and glob:**/*.jar and not **example**");

        assertFalse(matcher.matches(Paths.get("org")));
        assertFalse(matcher.matches(Paths.get("org/apache")));
        assertTrue(matcher.matches(Paths.get("org/apache/foo.jar")));
        assertFalse(matcher.matches(Paths.get("org/foo")));
        assertTrue(matcher.matches(Paths.get("org/foo/foo/bar.jar")));
        assertFalse(matcher.matches(Paths.get("org/foo/foo/bar.xml")));
        assertFalse(matcher.matches(Paths.get("org/foo/example/bar.jar")));
        assertFalse(matcher.matches(Paths.get("io/fabric8")));
        assertFalse(matcher.matches(Paths.get("io/fabric8/foo.jar")));
        assertFalse(matcher.matches(Paths.get("io/fabric8/foo.jar")));
    }

}
