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
package io.fabric8.bridge.model;

import java.util.HashSet;
import java.util.Set;

import javax.jms.Session;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import io.fabric8.bridge.MessageConverter;
import io.fabric8.bridge.internal.BatchMessageListenerContainer;
import io.fabric8.bridge.internal.ConnectionFactoryAdapter;
import io.fabric8.bridge.internal.MessageConverterAdapter;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * @author Dhiraj Bokde
 *
 */
@XmlRootElement(name="dispatch-policy")
@XmlAccessorType(XmlAccessType.FIELD)
public class DispatchPolicy extends IdentifiedType {

	// listener properties
	@XmlAttribute
	private int cacheLevel = DefaultMessageListenerContainer.CACHE_AUTO;

	@XmlAttribute
	private int concurrentConsumers = 1;

	@XmlAttribute
	private int maxConcurrentConsumers = 1;
	
	@XmlAttribute
	private long batchSize = BatchMessageListenerContainer.DEFAULT_BATCH_SIZE;
	
	@XmlAttribute
	private long batchTimeout = BatchMessageListenerContainer.DEFAULT_BATCH_TIMEOUT;
	
	@XmlAttribute
	private int localAcknowledgeMode = Session.SESSION_TRANSACTED;
	
	@XmlAttribute
	private boolean localSessionTransacted = true;
	
	@XmlAttribute
	private String messageSelector;

	// sender properties
	@XmlAttribute(required=false)
	private int remoteAcknowledgeMode = Session.SESSION_TRANSACTED;

	@XmlAttribute(required=false)
	private boolean remoteSessionTransacted = true;
	
	// use the bean name to lookup in an ApplicationContext
	// represents the exported message converter for this bridge destination
    // generateSpringSchema.xslt removes it in the generated schema
	@XmlElement(name="exportedMessageConverter")
	@XmlJavaTypeAdapter(MessageConverterAdapter.class)
	@XmlMimeType("application/octet-stream")
	private MessageConverter messageConverter;

	// placeholder for bean name, used to generate fabric-bridge.xsd
	@XmlAttribute
	private String messageConverterRef;

    // mechanism for determining which properties have been set
    // NOTE this has to be marshaled in JAXB, but not used in Spring,
    // generateSpringSchema.xslt removes it in the generated schema
    @XmlElement(name = "propertySet", required = true)
    private Set<String> propertiesSet = new HashSet<String>();

	public final Set<String> getPropertiesSet() {
		return propertiesSet;
	}

	public final int getCacheLevel() {
		return cacheLevel;
	}

	public final void setCacheLevel(int cacheLevel) {
		this.cacheLevel = cacheLevel;
		propertiesSet.add("cacheLevel");
	}

	public final int getConcurrentConsumers() {
		return concurrentConsumers;
	}

	public final void setConcurrentConsumers(int concurrentConsumers) {
		this.concurrentConsumers = concurrentConsumers;
		propertiesSet.add("concurrentConsumers");
	}

	public final int getMaxConcurrentConsumers() {
		return maxConcurrentConsumers;
	}

	public final void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		this.maxConcurrentConsumers = maxConcurrentConsumers;
		propertiesSet.add("maxConcurrentConsumers");
	}

	public final long getBatchSize() {
		return batchSize;
	}

	/**
	 * Set to <=0 to disable batch delivery
	 * 
	 * @param batchSize
	 */
	public final void setBatchSize(long batchSize) {
		this.batchSize = batchSize;
		propertiesSet.add("batchSize");
	}

	public final long getBatchTimeout() {
		return batchTimeout;
	}

	/**
	 * Set to <=0 to disable batch delivery
	 * 
	 * @param batchTimeout
	 */
	public final void setBatchTimeout(long batchTimeout) {
		this.batchTimeout = batchTimeout;
		propertiesSet.add("batchTimeout");
	}

	public final int getLocalAcknowledgeMode() {
		return localAcknowledgeMode;
	}

	public final void setLocalAcknowledgeMode(int localAcknowledgeMode) {
		this.localAcknowledgeMode = localAcknowledgeMode;
		propertiesSet.add("localAcknowledgeMode");
	}

	public final boolean isLocalSessionTransacted() {
		return localSessionTransacted;
	}

	public final void setLocalSessionTransacted(boolean localSessionTransacted) {
		this.localSessionTransacted = localSessionTransacted;
		propertiesSet.add("localSessionTransacted");
	}

	public final String getMessageSelector() {
		return messageSelector;
	}

	public final void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
		propertiesSet.add("messageSelector");
	}

	public final void setRemoteAcknowledgeMode(int remoteAcknowledgeMode) {
		this.remoteAcknowledgeMode = remoteAcknowledgeMode;
		propertiesSet.add("remoteAcknowledgeMode");
	}

	public final int getRemoteAcknowledgeMode() {
		return remoteAcknowledgeMode;
	}

	public final boolean isRemoteSessionTransacted() {
		return remoteSessionTransacted;
	}

	public final void setRemoteSessionTransacted(boolean remoteSessionTransacted) {
		this.remoteSessionTransacted = remoteSessionTransacted;
		propertiesSet.add("remoteSessionTransacted");
	}

	public final MessageConverter getMessageConverter() {
		return messageConverter;
	}
	
	public final void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
		propertiesSet.add("messageConverter");
	}

	public String getMessageConverterRef() {
		return messageConverterRef;
	}

	public void setMessageConverterRef(String messageConverterRef) {
		this.messageConverterRef = messageConverterRef;
		propertiesSet.add("messageConverterRef");
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this,ToStringStyle.SHORT_PREFIX_STYLE);
	}

	@Override
	public int hashCode() {
		int val = 0;
		val += cacheLevel;
		val += concurrentConsumers;
		val += maxConcurrentConsumers;
		val += batchSize;
		val += batchTimeout;
		val += localAcknowledgeMode;
		val += (localSessionTransacted ? 1 : 0);
		val += (messageSelector != null ? messageSelector.hashCode() : 0);
		val += remoteAcknowledgeMode;
		val += (remoteSessionTransacted ? 1 : 0);
		return val;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (obj instanceof DispatchPolicy) {
			DispatchPolicy policy = (DispatchPolicy) obj;
			return this.cacheLevel == policy.cacheLevel
					&& this.concurrentConsumers == policy.concurrentConsumers
					&& this.maxConcurrentConsumers == policy.maxConcurrentConsumers
					&& this.batchSize == policy.batchSize
					&& this.batchTimeout == policy.batchTimeout
					&& this.localAcknowledgeMode == policy.localAcknowledgeMode
					&& this.localSessionTransacted == policy.localSessionTransacted
					&& (this.messageSelector != null ? this.messageSelector.equals(policy.messageSelector)
							: policy.messageSelector == null)
					&& this.remoteAcknowledgeMode == policy.remoteAcknowledgeMode
					&& this.remoteSessionTransacted == policy.remoteSessionTransacted
					&& (this.messageConverter != null ? this.messageConverter.equals(policy.messageConverter)
							: policy.messageConverter == null);

		}
		return false;
	}

}
