/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.service;

import static io.fabric8.api.MQService.Config.CONFIG_URL;
import static io.fabric8.api.MQService.Config.CONNECTORS;
import static io.fabric8.api.MQService.Config.GROUP;
import static io.fabric8.api.MQService.Config.STANDBY_POOL;
import io.fabric8.api.FabricService;
import io.fabric8.api.MQService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.Version;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

public class MQServiceImpl implements MQService {
    private static final transient Logger LOG = LoggerFactory.getLogger(MQServiceImpl.class);

    private final ProfileService profileService;
    private final ProfileRegistry profileRegistry;
    private final RuntimeProperties runtimeProperties;

    public MQServiceImpl(FabricService fabricService, RuntimeProperties runtimeProperties) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.profileRegistry = fabricService.adapt(ProfileRegistry.class);
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public Profile createOrUpdateMQProfile(String versionId, String profileId, String brokerName, Map<String, String> configs, boolean replicated) {
        Version version = profileService.getRequiredVersion(versionId);

        String parentProfileName = null;
        if (configs != null && configs.containsKey("parent")) {
            parentProfileName = configs.remove("parent");
        }
        if (Strings.isNullOrBlank(parentProfileName)) {
            parentProfileName = replicated ? MQ_PROFILE_REPLICATED : MQ_PROFILE_BASE;
        }
        
        Profile parentProfile = version.getRequiredProfile(parentProfileName);
        if (brokerName == null || profileId == null)
            return parentProfile;
        
        String pidName = getBrokerPID(brokerName);
        // lets check we have a config value

        ProfileBuilder builder;
        
        // create a profile if it doesn't exist
        Map<String, String> config = null;
        boolean create = !version.hasProfile(profileId);
        if (create) {
            builder = ProfileBuilder.Factory.create(versionId, profileId);
        } else {
            Profile profile = version.getRequiredProfile(profileId);
            builder = ProfileBuilder.Factory.createFrom(profile);
            config = new HashMap<>(builder.getConfiguration(pidName));
        }
        
        Map<String, String> parentProfileConfig = parentProfile.getConfiguration(MQ_PID_TEMPLATE);
        if (config == null) {
            config = new HashMap<>(parentProfileConfig);
        }

        if( "true".equals(configs.get("ssl")) ) {

            // Only generate the keystore file if it does not exist.
            // [TOOD] Fix direct data access! This should be part of the ProfileBuilder
            byte[] keystore  = builder.getFileConfiguration("keystore.jks");
            if( keystore==null ) {
                try {

                    String host = configs.get("keystore.cn");
                    if( host == null ) {
                        host = configs.get(GROUP);
                        if( host == null ) {
                            host = "localhost";
                        }
                        configs.put("keystore.cn", host);
                    }
                    String password = configs.get("keystore.password");
                    if( password == null ) {
                        password = generatePassword(8);
                        configs.put("keystore.password", password);
                    }

                    File keystoreFile = io.fabric8.utils.Files.createTempFile(runtimeProperties.getDataPath());
                    keystoreFile.delete();
                    LOG.info("Generating ssl keystore...");
                    int rc = system("keytool", "-genkey",
                            "-storetype", "JKS",
                            "-storepass", password,
                            "-keystore", keystoreFile.getCanonicalPath(),
                            "-keypass", password,
                            "-alias", host,
                            "-keyalg", "RSA",
                            "-keysize", "4096",
                            "-dname", String.format("cn=%s", host),
                            "-validity", "3650");

                    if(rc!=0) {
                      throw new IOException("keytool failed with exit code: "+rc);
                    }

                    keystore = Files.readBytes(keystoreFile);
                    keystoreFile.delete();
                    LOG.info("Keystore generated");

                    // [TOOD] Fix direct data access! This should be part of the ProfileBuilder
                    profileRegistry.setFileConfiguration(versionId, profileId, "keystore.jks", keystore);
                    configs.put("keystore.file", "profile:keystore.jks");

                } catch (IOException e) {
                    LOG.info("Failed to generate keystore.jks: "+e, e);
                }

            }

            // [TOOD] Fix direct data access! This should be part of the ProfileBuilder
            byte[] truststore = builder.getFileConfiguration("truststore.jks");
            if( truststore==null ) {

                try {

                    String password = configs.get("truststore.password");
                    if( password == null ) {
                        password = configs.get("keystore.password");
                        configs.put("truststore.password", password);
                    }

                    File keystoreFile = io.fabric8.utils.Files.createTempFile(runtimeProperties.getDataPath());
                    Files.writeToFile(keystoreFile, keystore);

                    File certFile = io.fabric8.utils.Files.createTempFile(runtimeProperties.getDataPath());
                    certFile.delete();

                    LOG.info("Exporting broker certificate to create truststore.jks");
                    int rc = system("keytool", "-exportcert", "-rfc",
                            "-keystore", keystoreFile.getCanonicalPath(),
                            "-storepass", configs.get("keystore.password"),
                            "-alias",  configs.get("keystore.cn"),
                            "--file", certFile.getCanonicalPath());

                    keystoreFile.delete();
                    if(rc!=0) {
                      throw new IOException("keytool failed with exit code: "+rc);
                    }

                    LOG.info("Creating truststore.jks");
                    File truststoreFile = io.fabric8.utils.Files.createTempFile(runtimeProperties.getDataPath());
                    truststoreFile.delete();
                    rc = system("keytool", "-importcert", "-noprompt",
                            "-keystore", truststoreFile.getCanonicalPath(),
                            "-storepass", password,
                            "--file", certFile.getCanonicalPath());
                    certFile.delete();
                    if(rc!=0) {
                      throw new IOException("keytool failed with exit code: "+rc);
                    }

                    truststore = Files.readBytes(truststoreFile);
                    truststoreFile.delete();
                    
                    // [TOOD] Fix direct data access! This should be part of the ProfileBuilder
                    profileRegistry.setFileConfiguration(versionId, profileId, "truststore.jks", truststore);
                    configs.put("truststore.file", "profile:truststore.jks");

                } catch (IOException e) {
                    LOG.info("Failed to generate truststore.jks: "+e, e);
                }

            }

        }

        config.put("broker-name", brokerName);
        if (configs != null) {
            config.putAll(configs);
        }

        // lets check we've a bunch of config values inherited from the template
        String[] propertiesToDefault = { CONFIG_URL, STANDBY_POOL, CONNECTORS };
        for (String key : propertiesToDefault) {
            if (config.get(key) == null) {
                String defaultValue = parentProfileConfig.get(key);
                if (Strings.isNotBlank(defaultValue)) {
                    config.put(key, defaultValue);
                }
            }
        }
        
        builder.addConfiguration(pidName, config);
        Profile profile = builder.getProfile();
        return create ? profileService.createProfile(profile) : profileService.updateProfile(profile);
    }

