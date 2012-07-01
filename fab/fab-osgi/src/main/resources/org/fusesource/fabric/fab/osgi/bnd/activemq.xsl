<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright (C) FuseSource, Inc.
  http://fusesource.com

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:beans="http://www.springframework.org/schema/beans" xmlns:aop="http://www.springframework.org/schema/aop" xmlns:context="http://www.springframework.org/schema/context" xmlns:jee="http://www.springframework.org/schema/jee" xmlns:jms="http://www.springframework.org/schema/jms" xmlns:lang="http://www.springframework.org/schema/lang" xmlns:osgi-compendium="http://www.springframework.org/schema/osgi-compendium" xmlns:osgi="http://www.springframework.org/schema/osgi" xmlns:tool="http://www.springframework.org/schema/tool" xmlns:tx="http://www.springframework.org/schema/tx" xmlns:util="http://www.springframework.org/schema/util" xmlns:webflow-config="http://www.springframework.org/schema/webflow-config"
                xmlns:amq="http://activemq.apache.org/schema/core">

    <xsl:output method="text" />

    <xsl:template match="amq:*">
      org.apache.xbean.spring.context.v2.XBeanNamespaceHandler
      <xsl:call-template name="addImports">
        <xsl:with-param name="elementName" select="local-name()"/>
      </xsl:call-template>
      <xsl:apply-templates/>
    </xsl:template>

  <!-- TODO: figure this out from the XBean descriptor instead of hard-coding the list -->
  <xsl:template name="addImports">
    <xsl:param name="elementName" />
    <xsl:choose>
      <xsl:when test="$elementName = 'abortSlowConsumerStrategy'">
        org.apache.activemq.broker.region.policy.AbortSlowConsumerStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'amqPersistenceAdapter'">
        org.apache.activemq.store.amq.AMQPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'amqPersistenceAdapterFactory'">
        org.apache.activemq.store.amq.AMQPersistenceAdapterFactory
      </xsl:when>
      <xsl:when test="$elementName = 'authenticationUser'">
        org.apache.activemq.security.AuthenticationUser
      </xsl:when>
      <xsl:when test="$elementName = 'authorizationEntry'">
        org.apache.activemq.security.AuthorizationEntry
      </xsl:when>
      <xsl:when test="$elementName = 'authorizationMap'">
        org.apache.activemq.security.DefaultAuthorizationMap
      </xsl:when>
      <xsl:when test="$elementName = 'authorizationPlugin'">
        org.apache.activemq.security.AuthorizationPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'axionJDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.AxionJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'blobJDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.BlobJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'broker'">
        org.apache.activemq.xbean.XBeanBrokerService
      </xsl:when>
      <xsl:when test="$elementName = 'brokerService'">
        org.apache.activemq.broker.BrokerService
      </xsl:when>
      <xsl:when test="$elementName = 'bytesJDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.BytesJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'cachedLDAPAuthorizationMap'">
        org.apache.activemq.security.CachedLDAPAuthorizationMap
      </xsl:when>
      <xsl:when test="$elementName = 'commandAgent'">
        org.apache.activemq.broker.util.CommandAgent
      </xsl:when>
      <xsl:when test="$elementName = 'compositeDemandForwardingBridge'">
        org.apache.activemq.network.CompositeDemandForwardingBridge
      </xsl:when>
      <xsl:when test="$elementName = 'compositeQueue'">
        org.apache.activemq.broker.region.virtual.CompositeQueue
      </xsl:when>
      <xsl:when test="$elementName = 'compositeTopic'">
        org.apache.activemq.broker.region.virtual.CompositeTopic
      </xsl:when>
      <xsl:when test="$elementName = 'conditionalNetworkBridgeFilterFactory'">
        org.apache.activemq.network.ConditionalNetworkBridgeFilterFactory
      </xsl:when>
      <xsl:when test="$elementName = 'connectionDotFilePlugin'">
        org.apache.activemq.broker.view.ConnectionDotFilePlugin
      </xsl:when>
      <xsl:when test="$elementName = 'connectionFactory'">
        org.apache.activemq.spring.ActiveMQConnectionFactory
      </xsl:when>
      <xsl:when test="$elementName = 'constantPendingMessageLimitStrategy'">
        org.apache.activemq.broker.region.policy.ConstantPendingMessageLimitStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'locker'">
        org.apache.activemq.store.jdbc.DefaultDatabaseLocker
      </xsl:when>
      <xsl:when test="$elementName = 'JDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.DB
      </xsl:when>
      <xsl:when test="$elementName = 'defaultIOExceptionHandler'">
        org.apache.activemq.util.DefaultIOExceptionHandler
      </xsl:when>
      <xsl:when test="$elementName = 'defaultJDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.DefaultJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'defaultNetworkBridgeFilterFactory'">
        org.apache.activemq.network.DefaultNetworkBridgeFilterFactory
      </xsl:when>
      <xsl:when test="$elementName = 'defaultUsageCapacity'">
        org.apache.activemq.usage.DefaultUsageCapacity
      </xsl:when>
      <xsl:when test="$elementName = 'demandForwardingBridge'">
        org.apache.activemq.network.DemandForwardingBridge
      </xsl:when>
      <xsl:when test="$elementName = 'destinationDotFilePlugin'">
        org.apache.activemq.broker.view.DestinationDotFilePlugin
      </xsl:when>
      <xsl:when test="$elementName = 'destinationEntry'">
        org.apache.activemq.filter.DefaultDestinationMapEntry
      </xsl:when>
      <xsl:when test="$elementName = 'destinationPathSeparatorPlugin'">
        org.apache.activemq.broker.util.DestinationPathSeparatorBroker
      </xsl:when>
      <xsl:when test="$elementName = 'discardingDLQBrokerPlugin'">
        org.apache.activemq.plugin.DiscardingDLQBrokerPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'fileCursor'">
        org.apache.activemq.broker.region.policy.FilePendingSubscriberMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'fileDurableSubscriberCursor'">
        org.apache.activemq.broker.region.policy.FilePendingDurableSubscriberMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'fileQueueCursor'">
        org.apache.activemq.broker.region.policy.FilePendingQueueMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'filteredDestination'">
        org.apache.activemq.broker.region.virtual.FilteredDestination
      </xsl:when>
      <xsl:when test="$elementName = 'filteredKahaDB'">
        org.apache.activemq.store.kahadb.FilteredKahaDBPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'fixedCountSubscriptionRecoveryPolicy'">
        org.apache.activemq.broker.region.policy.FixedCountSubscriptionRecoveryPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'fixedSizedSubscriptionRecoveryPolicy'">
        org.apache.activemq.broker.region.policy.FixedSizedSubscriptionRecoveryPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'forcePersistencyModeBroker'">
        org.apache.activemq.plugin.ForcePersistencyModeBroker
      </xsl:when>
      <xsl:when test="$elementName = 'forcePersistencyModeBrokerPlugin'">
        org.apache.activemq.plugin.ForcePersistencyModeBrokerPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'forwardingBridge'">
        org.apache.activemq.network.ForwardingBridge
      </xsl:when>
      <xsl:when test="$elementName = 'adapter'">
        org.apache.activemq.store.jdbc.adapter.HsqldbJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'imageBasedJDBCAdaptor'">
        org.apache.activemq.store.jdbc.adapter.ImageBasedJDBCAdaptor
      </xsl:when>
      <xsl:when test="$elementName = 'inboundQueueBridge'">
        org.apache.activemq.network.jms.InboundQueueBridge
      </xsl:when>
      <xsl:when test="$elementName = 'inboundTopicBridge'">
        org.apache.activemq.network.jms.InboundTopicBridge
      </xsl:when>
      <xsl:when test="$elementName = 'individualDeadLetterStrategy'">
        org.apache.activemq.broker.region.policy.IndividualDeadLetterStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'informixJDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.InformixJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'jaasAuthenticationPlugin'">
        org.apache.activemq.security.JaasAuthenticationPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'jaasCertificateAuthenticationPlugin'">
        org.apache.activemq.security.JaasCertificateAuthenticationPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'jaasDualAuthenticationPlugin'">
        org.apache.activemq.security.JaasDualAuthenticationPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'jdbcPersistenceAdapter'">
        org.apache.activemq.store.jdbc.JDBCPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'jmsQueueConnector'">
        org.apache.activemq.network.jms.JmsQueueConnector
      </xsl:when>
      <xsl:when test="$elementName = 'jmsTopicConnector'">
        org.apache.activemq.network.jms.JmsTopicConnector
      </xsl:when>
      <xsl:when test="$elementName = 'journalPersistenceAdapter'">
        org.apache.activemq.store.journal.JournalPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'journalPersistenceAdapterFactory'">
        org.apache.activemq.store.journal.JournalPersistenceAdapterFactory
      </xsl:when>
      <xsl:when test="$elementName = 'journaledJDBC'">
        org.apache.activemq.store.PersistenceAdapterFactoryBean
      </xsl:when>
      <xsl:when test="$elementName = 'kahaDB'">
        org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'kahaPersistenceAdapter'">
        org.apache.activemq.store.kahadaptor.KahaPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'lDAPAuthorizationMap'">
        org.apache.activemq.security.LDAPAuthorizationMap
      </xsl:when>
      <xsl:when test="$elementName = 'lastImageSubscriptionRecoveryPolicy'">
        org.apache.activemq.broker.region.policy.LastImageSubscriptionRecoveryPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'ldapNetworkConnector'">
        org.apache.activemq.network.LdapNetworkConnector
      </xsl:when>
      <xsl:when test="$elementName = 'loggingBrokerPlugin'">
        org.apache.activemq.broker.util.LoggingBrokerPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'mKahaDB'">
        org.apache.activemq.store.kahadb.MultiKahaDBPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'managementContext'">
        org.apache.activemq.broker.jmx.ManagementContext
      </xsl:when>
      <xsl:when test="$elementName = 'masterConnector'">
        org.apache.activemq.broker.ft.MasterConnector
      </xsl:when>
      <xsl:when test="$elementName = 'adapter'">
        org.apache.activemq.store.jdbc.adapter.MaxDBJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'memoryPersistenceAdapter'">
        org.apache.activemq.store.memory.MemoryPersistenceAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'memoryUsage'">
        org.apache.activemq.usage.MemoryUsage
      </xsl:when>
      <xsl:when test="$elementName = 'messageGroupHashBucketFactory'">
        org.apache.activemq.broker.region.group.MessageGroupHashBucketFactory
      </xsl:when>
      <xsl:when test="$elementName = 'mirroredQueue'">
        org.apache.activemq.broker.region.virtual.MirroredQueue
      </xsl:when>
      <xsl:when test="$elementName = 'multicastNetworkConnector'">
        org.apache.activemq.network.MulticastNetworkConnector
      </xsl:when>
      <xsl:when test="$elementName = 'multicastTraceBrokerPlugin'">
        org.apache.activemq.broker.util.MulticastTraceBrokerPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'adapter'">
        org.apache.activemq.store.jdbc.adapter.MySqlJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'networkConnector'">
        org.apache.activemq.network.DiscoveryNetworkConnector
      </xsl:when>
      <xsl:when test="$elementName = 'noSubscriptionRecoveryPolicy'">
        org.apache.activemq.broker.region.policy.NoSubscriptionRecoveryPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'oldestMessageEvictionStrategy'">
        org.apache.activemq.broker.region.policy.OldestMessageEvictionStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'oldestMessageWithLowestPriorityEvictionStrategy'">
        org.apache.activemq.broker.region.policy.OldestMessageWithLowestPriorityEvictionStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'oracleJDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.OracleJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'outboundQueueBridge'">
        org.apache.activemq.network.jms.OutboundQueueBridge
      </xsl:when>
      <xsl:when test="$elementName = 'outboundTopicBridge'">
        org.apache.activemq.network.jms.OutboundTopicBridge
      </xsl:when>
      <xsl:when test="$elementName = 'pListStore'">
        org.apache.activemq.store.kahadb.plist.PListStore
      </xsl:when>
      <xsl:when test="$elementName = 'policyEntry'">
        org.apache.activemq.broker.region.policy.PolicyEntry
      </xsl:when>
      <xsl:when test="$elementName = 'policyMap'">
        org.apache.activemq.broker.region.policy.PolicyMap
      </xsl:when>
      <xsl:when test="$elementName = 'adapter'">
        org.apache.activemq.store.jdbc.adapter.PostgresqlJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'prefetchPolicy'">
        org.apache.activemq.ActiveMQPrefetchPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'prefetchRatePendingMessageLimitStrategy'">
        org.apache.activemq.broker.region.policy.PrefetchRatePendingMessageLimitStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'priorityNetworkDispatchPolicy'">
        org.apache.activemq.broker.region.policy.PriorityNetworkDispatchPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'proxyConnector'">
        org.apache.activemq.proxy.ProxyConnector
      </xsl:when>
      <xsl:when test="$elementName = 'queryBasedSubscriptionRecoveryPolicy'">
        org.apache.activemq.broker.region.policy.QueryBasedSubscriptionRecoveryPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'queue'">
        org.apache.activemq.command.ActiveMQQueue
      </xsl:when>
      <xsl:when test="$elementName = 'queueDispatchSelector'">
        org.apache.activemq.broker.region.QueueDispatchSelector
      </xsl:when>
      <xsl:when test="$elementName = 'redeliveryPolicy'">
        org.apache.activemq.RedeliveryPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'roundRobinDispatchPolicy'">
        org.apache.activemq.broker.region.policy.RoundRobinDispatchPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'sharedDeadLetterStrategy'">
        org.apache.activemq.broker.region.policy.SharedDeadLetterStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'simpleAuthenticationPlugin'">
        org.apache.activemq.security.SimpleAuthenticationPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'simpleAuthorizationMap'">
        org.apache.activemq.security.SimpleAuthorizationMap
      </xsl:when>
      <xsl:when test="$elementName = 'simpleDispatchPolicy'">
        org.apache.activemq.broker.region.policy.SimpleDispatchPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'simpleDispatchSelector'">
        org.apache.activemq.broker.region.policy.SimpleDispatchSelector
      </xsl:when>
      <xsl:when test="$elementName = 'simpleJmsMessageConvertor'">
        org.apache.activemq.network.jms.SimpleJmsMessageConvertor
      </xsl:when>
      <xsl:when test="$elementName = 'simpleMessageGroupMapFactory'">
        org.apache.activemq.broker.region.group.SimpleMessageGroupMapFactory
      </xsl:when>
      <xsl:when test="$elementName = 'sslContext'">
        org.apache.activemq.spring.SpringSslContext
      </xsl:when>
      <xsl:when test="$elementName = 'statements'">
        org.apache.activemq.store.jdbc.Statements
      </xsl:when>
      <xsl:when test="$elementName = 'statisticsBrokerPlugin'">
        org.apache.activemq.plugin.StatisticsBrokerPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'storeCursor'">
        org.apache.activemq.broker.region.policy.StorePendingQueueMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'storeDurableSubscriberCursor'">
        org.apache.activemq.broker.region.policy.StorePendingDurableSubscriberMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'storeUsage'">
        org.apache.activemq.usage.StoreUsage
      </xsl:when>
      <xsl:when test="$elementName = 'streamJDBCAdapter'">
        org.apache.activemq.store.jdbc.adapter.StreamJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'strictOrderDispatchPolicy'">
        org.apache.activemq.broker.region.policy.StrictOrderDispatchPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'adapter'">
        org.apache.activemq.store.jdbc.adapter.SybaseJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'systemUsage'">
        org.apache.activemq.usage.SystemUsage
      </xsl:when>
      <xsl:when test="$elementName = 'taskRunnerFactory'">
        org.apache.activemq.thread.TaskRunnerFactory
      </xsl:when>
      <xsl:when test="$elementName = 'tempDestinationAuthorizationEntry'">
        org.apache.activemq.security.TempDestinationAuthorizationEntry
      </xsl:when>
      <xsl:when test="$elementName = 'tempQueue'">
        org.apache.activemq.command.ActiveMQTempQueue
      </xsl:when>
      <xsl:when test="$elementName = 'tempTopic'">
        org.apache.activemq.command.ActiveMQTempTopic
      </xsl:when>
      <xsl:when test="$elementName = 'tempUsage'">
        org.apache.activemq.usage.TempUsage
      </xsl:when>
      <xsl:when test="$elementName = 'timeStampingBrokerPlugin'">
        org.apache.activemq.broker.util.TimeStampingBrokerPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'timedSubscriptionRecoveryPolicy'">
        org.apache.activemq.broker.region.policy.TimedSubscriptionRecoveryPolicy
      </xsl:when>
      <xsl:when test="$elementName = 'topic'">
        org.apache.activemq.command.ActiveMQTopic
      </xsl:when>
      <xsl:when test="$elementName = 'traceBrokerPathPlugin'">
        org.apache.activemq.broker.util.TraceBrokerPathPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'locker'">
        org.apache.activemq.store.jdbc.adapter.TransactDatabaseLocker
      </xsl:when>
      <xsl:when test="$elementName = 'adapter'">
        org.apache.activemq.store.jdbc.adapter.TransactJDBCAdapter
      </xsl:when>
      <xsl:when test="$elementName = 'transportConnector'">
        org.apache.activemq.broker.TransportConnector
      </xsl:when>
      <xsl:when test="$elementName = 'udpTraceBrokerPlugin'">
        org.apache.activemq.broker.util.UDPTraceBrokerPlugin
      </xsl:when>
      <xsl:when test="$elementName = 'uniquePropertyMessageEvictionStrategy'">
        org.apache.activemq.broker.region.policy.UniquePropertyMessageEvictionStrategy
      </xsl:when>
      <xsl:when test="$elementName = 'usageCapacity'">
        org.apache.activemq.usage.UsageCapacity
      </xsl:when>
      <xsl:when test="$elementName = 'virtualDestinationInterceptor'">
        org.apache.activemq.broker.region.virtual.VirtualDestinationInterceptor
      </xsl:when>
      <xsl:when test="$elementName = 'virtualTopic'">
        org.apache.activemq.broker.region.virtual.VirtualTopic
      </xsl:when>
      <xsl:when test="$elementName = 'vmCursor'">
        org.apache.activemq.broker.region.policy.VMPendingSubscriberMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'vmDurableCursor'">
        org.apache.activemq.broker.region.policy.VMPendingDurableSubscriberMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'vmQueueCursor'">
        org.apache.activemq.broker.region.policy.VMPendingQueueMessageStoragePolicy
      </xsl:when>
      <xsl:when test="$elementName = 'xaConnectionFactory'">
        org.apache.activemq.spring.ActiveMQXAConnectionFactory
      </xsl:when>
    </xsl:choose>
  </xsl:template>

    <!-- stripping text nodes -->
    <xsl:template match="text()" />

</xsl:stylesheet>