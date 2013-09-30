angular.module('eshead', ['hawtioCore'])
    .config(function($routeProvider) {
        $routeProvider
            .when('/eshead', {
                templateUrl: '/eshead/hawtio/layout.html'
            });
    }).run(function(workspace, viewRegistry, layoutFull) {

      viewRegistry['eshead'] = layoutFull;

      // Set up top-level link to our plugin
      workspace.topLevelTabs.push({
        content: "ESHead",
        title: "ElasticSearch Head",
        isValid: function() { return true; },
        href: function() { return "#/eshead"; },
        isActive: function() { return workspace.isLinkActive("eshead"); }
      });

    }).controller('EsHeadController', function($scope) {
        $scope.app = new app.App("#eshead", {
            id: "es",
            base_uri: "/eshead/es",
            auth_user : "",
            auth_password : undefined
        });
    });

hawtioPluginLoader.addModule('eshead');
