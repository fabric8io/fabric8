package io.fabric8.service.jclouds;

import org.jclouds.compute.ComputeService;

import java.util.List;

public interface ComputeRegistry {

    /**
     * Returns a {@link List} of all available {@link ComputeService} instances.
     */
    public List<ComputeService> list();


    /**
     * Finds or waits for the {@link ComputeService} that matches the specified name.
     */
    public ComputeService getIfPresent(String name);


    /**
     * Finds or waits for the {@link ComputeService} that matches the specified name.
     */
    public ComputeService getOrWait(String name);


    /**
     * Removes the {@link ComputeService} that matches the specified name.
     */
    public void remove(String name);

}
