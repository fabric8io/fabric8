<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <title>${requestContext.brokerQuery.brokerAdmin.brokerName} : <decorator:title default="JBoss A-MQ Console"/></title>
  <style type="text/css" media="screen">
    @import url('${pageContext.request.contextPath}/styles/sorttable.css');
    @import url('${pageContext.request.contextPath}/styles/type-settings.css');
    @import url('${pageContext.request.contextPath}/styles/site.css');
    @import url('${pageContext.request.contextPath}/styles/prettify.css');
    @import url('${pageContext.request.contextPath}/styles/bootstrap.css');
    @import url('${pageContext.request.contextPath}/styles/fuse.css');
  </style>
  <c:if test="${!disableJavaScript}">
    <script type='text/javascript' src='${pageContext.request.contextPath}/js/common.js'></script>
    <script type='text/javascript' src='${pageContext.request.contextPath}/js/css.js'></script>
    <script type='text/javascript' src='${pageContext.request.contextPath}/js/standardista-table-sorting.js'></script>
    <script type='text/javascript' src='${pageContext.request.contextPath}/js/prettify.js'></script>
    <script type='text/javascript' src='${pageContext.request.contextPath}/js/jquery.js'></script>
    <script>addEvent(window, 'load', prettyPrint)</script>

    <script type='text/javascript'>
      var adjustHeight = function() {
        windowHeight = $(window).height();
        headerHeight = $("#header").height();
        topbarInnerHeight = $(".topbar-inner").height();
        footerHeight = $("#footer").height();
        containerHeight = windowHeight - headerHeight - topbarInnerHeight - footerHeight - 49;
        $(".body-table").css("min-height", "" + containerHeight + "px");
      }

      $(document).ready(function() {
        adjustHeight();
        $(window).resize(adjustHeight);
      });
    </script>
  </c:if>

  <decorator:head/>
</head>

<body>
  <div id="outer">
    <div id="middle">
      <div id="header" class="topbar">
        <div id="logo">
          <img src="images/logo.png" alt="Red Hat">
        </div>
        <div id="productname">
          JBoss A-MQ
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

      <table class="body-table" border="0" width="100%">
        <tbody>
          <tr>
            <td valign="top" width="100%" style="overflow:hidden;">
              <div class="body-content">
                <decorator:body/>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div id="footer">&copy; 2013 Red Hat, Inc. and/or its affiliates. All rights reserved.</div>
      <div id="dialog_container"></div>
    </div>
  </div>
</body>
</html>

