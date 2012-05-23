package com.fusesource.bai.backend;

import com.fusesource.bai.event.AuditEvent;

/**
 * Business Activity Insight backend interface
 * @author Raul Kripalani
 *
 */
public interface BAIAuditBackend  {
   
	public void audit(AuditEvent event);
	
}
