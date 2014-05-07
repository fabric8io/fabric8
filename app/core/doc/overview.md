<h3 class="help-header">Welcome to <img class='no-shadow' ng-src='{{branding.appLogo}}'>{{branding.appName}} </h3>

Don't cha wish your console was <a href="http://www.youtube.com/watch?v=YNSxNsr4wmA">hawt like me</a>? I'm <i>hawt</i> so you can stay cool!

<b>{{branding.appName}}</b> is a lightweight and <a href="http://hawt.io/plugins/index.html">modular</a> HTML5 web console with <a href="http://hawt.io/plugins/index.html">lots of plugins</a> for managing your Java stuff

##### General Navigation #####
Primary navigation in [{{branding.appName}}](http://hawt.io "{{branding.appName}}") is via the top navigation bar.

![Main Navigation Bar](app/core/doc/img/main-nav.png "Main Navigation Bar")

Clicking on a navigation link will take you to that plugin's main page.

<i class='yellow text-shadowed icon-warning-sign'></i> **Note:** The available links in the navigation bar depend on what plugins are available and what JMX MBeans are available in the JVM, and so may differ from what is shown here.

##### Getting Help #####
Click the Help icon (<i class='icon-question-sign'></i>) in the main navigation bar to access [{{branding.appName}}](http://hawt.io "{{branding.appName}}")'s help system. Browse the available help topics for plugin-specific documentation using the help navigation bar on the left.

![Help Topic Navigation Bar](app/core/doc/img/help-topic-nav.png "Help Topic Navigation Bar")

Available sub-topics for each plugin can be selected via the secondary navigation bar above the help display area.

![Help Sub-Topic Navigation Bar](app/core/doc/img/help-subtopic-nav.png "Help Sub-Topic Navigation Bar")

##### Preferences #####
Click the Preferences icon (<i class='icon-cogs'></i>) in the main navigation bar to access the [Preferences](#/preferences) page.  Available configuration options include, but not limited to:

###### Behaviour ######
- **Update Rate** - How often [{{branding.appName}}](http://hawt.io "{{branding.appName}}") polls the [Jolokia](http://jolokia.org) backend for JMX metrics.  Can be set to "No Refreshes" and intervals of 1, 2, 5, 10, and 30 seconds.

  <i class='yellow text-shadowed icon-warning-sign'></i> **Note:** Setting this to "No Refreshes" will disable charting, as charting requires fetching periodic metric updates.
- **Auto Refresh** - Automatically refresh the browser window if [{{branding.appName}}](http://hawt.io "{{branding.appName}}") detects a change in available plugins.
- **Host Identification** - To associate a label and colour to host(s) when [connecting](#/help/jvm) to containers.

###### Logs ######
- **Log cache size** - How many log lines to cache in the [Logs](#/help/log) plugin.

###### Git ######
- **User Name** - The git username to use when committing updates to the [Dashboard](#/help/dashboard/) or [Wiki](#/help/wiki).
- **Email address** - The e-mail address to associate with git commits.

###### ActiveMQ ######
- **User Name** - The username to use when connecting to the [ActiveMQ](#/help/activemq/) broker.
- **Password** - The password for the username.

###### Editor ######
- **Theme** - The theme to be used by the CodeMirror code editor
- **Tab Size** - The tab stop size to be used by the CodeMirror code editor.


##### Further Reading #####
- [hawtio](http://hawt.io "hawtio") website
- Chat with the hawtio team on IRC by joining **#hawtio** on **irc.freenode.net**
- Help improve [hawtio](http://hawt.io "hawtio") by [contributing](http://hawt.io/contributing/index.html)
- [hawtio on github](https://github.com/hawtio/hawtio)





