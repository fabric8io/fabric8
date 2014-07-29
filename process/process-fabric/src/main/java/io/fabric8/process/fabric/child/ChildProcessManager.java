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
package io.fabric8.process.fabric.child;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.common.util.Objects;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessController;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.process.manager.support.ApplyConfigurationTask;
import io.fabric8.process.manager.support.CompositeTask;
import io.fabric8.process.manager.support.InstallDeploymentsTask;
import io.fabric8.process.manager.support.ProcessUtils;
import io.fabric8.service.child.ChildConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ChildProcessManager {
    private static final transient Logger LOG = LoggerFactory.getLogger(ChildProcessManager.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private FabricService fabricService;
    private ProcessManager processManager;

    public void destroy() {
        executorService.shutdown();
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    Installation provisionProcess(ProcessRequirements requirements) throws Exception {
        // TODO check that the installation is the same
        uninstallProcess(requirements);

        //String id = requirements.getId();
        InstallOptions installOptions = requirements.createInstallOptions();
        Profile processProfile = getProcessProfile(requirements, true);
        Profile deployProcessProfile = getProcessProfile(requirements, false);
        Map<String, String> configuration = ProcessUtils.getProcessLayout(fabricService, processProfile, requirements.getLayout());

        //DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabricService, executorService);
        InstallTask applyConfiguration = new ApplyConfigurationTask(configuration, installOptions.getProperties());
        List<Profile> profiles = new ArrayList<Profile>();
        profiles.add(deployProcessProfile);
        Map<String, File> javaArtifacts = JavaContainers.getJavaContainerArtifactsFiles(fabricService, profiles, executorService);
        String versionId = Profiles.versionId(fabricService.getCurrentContainer().getVersion());
        List<String> profileIds = Profiles.profileIds(profiles);
        Map<String, String> contextPathConfiguration = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.WEB_CONTEXT_PATHS_PID);
        InstallTask applyProfile = new InstallDeploymentsTask(javaArtifacts, contextPathConfiguration);
        InstallTask compositeTask = new CompositeTask(applyConfiguration, applyProfile);
        Installation installation = processManager.install(installOptions, compositeTask);
        if (installation != null) {
            installation.getController().start();
        }
        return installation;
    }


    void uninstallProcess(ProcessRequirements requirements) throws Exception {
        String id = requirements.getId();

        Installation installation = findProcessInstallation(id);
        // for now lets remove it just in case! :)
        if (installation != null) {
            ProcessController controller = installation.getController();
            try {
                controller.stop();
            } catch (Exception e) {
                LOG.warn("Ignored exception while trying to stop process " + installation + " " + e);
            }
            controller.uninstall();
            controller = null;
        }
    }

    protected Profile getProcessProfile(ProcessRequirements requirements, boolean includeController) {
        Container container = fabricService.getCurrentContainer();
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        Profile processProfile = getProcessProfile(requirements, includeController, container);
        return Profiles.getEffectiveProfile(fabricService, profileService.getOverlayProfile(processProfile));
    }

    protected Installation findProcessInstallation(String id) {
        List<Installation> installations = processManager.listInstallations();
        for (Installation installation : installations) {
            String name = installation.getName();
            if (Objects.equal(id, name)) {
                return installation;
            }
        }
        return null;
    }

	private Profile getProcessProfile(ProcessRequirements requirements, boolean includeController, Container container) {
		String versionId = container.getVersion().getId();
		String profileId = "process-profile-" + requirements.getId();
		ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId);
		ProcessProfileOptions optionsProvider = new ProcessProfileOptions(container, requirements, includeController);
		return builder.addOptions(optionsProvider).getProfile();
	}

	static class ProcessProfileOptions implements OptionsProvider<ProfileBuilder> {

	    private final Container container;
	    private final ProcessRequirements requirements;
	    private final boolean includeContainerProfile;

	    ProcessProfileOptions(Container container, ProcessRequirements requirements, boolean includeContainerProfile) {
	        this.container = container;
	        this.requirements = requirements;
	        this.includeContainerProfile = includeContainerProfile;
	    }

	    @Override
		public ProfileBuilder addOptions(ProfileBuilder builder) {
	    	builder.addAttribute(Profile.ABSTRACT, Boolean.TRUE.toString());
	    	builder.addAttribute(Profile.LOCKED, Boolean.TRUE.toString());
	    	builder.addAttribute(Profile.HIDDEN, Boolean.TRUE.toString());
	    	builder.addParents(getParents());
			return builder;
		}

	    private List<Profile> getParents() {
	        List<String> parents = requirements.getProfiles();
	        List<Profile> profiles = new LinkedList<Profile>();
	        if (includeContainerProfile) {
	            profiles.add(container.getOverlayProfile());
	        }
	        for (String parent : parents) {
	            Profile p = container.getVersion().getRequiredProfile(parent);
	            profiles.add(p);
	        }
	        return profiles;
	    }
	}
}
