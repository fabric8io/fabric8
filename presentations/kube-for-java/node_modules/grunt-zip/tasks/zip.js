/*
 * grunt-zip
 * https://github.com/twolfson/grunt-zip
 *
 * Copyright (c) 2013 Todd Wolfson
 * Licensed under the MIT license.
 */

var fs = require('fs'),
    path = require('path'),
    Zip = require('node-zip'),
    gruntRetro = require('grunt-retro');
module.exports = function(grunt) {
  // Load and bind grunt-retro
  grunt = gruntRetro(grunt);

  // Please see the grunt documentation for more information regarding task and
  // helper creation: https://github.com/gruntjs/grunt/blob/master/docs/toc.md

  // ==========================================================================
  // TASKS
  // ==========================================================================

  // Localize underscore
  var _ = grunt.utils._;

  grunt.registerMultiTask('zip', 'Zip files together', function() {
    // Localize variables
    var file = this.file,
        data = this.data,
        src = file.src,
        dest = file.dest,
        router = data.router;

    // Fallback options (e.g. base64, compression)
    _.defaults(data, {
      base64: false
    });

    // Collect our file paths
    var globOptions = {dot: data.dot},
        srcFolders = grunt.file.expandDirs(globOptions, src),
        srcFiles = grunt.file.expandFiles(globOptions, src);

    // If there is no router
    if (!router) {
      // Grab the cwd and return the relative path as our router
      var cwd = data.cwd || process.cwd();
      router = function routerFn (filepath) {
        return path.relative(cwd, filepath);
      };
    }

    // Generate our zipper
    var zip = new Zip();

    // For each of the srcFolders, route it and add it to the zip
    srcFolders.forEach(function (folderpath) {
      var routedPath = router(folderpath);
      zip.folder(routedPath);
    });

    // For each of the srcFiles
    srcFiles.forEach(function (filepath) {
      // Read in the content and add it to the zip
      var input = fs.readFileSync(filepath, 'binary'),
          routedPath = router(filepath);

      // Add it to the zip
      zip.file(routedPath, input, {binary: true});
    });

    // Create the destination directory
    var destDir = path.dirname(dest);
    grunt.file.mkdir(destDir);

    // Write out the content
    var output = zip.generate({base64: data.base64, compression: data.compression});
    fs.writeFileSync(dest, output, 'binary');

    // Fail task if errors were logged.
    if (this.errorCount) { return false; }

    // Otherwise, print a success message.
    grunt.log.writeln('File "' + dest + '" created.');
  });

  function echo(a) {
    return a;
  }
  grunt.registerMultiTask('unzip', 'Unzip files into a folder', function() {
    // Collect the filepaths we need
    var file = this.file,
        data = this.data,
        src = file.src,
        srcFiles = grunt.file.expand(src),
        dest = file.dest,
        router = data.router || echo;

    // Fallback options (e.g. checkCRC32)
    _.defaults(data, {
      base64: false,
      checkCRC32: true
    });

    // Iterate over the srcFiles
    srcFiles.forEach(function (filepath) {
      // Read in the contents
      var input = fs.readFileSync(filepath, 'binary');

      // Unzip it
      var zip = new Zip(input, {base64: data.base64, checkCRC32: data.checkCRC32});

      // Pluck out the files
      var files = zip.files,
          filenames = Object.getOwnPropertyNames(files);

      // Filter out all non-leaf files
      filenames = filenames.filter(function filterNonLeafs (filename) {
        // Iterate over the other filenames
        var isLeaf = true,
            i = filenames.length,
            otherFile,
            pathToFile,
            isParentDir;
        while (i--) {
          // If the other file is the current file, skip it
          otherFile = filenames[i];
          if (otherFile === filename) {
            continue;
          }

          // Determine if this file contains the other
          pathToFile = path.relative(filename, otherFile);
          isParentDir = pathToFile.indexOf('..') === -1;

          // If it does, falsify isLeaf
          if (isParentDir) {
            isLeaf = false;
            break;
          }
        }

        // Return that the file was a leaf
        return isLeaf;
      });

      // Iterate over the files
      filenames.forEach(function (filename) {
        // Find the content
        var fileObj = files[filename],
            content = fileObj.data,
            routedName = router(filename);

        // Determine the filepath
        var filepath = path.join(dest, routedName);

        // Create the destination directory
        var fileDir = path.dirname(filepath);
        grunt.file.mkdir(fileDir);

        // Write out the content
        fs.writeFileSync(filepath, content, 'binary');
      });
    });

    // Fail task if errors were logged.
    if (this.errorCount) { return false; }

    // Otherwise, print a success message.
    grunt.log.writeln('File "' + this.file.dest + '" created.');
  });

};
