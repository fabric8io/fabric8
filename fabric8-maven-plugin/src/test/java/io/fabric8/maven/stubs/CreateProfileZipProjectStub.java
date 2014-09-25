package io.fabric8.maven.stubs;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CreateProfileZipProjectStub extends MavenProjectStub {

    public CreateProfileZipProjectStub() {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model;
        File pomFile = new File(getBasedir(), "pom.xml");
        try
        {

            model = pomReader.read( ReaderFactory.newXmlReader(pomFile) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        setGroupId( model.getGroupId() );
        setArtifactId( model.getArtifactId() );
        setVersion( model.getVersion() );
        setName( model.getName() );
        setUrl( model.getUrl() );
        setPackaging( model.getPackaging() );
        setArtifact(new CreateProfileArtifactStub(getGroupId(), getArtifactId(), getVersion(), getPackaging()));
        setFile(pomFile);

        Build build = new Build();
        build.setFinalName( model.getArtifactId() );
        build.setDirectory( getBasedir() + "/target" );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setOutputDirectory( getBasedir() + "/target/classes" );
        build.setTestSourceDirectory( getBasedir() + "/src/test/java" );
        build.setTestOutputDirectory( getBasedir() + "/target/test-classes" );
        setBuild( build );

        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );

        List testCompileSourceRoots = new ArrayList();
        testCompileSourceRoots.add( getBasedir() + "/src/test/java" );
        setTestCompileSourceRoots( testCompileSourceRoots );
    }

    public File getBasedir() {
        return super.getBasedir();
    }
}
