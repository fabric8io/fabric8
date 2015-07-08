/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.rest.producer;

import org.jboss.forge.addon.ui.command.CommandFactory;
import org.jboss.forge.addon.ui.controller.CommandControllerFactory;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.se.FurnaceFactory;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@ApplicationScoped
public class FurnaceProducer {

	private Furnace furnace;

	private CommandFactory commandFactory;

	private CommandControllerFactory controllerFactory;

	public void setup(File repoDir) {
		furnace = FurnaceFactory.getInstance(Thread.currentThread()
				.getContextClassLoader(), Thread.currentThread()
				.getContextClassLoader());
		furnace.addRepository(AddonRepositoryMode.IMMUTABLE, repoDir);
		Future<Furnace> future = furnace.startAsync();

		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Furnace failed to start.", e);
		}

		AddonRegistry addonRegistry = furnace.getAddonRegistry();
		commandFactory = addonRegistry.getServices(CommandFactory.class).get();
		controllerFactory = (CommandControllerFactory) addonRegistry
				.getServices(CommandControllerFactory.class.getName()).get();
	}

	@Produces
	public Furnace getFurnace() {
		return furnace;
	}

	@Produces
	public CommandFactory getCommandFactory() {
		return commandFactory;
	}

	@Produces
	public CommandControllerFactory getControllerFactory() {
		return controllerFactory;
	}

	@PreDestroy
	public void destroy() {
		furnace.stop();
	}
}
