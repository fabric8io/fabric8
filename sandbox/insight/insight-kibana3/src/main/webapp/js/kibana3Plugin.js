var modules = [
  'kibana.services',
  'kibana.controllers',
  'kibana.filters',
  'kibana.directives',
  'elasticjs.service',
  '$strap.directives',
  'kibana.panels',
  'ngSanitize'
];

_.each(config.modules, function(v) {
  modules.push('kibana.' + v);
});

angular.module('kibana3', modules).config(['$routeProvider', function($routeProvider) {

  $routeProvider
      .when('/kibana3', {
        redirectTo: '/kibana3/dashboard/file/log'
      })
      .when('/kibanalogs', {
        redirectTo: '/kibana3/dashboard/file/log'
      })
      .when('/kibanacamel', {
        redirectTo: '/kibana3/dashboard/file/camel'
      })
      .when('/kibana3/dashboard', {
        redirectTo: '/kibana3/dashboard/file/log'
      })
      .when('/kibana3/dashboard/:type/:id', {
        templateUrl: '/kibana3/partials/dashboardPlugin.html'
      })
      .when('/kibana3/dashboard/:type/:id/:params', {
        templateUrl: '/kibana3/partials/dashboardPlugin.html'
      });
}])
    .run(function(workspace, viewRegistry, layoutFull) {

      viewRegistry['kibana3'] = layoutFull;

      // Set up top-level link to our plugin
      workspace.topLevelTabs.push({
        content: "Logs",
        id: "insightlogs",
        title: "Insight Logs",
        isValid: function() { return true; },
        href: function() { return "#/kibanalogs"; },
        isActive: function() { return workspace.isLinkActive("/kibana3/dashboard/file/log"); }
      });
      workspace.topLevelTabs.push({
        content: "Camel Events",
        id: "kibanacamel",
        title: "Insight Camel exchanges",
        isValid: function() { return true; },
        href: function() { return "#/kibanacamel"; },
        isActive: function() { return workspace.isLinkActive("/kibana3/dashboard/file/camel"); }
      });

      var link = $("<link>");
      $("head").append(link);
      link.attr({
        rel: 'stylesheet',
        type: 'text/css',
        href: '/kibana3/common/css/kibana-hawtio.css'
      });
    });


hawtioPluginLoader.addModule('kibana3');
modules.forEach(function(module) {
  hawtioPluginLoader.addModule(module);
});
