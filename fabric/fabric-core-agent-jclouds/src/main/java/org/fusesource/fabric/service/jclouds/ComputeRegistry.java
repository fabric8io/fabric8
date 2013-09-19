package org.fusesource.fabric.service.jclouds;

import org.jclouds.compute.ComputeService;

import java.util.List;

public interface ComputeRegistry {

    /**
     * Returns a {@link List} of all available {@link ComputeService} instances.
     * @return
     */
    public List<ComputeService> list();


    /**
     * Finds or waits for the {@link ComputeService} that matches the specified name.
     *
     * @param name
     * @return
     */
    public ComputeService getIfPresent(String name);


    /**
     * Finds or waits for the {@link ComputeService} that matches the specified name.
     *
     * @param name
     * @return
     */
    public ComputeService getOrWait(String name);


    /**
     * Removes the {@link ComputeService} that matches the specified name.
     *
     * @param name
     * @return
     */
    public void remove(String name);

}
