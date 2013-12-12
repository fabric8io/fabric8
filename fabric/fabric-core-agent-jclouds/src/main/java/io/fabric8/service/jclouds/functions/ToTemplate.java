/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.service.jclouds.functions;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import io.fabric8.service.jclouds.CreateJCloudsContainerOptions;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;

import java.lang.reflect.Field;
import java.util.Map;

public class ToTemplate {

    public static Template apply(CreateJCloudsContainerOptions options) {
        ComputeService service = options.getComputeService();
        TemplateOptions templateOptions = service.templateOptions();
        TemplateBuilder builder = service.templateBuilder().any();
        applyInstanceType(builder, options);
        applyImageType(builder, options);
        applyLocation(builder, options);
        applyProviderSpecificOptions(templateOptions, options);

        Optional<AdminAccess> adminAccess = ToAdminAccess.apply(options);
        if (adminAccess.isPresent()) {
            templateOptions.runScript(adminAccess.get());
        }
        builder = builder.options(templateOptions);
        return builder.build();
    }


    /**
     * Applies node options to the template options. Currently only works for String key value pairs.
     *
     * @param templateOptions
     * @param options
     */
    private static void applyProviderSpecificOptions(TemplateOptions templateOptions, CreateJCloudsContainerOptions options) {
        if (options != null && templateOptions != null) {
            for (Map.Entry<String, String> entry : options.getNodeOptions().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                try {
                    Field field = templateOptions.getClass().getDeclaredField(key);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(templateOptions, value);
                    }
                } catch (Exception ex) {
                    //noop
                }

            }
        }
    }

    private static void applyInstanceType(TemplateBuilder builder, CreateJCloudsContainerOptions options) {
        //If no options about hardware has been specified ...
        if (options.getInstanceType() == null && Strings.isNullOrEmpty(options.getHardwareId())) {
            builder.minRam(1024);
        } else if (!Strings.isNullOrEmpty(options.getHardwareId())) {
            builder.hardwareId(options.getHardwareId());
        } else if (options.getInstanceType() != null) {
            switch (options.getInstanceType()) {
                case Smallest:
                    builder.smallest();
                    break;
                case Biggest:
                    builder.biggest();
                    break;
                case Fastest:
                    builder.fastest();
                    break;
                default:
                    builder.fastest();
            }
        }
    }

    private static void applyImageType(TemplateBuilder builder, CreateJCloudsContainerOptions options) {
        //Define ImageId
        if (!Strings.isNullOrEmpty(options.getImageId())) {
            builder.imageId(options.getImageId());
        }
        //or define Image by OS & Version or By ImageId
        else if (!Strings.isNullOrEmpty(options.getOsFamily())) {
            builder.osFamily(OsFamily.fromValue(options.getOsFamily()));
            if (!Strings.isNullOrEmpty(options.getOsVersion())) {
                builder.osVersionMatches(options.getOsVersion());
            }
        } else {
            throw new IllegalArgumentException("Required Image id or Operation System and version predicates.");
        }
    }

    private static void applyLocation(TemplateBuilder builder, CreateJCloudsContainerOptions options) {
        //Define Location & Hardware
        if (!Strings.isNullOrEmpty(options.getLocationId())) {
            builder.locationId(options.getLocationId());
        }
    }
}
