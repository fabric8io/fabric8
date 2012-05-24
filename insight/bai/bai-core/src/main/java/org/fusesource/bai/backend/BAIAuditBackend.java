package org.fusesource.bai.backend;

import org.fusesource.bai.event.AuditEvent;

/**
 * Business Activity Insight backend interface
 * @author Raul Kripalani
 *
 */
public interface BAIAuditBackend  {
   
	public void audit(AuditEvent event);
	
}
