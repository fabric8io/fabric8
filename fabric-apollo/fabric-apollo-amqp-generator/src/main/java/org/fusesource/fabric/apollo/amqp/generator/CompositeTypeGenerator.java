package org.fusesource.fabric.apollo.amqp.generator;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Descriptor;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Field;
import org.fusesource.fabric.apollo.amqp.jaxb.schema.Type;

import static org.fusesource.fabric.apollo.amqp.generator.Utilities.*;

public class CompositeTypeGenerator {
    private final Generator generator;

    public CompositeTypeGenerator(Generator generator) {
        this.generator = generator;
    }

    void generateDescribedTypes() throws Exception {
        for ( Type type : generator.getComposites() ) {
            //String sectionPackage = sanitize(sections.get(type.getName()));
            String name = toJavaClassName(type.getName());
            //name = packagePrefix + "." + sectionPackage + "." + name;
            name = generator.getPackagePrefix() + "." + generator.getTypes() + "." + name;

            JDefinedClass cls = generator.getCm()._getClass(name);
            if ( cls == null ) {
                cls = generator.getCm()._class(name);
            }

            if ( type.getProvides() != null && !type.getProvides().equals(type.getName()) ) {
                cls._implements(generator.getCm().ref(generator.getPackagePrefix() + "." + generator.getInterfaces() + "." + toJavaClassName(type.getProvides())));
            }

            Log.info("");
            Log.info("Generating %s", cls.binaryName());

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

                } else if ( obj instanceof Field ) {
                    Field field = (Field) obj;
                    Log.info("Field name=%s type=%s", field.getName(), field.getType());
                    String fieldType = field.getType();
                    String fieldName = sanitize(field.getName());

                    if ( fieldType.equals("*") && field.getRequires() != null ) {
                        fieldType = generator.getPackagePrefix() + "." + generator.getInterfaces() + "." + toJavaClassName(field.getRequires());
                        Log.info("Trying required type %s", fieldType);
                    } else {
                        while (!generator.getMapping().containsKey(fieldType)) {
                            fieldType = generator.getRestrictedMapping().get(fieldType);
                            if ( fieldType == null ) {
                                break;
                            }
                            Log.info("Trying field type %s for field %s", fieldType, fieldName);
                        }
                    }

                    if ( fieldType == null ) {
                        fieldType = generator.getCompositeMapping().get(field.getType());
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
                            if ( c == null ) {
                                c = generator.getCm()._class(fieldType);
                            }
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

                    } else {
                        Log.info("Skipping field %s, type not found", field.getName());
                    }
                }
            }
        }
    }
}