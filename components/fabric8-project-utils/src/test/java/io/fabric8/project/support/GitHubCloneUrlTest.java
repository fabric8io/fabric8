package io.fabric8.project.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GitHubCloneUrlTest {

  @Test
  public void should_return_url_without_api() {
    String expected = "https://github.com";
    String address = "https://api.github.com";
    String actual = BuildConfigHelper.resolveToRoot(address);
    assertEquals(expected, actual);
  }

  @Test
  public void should_return_url_as_it_is() {
    String expected = "https://gogs.vgrant.f8";
    String address = "https://gogs.vgrant.f8";
    String actual = BuildConfigHelper.resolveToRoot(address);
    assertEquals(expected, actual);
  }
}
