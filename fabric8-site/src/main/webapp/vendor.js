// lets disable jolokia

// enable the Site plugin
(function (Site) {
  Site.sitePluginEnabled = true;
})(Site || {});

// default the perspective
(function (Perspective) {
  var metadata = Perspective.metadata || {};
  Perspective.defaultPerspective = "website";
  Perspective.defaultPageLocation = "#/site/book/doc/index.md";

  metadata["website"] = {
    "label": "fabric8",
    "isValid": function() { return true; },
    "lastPage": "#/site/book/doc/index.md",
    "topLevelTabs": {
      "includes": [
              /*
        {
          "content": "Get Started",
          "title": "How to get started using hawtio",
          "href": function () {
            return "#/site/doc/GetStarted.md";
          },
          "isValid": function () {
            return true;
          }
        },
        */
        {
          "content": "User Guide",
          "title": "All the docs on using hawtio",
          "href": function () {
            return "#/site/book/doc/index.md";
          },
          "isValid": function () {
            return true;
          }
        },
        {
          "content": "FAQ",
          "title": "Frequently Asked Questions",
          "href": function () {
            return "#/site/FAQ.md";
          },
          "isValid": function () {
            return true;
          }
        },
        {
          "content": "Community",
          "title": "Come on in and join our community!",
          "href": function () {
            return "#/site/doc/community.html";
          },
          "isValid": function () {
            return true;
          }
        },
        /*
        {
          "content": "Developers",
          "title": "Resources for developers if you want to hack on hawtio or provide your own plugins",
          "href": function () {
            return "#/site/doc/developers/index.md";
          },
          "isValid": function () {
            return true;
          }
        },
        */
        {
          "content": "github",
          "title": "fabric8's source code and issue tracker",
          "href": function () {
            return "https://github.com/jboss-fuse/fuse";
          },
          "isValid": function () {
            return true;
          }
        }
      ]
    }
  };

  // configure the branding
  angular.module("fabric8", ['hawtioCore', 'hawtio-ui']).
          run(function (branding) {
            branding.appName = "fabric8";
            //branding.appLogo = 'img/branding/RHJB_Fuse_UXlogotype_0513LL_white.svg';
          });

  hawtioPluginLoader.addModule("fabric8");

})(Perspective || {});


