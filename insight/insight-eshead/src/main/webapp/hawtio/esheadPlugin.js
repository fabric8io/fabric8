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
        id: "eshead",
        title: "ElasticSearch Head",
        isValid: function() { return true; },
        href: function() { return "#/eshead"; },
        isActive: function() { return workspace.isLinkActive("eshead"); }
      });

      var link = $("<link>");
      var head = $("head");
      head.append(link);

      link.attr({
        rel: 'stylesheet',
        type: 'text/css',
        href: '/eshead/dist/app.css'
      });

      link = $("<link>");
      head.append(link);

      link.attr({
        rel: 'stylesheet',
        type: 'text/css',
        href: '/eshead/hawtio/tweaks.css'
      });

    }).directive('eshead', function() {
      return {
        restrict: 'A',
        scope: false,
        link: function($scope, $element, $attrs) {
          $scope.app = new app.App($element, {
            id: "es",
            base_uri: "/eshead/es",
            auth_user : "",
            auth_password : undefined
          });
        }
      };
    }).controller('EsHeadController', function($scope) {

    });

hawtioPluginLoader.addModule('eshead');