    static public String generatePassword(int len) {
        Random random = new Random(System.nanoTime());
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_-!.+^";
        StringBuilder rc = new StringBuilder(len);
        for( int i=0; i < len; i++) {
            rc.append(characters.charAt(random.nextInt(characters.length())));
        }
        return rc.toString();
    }

    private int system(final String...args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);

        // start the process
        final Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            LOG.debug("Process failed to start: "+e, e);
            return -1;
        }

        new Thread("system command output processor") {
            @Override
            public void run() {
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
                try {
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) break;
                        LOG.info(String.format("%s: %s", args[0], line));
                    }
                } catch (IOException e) {
                } finally {
                    Closeables.closeQuietly(reader);
                }
            }
        }.start();

        // wait for command to exit
        int exitCode = 0;
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            LOG.debug("Thread interrupted, killing process");
            process.destroy();
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public Profile createOrUpdateMQClientProfile(String versionId, String profileId, String group, String parentProfileName) {
        Version version = profileService.getRequiredVersion(versionId);

        Profile parentProfile = null;
        if (Strings.isNotBlank(parentProfileName)) {
            parentProfile = version.getRequiredProfile(parentProfileName);
        }
        if (group == null || profileId == null)
            return parentProfile;
        
        ProfileBuilder builder;
        
        // create a profile if it doesn't exist
        boolean create = !version.hasProfile(profileId);
        if (create) {
            builder = ProfileBuilder.Factory.create(versionId, profileId);
        } else {
            Profile profile = version.getRequiredProfile(profileId);
            builder = ProfileBuilder.Factory.createFrom(profile);
        }

        // set the parent if its specified
        if (parentProfile != null) {
            builder.addParent(parentProfile);
        }

        Map<String, String> config = builder.getConfiguration(MQ_CONNECTION_FACTORY_PID);
        config = config != null ? new HashMap<>(config) : new HashMap<String, String>();
        config.put(GROUP, group);
        builder.addConfiguration(MQ_CONNECTION_FACTORY_PID, config);
        
        Profile profile = builder.getProfile();
        return create ? profileService.createProfile(profile) : profileService.updateProfile(profile);
    }


    @Override
    public String getConfig(String version, String config) {
        return "profile:" + config;
    }

    protected String getBrokerPID(String brokerName) {
        return MQ_FABRIC_SERVER_PID_PREFIX + brokerName;
    }

}
