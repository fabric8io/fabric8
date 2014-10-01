package io.fabric8.maven.stubs;

import java.io.File;

public class ForkedProjectAttachmentsProjectStub extends AbstractProjectStub {

    @Override
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/forked-project-attachments/mule-test/" );
    }
}
