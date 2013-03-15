<%--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at
   
    http://www.apache.org/licenses/LICENSE-2.0
   
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
--%>
<table>
  <tr>
    <td>

      <h2>Welcome!</h2>

      <p>
        Welcome to the JBoss A-MQ Console of <b>${requestContext.brokerQuery.brokerName}</b> (${requestContext.brokerQuery.brokerAdmin.brokerId})
      </p>

      <p>
        You can find more information about JBoss A-MQ on the <a href="http://www.redhat.com/products/jbossenterprisemiddleware/amq/">JBoss A-MQ Site</a>
      </p>

      <table class="overviewSection">
        <thead>
        <tr>
          <th class="center" colspan="2">
            <h5>Broker : ${requestContext.brokerQuery.brokerAdmin.brokerName}</h5>
          </th>
        </tr>
        </thead>
        <tbody>
        <tr>
          <td>Version: </td>
          <td><b>${requestContext.brokerQuery.brokerAdmin.brokerVersion}</b></td>
        </tr>
        <tr>
          <td>ID: </td>
          <td><b>${requestContext.brokerQuery.brokerAdmin.brokerId}</b></td>
        </tr>
        <tr>
          <td>Uptime: </td>
          <td><b>${requestContext.brokerQuery.brokerAdmin.uptime}</b></td>
        </tr>
        <tr>
          <td>Store percent used: </td>
          <td><b>${requestContext.brokerQuery.brokerAdmin.storePercentUsage}</b></td>
        </tr>
        <tr>
          <td>Memory percent used: </td>
          <td><b>${requestContext.brokerQuery.brokerAdmin.memoryPercentUsage}</b></td>
        </tr>
        <tr>
          <td>Temp percent used: </td>
          <td><b>${requestContext.brokerQuery.brokerAdmin.tempPercentUsage}</b></td>
        </tr>

        </tbody>
      </table>

    </td>

      <td valign="top">

        <div class="navigation">
          <div class="navigation_top">
            <div class="navigation_bottom">
              <H3>Queue Views</H3>

              <ul class="alternate">


                <li><a href="queueGraph.jsp" title="View the queue depths as a graph">Graph</a></li>
                <li><a href="xml/queues.jsp" title="View the queues as XML">XML</a></li>
              </ul>
              <H3>Topic Views</H3>

              <ul class="alternate">


                <li><a href="xml/topics.jsp" title="View the topics as XML">XML</a></li>
              </ul>
              <H3>Subscribers Views</H3>

              <ul class="alternate">


                <li><a href="xml/subscribers.jsp" title="View the subscribers as XML">XML</a></li>
              </ul>
              <H3>Useful Links</H3>

              <ul class="alternate">
                <li><a href="https://access.redhat.com/knowledge/docs/JBoss_A-MQ/"
                       title="The most popular and powerful open source Message Broker">Documentation</a></li>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </td>
  </tr>
</table>
