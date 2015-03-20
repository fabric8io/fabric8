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
