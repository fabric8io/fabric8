<h3 class="about-header">About <img class='no-shadow' ng-src='{{branding.appLogo}}'>{{branding.appName}} </h3>

<div ng-show="!customBranding">
  Don't cha wish your console was <a href="http://www.youtube.com/watch?v=YNSxNsr4wmA">hawt like me</a>? I'm <i>hawt</i> so you can stay cool!
  <p/>
  <b>{{branding.appName}}</b> is a lightweight and <a href="http://hawt.io/plugins/index.html">modular</a> HTML5 web console with <a href="http://hawt.io/plugins/index.html">lots of plugins</a> for managing your Java stuff
  <p/>
</div>

<div ng-show="customBranding">
  <p/>
  {{branding.appName}} is powered by <img class='no-shadow' ng-src='img/logo-16px.png'><a href="http://hawt.io/">hawtio</a>
  <p/>
</div>

<h4>Versions</h4>

  **hawtio** version: {{hawtioVersion}}

  **jolokia** version: {{jolokiaVersion}}

<div ng-show="serverVendor">
  **server** version: {{serverVendor}} {{serverProduct}} {{serverVersion}}
</div>
