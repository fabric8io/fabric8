package io.fabric8.maven.stubs;

import java.io.File;

public class CreateProfileZipWarProjectStub extends AbstractProjectStub {

    @Override
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/profile-zip/war-test/" );
    }
}
