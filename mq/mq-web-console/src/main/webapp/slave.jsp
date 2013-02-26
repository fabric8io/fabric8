<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <title>${requestContext.brokerQuery.brokerAdmin.brokerName} : JBoss A-MQ Console/></title>
    <style type="text/css" media="screen">
        @import url('${pageContext.request.contextPath}/styles/sorttable.css');
        @import url('${pageContext.request.contextPath}/styles/type-settings.css');
        @import url('${pageContext.request.contextPath}/styles/site.css');
        @import url('${pageContext.request.contextPath}/styles/prettify.css');
        @import url('${pageContext.request.contextPath}/styles/fuse.css');
    </style>
    <c:if test="${!disableJavaScript}">
        <script type='text/javascript' src='${pageContext.request.contextPath}/js/common.js'></script>
        <script type='text/javascript' src='${pageContext.request.contextPath}/js/css.js'></script>
        <script type='text/javascript' src='${pageContext.request.contextPath}/js/standardista-table-sorting.js'></script>
        <script type='text/javascript' src='${pageContext.request.contextPath}/js/prettify.js'></script>
        <script>addEvent(window, 'load', prettyPrint)</script>
    </c:if>

</head>

  <body>
    <div id="outer">
      <div id="middle">
        <div id="header" class="topbar">
            <div id="asf_logo">
                <div id="logo">
                    <img src="images/logo.gif" alt="JBoss A-MQ">
                </div>
            </div>
            <div id="topbar_nav_container">
            </div>
            <div id="topbar_user_container">
            </div>
        </div>


                    <div class="topbar-inner">
                        <div id="site-breadcrumbs">
                            <a href="<c:url value='/index.jsp'/>" title="Home">Home</a>
                            &#124;
                            <a href="<c:url value='/queues.jsp'/>" title="Queues">Queues</a>
                            &#124;
                            <a href="<c:url value='/topics.jsp'/>" title="Topics">Topics</a>
                            &#124;
                            <a href="<c:url value='/subscribers.jsp'/>" title="Subscribers">Subscribers</a>
                            &#124;
                            <a href="<c:url value='/connections.jsp'/>" title="Connections">Connections</a>
                            &#124;
                            <a href="<c:url value='/network.jsp'/>" title="Network">Network</a>
                            &#124;
                             <a href="<c:url value='/scheduled.jsp'/>" title="Scheduled">Scheduled</a>
                            &#124;
                            <a href="<c:url value='/send.jsp'/>"
                               title="Send">Send</a>
                        </div>
                        <div id="site-quicklinks"><P>
                            <a href="http://www.redhat.com/products/jbossenterprisemiddleware/amq/"
                               title="Get help and support using JBoss A-MQ">Support</a></p>
                        </div>
                    </div>

                    <table border="0" width="100%">
                        <tbody>
                            <tr>
                                <td valign="top" width="100%" style="overflow:hidden;">
                                    <div class="body-content">
                                        <div class="body-content">
                                          <p align="center">Broker is currently in <b>slave</b> mode!</p>
                                        </div>
                                    </div>
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
                                                    <li><a href="http://fusesource.com/documentation/fuse-mq-enterprise-documentation/"
                                                           title="The most popular and powerful open source Message Broker">Documentation</a></li>
                                                    </li>
                                                </ul>
                                            </div>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
        <div id="footer">
            &copy; 2012 FuseSource Corp. All rights reserved.
        </div>
        <div id="dialog_container">
        </div>
      </div>
    </div>
  </body>
</html>

