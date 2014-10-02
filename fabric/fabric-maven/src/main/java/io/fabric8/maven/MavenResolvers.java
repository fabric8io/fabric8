package io.fabric8.maven;

import java.util.Dictionary;

import io.fabric8.maven.url.internal.AetherBasedResolver;
import io.fabric8.maven.util.MavenConfigurationImpl;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertiesPropertyResolver;

public final class MavenResolvers {

    public static MavenResolver createMavenResolver(Dictionary<String, String> properties, String pid) {
        PropertiesPropertyResolver syspropsResolver = new PropertiesPropertyResolver(System.getProperties());
        DictionaryPropertyResolver propertyResolver = new DictionaryPropertyResolver(properties, syspropsResolver);
        MavenConfigurationImpl config = new MavenConfigurationImpl(propertyResolver, pid);
        return new AetherBasedResolver(config);
    }


    private MavenResolvers() { }
}
