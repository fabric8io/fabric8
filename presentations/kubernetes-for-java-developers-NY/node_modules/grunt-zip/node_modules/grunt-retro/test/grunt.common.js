// Configure Gruntfile.js and grunt.js with common parts
module.exports = function (grunt) {
  // Project configuration.
  grunt.initConfig({
    'pkg': require('../package.json'),
    'echo-src': {
      'actual/src_compact.txt': 'test_files/file.js',
      single: {
        src: 'test_files/file.js',
        dest: 'actual/src_single.txt'
      },
      multi: {
        src: ['test_files/file.js', 'test_files/file2.js'],
        dest: 'actual/src_multi.txt'
      },
      expansion: {
        src: ['test_files/{file,file2}.js'],
        dest: 'actual/src_expansion.txt'
      },
      // TODO: Move this to a string... =_=
      uri: {
        src: ['http://google.com/'],
        dest: 'actual/src_uri.txt'
      }
    },
    'echo-dest': {
      'actual/compact_test.txt': 'actual/dest_compact.txt',
      single: {
        src: 'actual/dest_simple.txt',
        dest: 'actual/simple_test.js'
      }
    },
    'expand-dirs': {
      string: {
        src: 'test_files/**/*',
        dest: 'actual/expand_dirs_string.txt'
      },
      array: {
        src: ['test_files/**/*'],
        dest: 'actual/expand_dirs_array.txt'
      }
    },
    'expand-files': {
      string: {
        src: 'test_files/**/*',
        dest: 'actual/expand_files_string.txt'
      },
      array: {
        src: ['test_files/**/*'],
        dest: 'actual/expand_files_array.txt'
      }
    },
    'expand-dirs-with-dot': {
      string: {
        src: 'test_files/**/*',
        dest: 'actual/expand_dirs_options.txt'
      }
    },
    'expand-files-with-dot': {
      string: {
        src: 'test_files/**/*',
        dest: 'actual/expand_files_options.txt'
      }
    }
  });

  // Register task for testing src
  grunt.registerMultiTask('echo-src', 'Save src to dest file', function () {
    var file = this.file,
        src = file.src,
        dest = this.data.dest || this.target;
    grunt.file.write(dest, JSON.stringify(src));
  });

  // Register task for testing dest
  grunt.registerMultiTask('echo-dest', 'Save dest to src file', function () {
    var file = this.file,
        dest = file.dest,
        src = this.data.src || this.data;
    grunt.file.write(src, JSON.stringify(dest));
  });

  // Register task for testing expandDirs
  grunt.registerMultiTask('expand-dirs', 'Save src to dest file', function () {
    var file = this.file,
        src = file.src,
        srcDirs = grunt.file.expandDirs(src),
        dest = this.data.dest || this.target;
    srcDirs.sort();
    grunt.file.write(dest, JSON.stringify(srcDirs));
  });

  // Register task for testing expandFiles
  grunt.registerMultiTask('expand-files', 'Save src to dest file', function () {
    var file = this.file,
        src = file.src,
        srcFiles = grunt.file.expandFiles(src),
        dest = this.data.dest || this.target;
    srcFiles.sort();
    grunt.file.write(dest, JSON.stringify(srcFiles));
  });

  // Register task for testing expandDirs
  grunt.registerMultiTask('expand-dirs-with-dot', 'Save src (with dot) to dest file', function () {
    var file = this.file,
        src = file.src,
        srcDirs = grunt.file.expandDirs({'dot': true}, src),
        dest = this.data.dest || this.target;
    srcDirs.sort();
    grunt.file.write(dest, JSON.stringify(srcDirs));
  });

  // Register task for testing expandFiles
  grunt.registerMultiTask('expand-files-with-dot', 'Save src (with dot) to dest file', function () {
    var file = this.file,
        src = file.src,
        srcFiles = grunt.file.expandFiles({'dot': true}, src),
        dest = this.data.dest || this.target;
    srcFiles.sort();
    grunt.file.write(dest, JSON.stringify(srcFiles));
  });

  // Register task about test setup
  grunt.registerTask('echo', 'echo-src echo-dest');
  grunt.registerTask('expand', 'expand-dirs expand-files');
  grunt.registerTask('expand-with-dot', 'expand-dirs-with-dot expand-files-with-dot');
  grunt.registerTask('test-setup', 'echo expand expand-with-dot');

  // Return grunt for a fluent interface
  return grunt;
};