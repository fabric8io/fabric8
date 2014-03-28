/*
 * Simple script loader and registry
 */

(function( window, undefined) {
  var log = Logger.get('PluginLoader');
  var hawtioPluginLoader = {
    urls: [],
    modules: []
  };

  hawtioPluginLoader.addModule = function(module) {
    log.debug("Adding module: " + module);
    hawtioPluginLoader.modules.push(module);
  };

  hawtioPluginLoader.addUrl = function(url) {
    log.debug("Adding URL: " + url);
    hawtioPluginLoader.urls.push(url);
  };

  hawtioPluginLoader.getModules = function() {
    return hawtioPluginLoader.modules.clone();
  };

  /**
   * Parses the given query search string of the form "?foo=bar&whatnot"
   * @param text
   * @return a map of key/values
   */
  hawtioPluginLoader.parseQueryString = function(text) {
      var query = (text || window.location.search || '?');
      var idx = -1;
      if (angular.isArray(query)) {
        query = query[0];
      }
      idx = query.indexOf("?");
      if (idx >= 0) {
        query = query.substr(idx + 1);
      }
      // if query string ends with #/ then lets remove that too
      idx = query.indexOf("#/");
      if (idx > 0) {
        query = query.substr(0, idx);
      }
      var map = {};
      query.replace(/([^&=]+)=?([^&]*)(?:&+|$)/g, function(match, key, value) {
          (map[key] = map[key] || []).push(value); 
        });
      return map;
  };

  /**
   * Parses the username:password from a http basic auth URL, e.g.
   * http://foo:bar@example.com
   */
  hawtioPluginLoader.getCredentials = function(urlString) {
/*
    No Uri class outside of IE right?

    var uri = new Uri(url);
    var credentials = uri.userInfo();
*/
    if (urlString) {
      var credentialsRegex = new RegExp(/.*:\/\/([^@]+)@.*/);
      var m = urlString.match(credentialsRegex);
      if (m && m.length > 1) {
        var credentials = m[1];
        if (credentials && credentials.indexOf(':') > -1) {
          return credentials.split(':');
        }
      }
    }
    return [];
  };


  hawtioPluginLoader.loaderCallback = null;

  hawtioPluginLoader.setLoaderCallback = function(cb) {
    hawtioPluginLoader.loaderCallback = cb;
    log.debug("Setting callback to : ", hawtioPluginLoader.loaderCallback);
  };


  hawtioPluginLoader.loadPlugins = function(callback) {

    var lcb = hawtioPluginLoader.loaderCallback;

    var plugins = {};

    var urlsToLoad = hawtioPluginLoader.urls.length;
    var totalUrls = urlsToLoad;

    var loadScripts = function() {

      // keep track of when scripts are loaded so we can execute the callback
      var loaded = 0;
      $.each(plugins, function(key, data) {
        loaded = loaded + data.Scripts.length;
      });

      var totalScripts = loaded;

      var scriptLoaded = function() {
        $.ajaxSetup({async:true});
        loaded = loaded - 1;
        if (lcb) {
          lcb.scriptLoaderCallback(lcb, totalScripts, loaded + 1);
        }
        if (loaded == 0) {
          callback();
        }
      };

      if (loaded > 0) {
        $.each(plugins, function(key, data) {

          data.Scripts.forEach( function(script) {

            // log.debug("Loading script: ", data.Name + " script: " + script);

            var scriptName = data.Context + "/" + script;
            log.debug("Fetching script: ", scriptName);
            $.ajaxSetup({async:false});
            $.getScript(scriptName)
                .done(function(textStatus) {
                  log.debug("Loaded script: ", scriptName);
                })
                .fail(function(jqxhr, settings, exception) {
                  log.info("Failed loading script: \"", exception.message, "\" (<a href=\"", scriptName, ":", exception.lineNumber, "\">", scriptName, ":", exception.lineNumber, "</a>)");
                })
                .always(scriptLoaded);
          });
        });
      } else {
        // no scripts to load, so just do the callback
        $.ajaxSetup({async:true});
        callback();
      }
    }

    if (urlsToLoad == 0) {
      loadScripts();
    } else {

      var urlLoaded = function () {
        urlsToLoad = urlsToLoad - 1;
        if (lcb) {
          lcb.urlLoaderCallback(lcb, totalUrls, urlsToLoad + 1);
        }
        if (urlsToLoad == 0) {
          loadScripts();
        }
      };

      var regex = new RegExp(/^jolokia:/);

      $.each(hawtioPluginLoader.urls, function(index, url) {

        if (regex.test(url)) {
          var parts = url.split(':');
          parts = parts.reverse();
          parts.pop();

          var url = parts.pop();
          var attribute = parts.reverse().join(':');
          var jolokia = new Jolokia(url);

          try {
            var data = jolokia.getAttribute(attribute, null);
            $.extend(plugins, data);
          } catch (Exception) {
            // console.error("Error fetching data: " + Exception);
          }
          urlLoaded();
        } else {

          log.debug("Trying url: ", url);

          $.get(url, function (data) {
                // log.debug("got data: ", data);
                $.extend(plugins, data);
              }).always(function() {
                urlLoaded();
              });
        }
      });
    }
  };

  hawtioPluginLoader.debug = function() {
    log.debug("urls and modules");
    log.debug(hawtioPluginLoader.urls);
    log.debug(hawtioPluginLoader.modules);
  };

  hawtioPluginLoader.setLoaderCallback({
    scriptLoaderCallback: function (self, total, remaining) {
      Logger.get("Core").debug("Total scripts: ", total, " Remaining: ", remaining);
    },
    urlLoaderCallback: function (self, total, remaining) {
      Logger.get("Core").debug("Total URLs: ", total, " Remaining: ", remaining);
    }
  });

  window.hawtioPluginLoader = hawtioPluginLoader;

})(window);


