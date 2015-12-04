/*
 * grunt-retro
 * https://github.com/twolfson/grunt-retro
 *
 * Copyright (c) 2013 Todd Wolfson
 * Licensed under the MIT license.
 */

var assert = require('assert');
module.exports = function (grunt) {
  // Proxy registerTask
  var registerTaskFn = grunt.registerTask;
  grunt.registerTask = function (taskName, taskList, taskFn) {
    // Capture arguments for manipulation
    var args = [].slice.call(arguments);

    // If there is no taskFn
    if (!taskFn) {
      // and if taskList is not an array
      if (!Array.isArray(taskList)) {
        args[1] = taskList.split(/\s+/g);
      }
    }

    // Invoke the original function
    return registerTaskFn.apply(this, args);
  };


  // Proxy registerMultiTask
  var registerMultiFn = grunt.registerMultiTask;
  grunt.registerMultiTask = function (taskName, description, taskFn) {
    var args = [].slice.call(arguments);

    // Wrap taskFn
    args[2] = function proxiedTaskFn () {
      // Fallback this.file
      // this.file = this.file || this.files[0].orig;
      var file = this.file;
      if (!file) {
        // Keep the original formatting
        this.file = this.files[0].orig;

        // If the src was single, downcast it from array to string
        // DEV: this.files will *always* have `src` as an array
        var src = this.data.src || this.data;
        if (!Array.isArray(src)) {
          this.file.src = this.file.src[0];
        }
      }

      // Call the original function
      return taskFn.apply(this, arguments);
    };

    // Call the original function
    return registerMultiFn.apply(this, args);
  };

  // Fallback grunt.utils and grunt...minimatch
  grunt.utils = grunt.utils || grunt.util;
  grunt.file.glob.minimatch = grunt.file.glob.minimatch || grunt.file.minimatch;

  // Set up storage for helpers
  var helpers = {};

  // Fallback helper helper
  function helper(name) {
    // Look up and assert the helper exists
    var helperFn = helpers[name];
    assert(helperFn, 'GRUNT HELPER: "' + name + '" could not be found.');

    // Call the helper with the arguments
    var args = [].slice.call(arguments, 1);
    return helperFn.apply(this, args);
  }
  grunt.helper = grunt.helper || helper;

  // Fallback registerHelper
  function registerHelper(name, fn) {
    helpers[name] = fn;
  }
  grunt.registerHelper = grunt.registerHelper || registerHelper;

  // Fallback file expansions
  var _ = grunt.utils._,
      gruntFileExpand = grunt.file.expand;
  grunt.file.expandDirs = grunt.file.expandDirs || function (options, dirs) {
    // If there are no dirs, fallback options
    if (dirs === undefined) {
      dirs = options;
      options = {};
    }

    // Collect the directories
    var expandOptions = _.extend({filter: 'isDirectory'}, options),
        expandedDirs = gruntFileExpand(expandOptions, dirs);

    // Append a `/` to directories (except for root)
    expandedDirs = expandedDirs.map(function (dir) {
      if (dir === '/') { return dir; }
      return dir + '/';
    });

    // Return the expanded dirs
    return expandedDirs;
  };
  grunt.file.expandFiles = grunt.file.expandFiles || function (options, files) {
    if (files === undefined) {
      files = options;
      options = {};
    }
    var expandOptions = _.extend({filter: 'isFile'}, options),
        expandedFiles = gruntFileExpand(expandOptions, files);
    return expandedFiles;
  };

  // Return grunt for a fluent interface
  return grunt;
};
