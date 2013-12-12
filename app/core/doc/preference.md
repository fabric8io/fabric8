### Preference

The preference is used to configure **hawtio** and the plugins.

##### Core Plugins #####

###### Behaviour ######
- **Welcome Page** - Whether to show welcome page on startup.
- **Update Rate** - How often [{{branding.appName}}](http://hawt.io "{{branding.appName}}") polls the [Jolokia](http://jolokia.org) backend for JMX metrics.  Can be set to "No Refreshes" and intervals of 1, 2, 5, 10, and 30 seconds.
  <i class='yellow text-shadowed icon-warning-sign'></i> **Note:** Setting this to "No Refreshes" will disable charting, as charting requires fetching periodic metric updates.
- **Auto Refresh** - Automatically refresh the browser window if [{{branding.appName}}](http://hawt.io "{{branding.appName}}") detects a change in available plugins.
- **Log Level** - Logging level used by the logging console (<i class='icon-desktop'></i>)
- **Log Buffer** - Number of log lines to keep in the logging console (<i class='icon-desktop'></i>)
- **Host Identification** - To associate a label and colour to host(s) when [connecting](#/help/jvm) to containers.

###### Logs ######
- **Log cache size** - How many log lines to cache in the [Logs](#/help/log) plugin.

###### Git ######
- **User Name** - The git username to use when committing updates to the [Dashboard](#/help/dashboard/) or [Wiki](#/help/wiki).
- **Email address** - The e-mail address to associate with git commits.

###### Editor ######
- **Theme** - The theme to be used by the CodeMirror code editor
- **Tab Size** - The tab stop size to be used by the CodeMirror code editor.


