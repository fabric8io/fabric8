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
            String pkg = generator.getInterfaces() + ".";
            String name = pkg + Utilities.toJavaClassName(base);
            Log.info("generating interface with name %s", name);
            JDefinedClass cls = generator.getCm()._class(name, INTERFACE);
            cls._implements(generator.getCm().ref(generator.getAmqpBaseType()));
        }
    }
}