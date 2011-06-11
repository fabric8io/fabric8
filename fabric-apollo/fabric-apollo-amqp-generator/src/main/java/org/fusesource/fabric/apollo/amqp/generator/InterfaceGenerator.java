package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;

import java.io.IOException;

import static com.sun.codemodel.ClassType.*;

public class InterfaceGenerator {
    private final Generator generator;

    public InterfaceGenerator(Generator generator) {
        this.generator = generator;
    }

    void generateAbstractBases() throws JClassAlreadyExistsException, IOException {
        for ( String base : generator.getProvides() ) {
            String pkg = generator.getPackagePrefix() + "." + generator.getInterfaces() + ".";
            String name = pkg + Utilities.toJavaClassName(base);
            JDefinedClass cls = generator.getCm()._class(name, INTERFACE);
        }
    }
}