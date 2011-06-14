package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.*;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Descriptor;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Field;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;
import org.fusesource.hawtbuf.Buffer;

import java.util.Set;

import static org.fusesource.fabric.apollo.amqp.generator.Utilities.sanitize;
import static org.fusesource.fabric.apollo.amqp.generator.Utilities.toJavaClassName;

public class DescribedTypeGenerator {
    private final Generator generator;

    public DescribedTypeGenerator(Generator generator) {
        this.generator = generator;
    }

    void generateDescribedTypes() throws Exception {

        for ( String key : generator.getDescribed().keySet() ) {

            String className = generator.getDescribedJavaClass().get(key);
            JDefinedClass cls = generator.getCm()._getClass(className);
            cls._implements(generator.getCm().ref("org.fusesource.fabric.apollo.amqp.codec.interfaces.AmqpType"));

            JMethod write = cls.method(JMod.PUBLIC, generator.getCm().VOID, "write");
            write._throws(java.lang.Exception.class);
            write.param(java.io.DataOutput.class, "out");

            JMethod read = cls.method(JMod.PUBLIC, generator.getCm().VOID, "read");
            read._throws(java.lang.Exception.class);
            read.param(java.io.DataInput.class, "in");
            read.param(generator.getCm().INT, "size");
            read.param(generator.getCm().INT, "count");

            JMethod encodeTo = cls.method(JMod.PUBLIC, generator.getCm().VOID, "encodeTo");
            encodeTo._throws(java.lang.Exception.class);
            encodeTo.param(Buffer.class, "buffer");
            encodeTo.param(generator.getCm().INT, "offset");

            JMethod decodeFrom = cls.method(JMod.PUBLIC, generator.getCm().VOID, "decodeFrom");
            decodeFrom._throws(java.lang.Exception.class);
            decodeFrom.param(Buffer.class, "buffer");
            decodeFrom.param(generator.getCm().INT, "offset");
            decodeFrom.param(generator.getCm().INT, "size");
            decodeFrom.param(generator.getCm().INT, "count");

            Type type = generator.getDescribed().get(key);

            Log.info("");
            Log.info("Generating %s", cls.binaryName());

            for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
                if ( obj instanceof Field ) {
                    Field field = (Field) obj;
                    String fieldType = field.getType();
                    String fieldName = sanitize(field.getName());

                    if (generator.getPrimitives().containsKey(fieldType)) {
                        if ( fieldType.equals("*") && field.getRequires() != null ) {
                            String requiredType = field.getRequires();
                            if (generator.getProvides().contains(requiredType)) {
                                fieldType = generator.getPackagePrefix() + "." + generator.getInterfaces() + "." + toJavaClassName(field.getRequires());
                            } else {
                                fieldType = "java.lang.Object";
                            }
                        }
                    } else if (generator.getDescribed().containsKey(fieldType)) {
                        fieldType = generator.getDescribedJavaClass().get(field.getType());
                    } else if (generator.getRestricted().containsKey(fieldType)) {
                        fieldType = generator.getRestrictedMapping().get(field.getType());
                    }

                    if ( fieldType != null ) {
                        boolean array = false;
                        if ( field.getMultiple() != null && field.getMultiple().equals("true") ) {
                            array = true;
                        }

                        Class clazz = generator.getMapping().get(fieldType);
                        JClass c = null;
                        if ( clazz == null ) {
                            c = generator.getCm()._getClass(fieldType);
                        } else {
                            c = generator.getCm().ref(clazz.getName());
                        }
                        if ( array ) {
                            c = c.array();
                        }
                        Log.info("%s %s", c.binaryName(), fieldName);
                        JFieldVar fieldVar = cls.field(JMod.PROTECTED, c, fieldName);

                        String doc = field.getName() + ":" + field.getType();

                        if ( field.getLabel() != null ) {
                            doc += " - " + field.getLabel();
                        }
                        fieldVar.javadoc().add(doc);

                        JMethod getter = cls.method(JMod.PUBLIC, fieldVar.type(), "get" + toJavaClassName(fieldName));
                        getter.body()._return(JExpr._this().ref(fieldVar));

                        JMethod setter = cls.method(JMod.PUBLIC, generator.getCm().VOID, "set" + toJavaClassName(fieldName));
                        JVar param = setter.param(fieldVar.type(), fieldName);
                        setter.body().assign(JExpr._this().ref(fieldVar), param);

                    } else {
                        Log.info("Skipping field %s, type not found", field.getName());
                    }
                }
            }
        }

    }

    void createDescribedClasses(Generator generator) throws JClassAlreadyExistsException {
        Set<String> keys = generator.getDescribed().keySet();

        for(String key : keys) {
            Type type = generator.getDescribed().get(key);
            String className = generator.getPackagePrefix() + "." + generator.getTypes() + "." +  toJavaClassName(key);
            generator.getDescribedJavaClass().put(key, className);

            JDefinedClass cls = generator.cm._class(className);

            if ( type.getProvides() != null )  {
                cls._implements(generator.cm.ref(generator.getPackagePrefix() + "." + generator.getInterfaces() + "." + toJavaClassName(type.getProvides())));
            }
            for ( Object obj : type.getEncodingOrDescriptorOrFieldOrChoiceOrDoc() ) {
                if ( obj instanceof Descriptor ) {
                    Descriptor desc = (Descriptor) obj;
                    int mods = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
                    cls.field(mods, String.class, "SYMBOLIC_ID", JExpr.lit(desc.getName()));

                    String code = desc.getCode();
                    String category = code.split(":")[0];
                    String descriptorId = code.split(":")[1];

                    cls.field(mods, long.class, "CATEGORY", JExpr.lit(Integer.parseInt(category.substring(2), 16)));
                    cls.field(mods, long.class, "DESCRIPTOR_ID", JExpr.lit(Integer.parseInt(descriptorId.substring(2), 16)));
                    cls.field(mods, long.class, "NUMERIC_ID", JExpr.direct("CATEGORY << 32 | DESCRIPTOR_ID"));
                }
            }
        }
    }
}