package io.fabric8.api.jmx;

import org.apache.commons.beanutils.PropertyUtils;
import io.fabric8.api.*;

import java.beans.PropertyDescriptor;
import java.util.*;

/**
 * @author Stan Lewis
 */
public class BeanUtils {

    public static List<String> getFields(Class clazz) {
        List<String> answer = new ArrayList<String>();

        try {
            for (PropertyDescriptor desc : PropertyUtils.getPropertyDescriptors(clazz)) {
                if (desc.getReadMethod() != null) {
                    answer.add(desc.getName());
                }
            }
        } catch (Exception e) {
            throw new FabricException("Failed to get property descriptors for " + clazz.toString(), e);
        }

        // few tweaks to maintain compatibility with existing views for now...
        if (clazz.getSimpleName().equals("Container")) {
            answer.add("parentId");
            answer.add("versionId");
            answer.add("profileIds");
            answer.add("childrenIds");
            answer.remove("fabricService");
        } else if (clazz.getSimpleName().equals("Profile")) {
            answer.add("id");
            answer.add("parentIds");
            answer.add("childIds");
            answer.add("containerCount");
            answer.add("containers");
            answer.add("fileConfigurations");
        } else if (clazz.getSimpleName().equals("Version")) {
            answer.add("id");
            answer.add("defaultVersion");
        }

        return answer;
    }

    public static void setValue(Object instance, String property, Object value) {
        try {
            org.apache.commons.beanutils.BeanUtils.setProperty(instance, property, value);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set property " + property + " on " + instance.getClass().getName(), t);
        }
    }

    public static Object getValue(Object instance, String property) {
        try {
            return org.apache.commons.beanutils.BeanUtils.getProperty(instance, property);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set property " + property + " on " + instance.getClass().getName(), t);
        }
    }

    public static Map<String, Object> convertProfileToMap(FabricService service, Profile profile, List<String> fields) {

        Map<String, Object> answer = new HashMap<String, Object>();

        for(String field: fields) {

            if (field.equalsIgnoreCase("configurations") ||
                field.equalsIgnoreCase("fileConfigurations")) {

                answer.put(field, fetchConfigurations(profile));

            } else if (field.equalsIgnoreCase("childIds")) {

                answer.put(field, fetchChildIds(service, profile));

            } else if (field.equalsIgnoreCase("containers") ||
                       field.equalsIgnoreCase("associatedContainers")) {

                answer.put(field, fetchContainers(service, profile));

            } else if (field.equalsIgnoreCase("containerCount")) {

                answer.put(field, fetchContainerCount(profile));

            } else if (field.equalsIgnoreCase("parentIds") ||
                       field.equalsIgnoreCase("parents")) {

                answer.put(field, Ids.getIds(profile.getParents()));

            } else if (field.equalsIgnoreCase("class")
                    || field.equalsIgnoreCase("string")
                    || field.equalsIgnoreCase("abstractProfile")) {

                // ignore...

            } else {
                addProperty(profile, field, answer);
            }
        }

        return answer;
    }

    private static void addProperty(Object obj, String field, Map<String, Object> map) {
        try {
            Object prop = PropertyUtils.getProperty(obj, field);
            map.put(field, prop);
        } catch (Exception e) {
            throw new FabricException("Failed to initialize DTO", e);
        }
    }

    public static Map<String, Object> convertContainerToMap(FabricService service, Container container, List<String> fields) {
        Map<String, Object> answer = new HashMap<String, Object>();

        for(String field: fields) {

            if ( field.equalsIgnoreCase("profiles") ||
                 field.equalsIgnoreCase("profileIds")) {

                answer.put(field, Ids.getIds(container.getProfiles()));

            } else if (field.equalsIgnoreCase("childrenIds") ||
                       field.equalsIgnoreCase("children")) {

                answer.put(field, Ids.getIds(container.getChildren()));

            } else if (field.equalsIgnoreCase("parent") ||
                       field.equalsIgnoreCase("parentId")) {

                answer.put(field, Ids.getId(container.getParent()));

            } else if (field.equalsIgnoreCase("version") ||
                       field.equalsIgnoreCase("versionId")) {

                answer.put(field, Ids.getId(container.getVersion()));

            } else if (field.equalsIgnoreCase("overlayProfile")) {

                answer.put(field, convertProfileToMap(service, container.getOverlayProfile(), getFields(Profile.class)));

            } else {
                addProperty(container, field, answer);
            }

        }

        return answer;
    }

    public static Map<String, Object> convertVersionToMap(FabricService service, Version version, List<String> fields) {

        Map<String, Object> answer = new HashMap<String, Object>();

        for(String field: fields) {
            if ( field.equalsIgnoreCase("profiles") ||
                 field.equalsIgnoreCase("profileIds")) {

                answer.put(field, Ids.getIds(version.getProfiles()));

            } else if (field.equalsIgnoreCase("defaultVersion")) {

                answer.put(field, service.getDefaultVersion().equals(version));

            } else if (field.equalsIgnoreCase("class")
                    || field.equalsIgnoreCase("string")) {
                    //|| field.equalsIgnoreCase("sequence")) {

                // ignore...

            } else {
                addProperty(version, field, answer);
            }
        }

        return answer;
    }


    public static List<String> fetchChildIds(FabricService service, Profile self) {
        List<String> ids = new ArrayList<String>();
        for(Profile p : service.getVersion(self.getVersion()).getProfiles()) {
            for (Profile parent : p.getParents()) {
                if (parent.getId().equals(self.getId())) {
                    ids.add(p.getId());
                    break;
                }
            }
        }
        return ids;
    }


    public static List<String> fetchContainers(FabricService service, Profile self) {
        List<String> answer = new ArrayList<String>();
        for (Container c : self.getAssociatedContainers()) {
            answer.add(c.getId());
        }
        return answer;
    }


    public static int fetchContainerCount(Profile self) {
        return self.getAssociatedContainers().length;
    }


    public static List<String>fetchConfigurations(Profile self) {
        List<String> answer = new ArrayList<String>();
        answer.addAll(self.getFileConfigurations().keySet());
        return answer;
    }

    public static List<String> collapseToList(List<Map<String, Object>> objs, String field) {
        List<String> answer = new ArrayList<String>();
        for (Map<String, Object> o : objs) {
            answer.add(o.get(field).toString());
        }
        return answer;
    }





}
