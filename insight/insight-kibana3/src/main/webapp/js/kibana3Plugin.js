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

var pluginName = 'kibana3';
var kibana3TopLevel = "/kibana3";

_.each(config.modules, function(v) {
  var script = "panels/" + v + "/modules.js";
  modules.push('kibana.'+v);
});

angular.module(pluginName, modules).config(['$routeProvider', function($routeProvider) {

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

      viewRegistry[pluginName] = layoutFull;

      // Set up top-level link to our plugin
      workspace.topLevelTabs.push({
        content: "Logs",
        title: "Insight Logs",
        isValid: function() { return true; },
        href: function() { return "#/kibanalogs"; },
        isActive: function() { return workspace.isLinkActive("/kibana3/dashboard/file/log"); }
      });
      workspace.topLevelTabs.push({
        content: "Camel Events",
        title: "Insight Camel exchanges",
        isValid: function() { return true; },
        href: function() { return "#/kibanacamel"; },
        isActive: function() { return workspace.isLinkActive("/kibana3/dashboard/file/camel"); }
      });

    });


hawtioPluginLoader.addModule(pluginName);
modules.forEach(function(module) {
  hawtioPluginLoader.addModule(module);
});
