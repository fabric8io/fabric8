package org.fusesource.fabric.agent.sort;


import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class RequirementSort  {

	/**
	 * Sorts {@link Resource} based on their {@link Requirement}s and {@link Capability}s.
	 * @param resources
	 * @return
	 */
	public static Collection<Resource> sort(Collection<Resource> resources) {
		Set<Resource> sorted = new LinkedHashSet<Resource>();
		Set<Resource> visited = new LinkedHashSet<Resource>();
		for (Resource r : resources) {
			visit(r, resources, visited, sorted);
		}
		return sorted;
	}


	private static void visit(Resource resource, Collection<Resource> resources, Set<Resource> visited, Set<Resource> sorted) {
		if (visited.contains(resource)) {
			return;
		}
		visited.add(resource);
		for (Resource r : collectDependencies(resource, resources)) {
			visit(r, resources, visited, sorted);
		}
		sorted.add(resource);
	}

	/**
	 * Finds the dependencies of the current resource.
	 * @param resource
	 * @param allResources
	 * @return
	 */
	private static Set<Resource> collectDependencies(Resource resource, Collection<Resource> allResources) {
		Set<Resource> result = new LinkedHashSet<Resource>();
		Requirement[] requirements = resource.getRequirements();
		for (Requirement requirement : requirements) {
			boolean isSatisfied = false;
			for (Resource r : result) {
				for (Capability capability : r.getCapabilities()) {
					if (requirement.isSatisfied(capability)) {
						isSatisfied = true;
						break;
					}
				}
			}

			for (Resource r : allResources) {
				if (!isSatisfied) {
					for (Capability capability : r.getCapabilities()) {
						if (requirement.isSatisfied(capability)) {
							result.add(r);
							break;
						}
					}
				}
			}
		}
		return result;
	}
}
