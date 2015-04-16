var path = require('path');
module.exports = function (grunt) {

  // TODO: Build in `cwd` handling for zip
  // TODO: Build in `router` handling for unzip

  // Project configuration.
  grunt.initConfig({
    pkg: require('../package.json'),
    clean: ['actual/'],
    zip: {
      single: {
        src: ['test_files/file.js'],
        dest: 'actual/single_zip/file.zip'
      },
      multi: {
        src: ['test_files/file.js', 'test_files/file2.js'],
        dest: 'actual/multi_zip/file.zip'
      },
      nested: {
        src: 'test_files/nested/**/*',
        dest: 'actual/nested_zip/file.zip'
      },
      image: {
        src: 'test_files/smile.gif',
        dest: 'actual/image_zip/file.zip'
      },
      router: {
        src: ['test_files/nested/hello.js', 'test_files/nested/nested2/hello10.txt'],
        dest: 'actual/router_zip/file.zip',
        router: function (filepath) {
          var filename = path.basename(filepath);
          return filename;
        }
      },
      cwd: {
        src: ['test_files/nested/hello.js', 'test_files/nested/nested2/hello10.txt'],
        dest: 'actual/cwd_zip/file.zip',
        cwd: 'test_files/nested'
      },
      dot: {
        src: ['test_files/dot/.test/hello.js', 'test_files/dot/test/.examplerc'],
        dest: 'actual/dot_zip/file.zip',
        dot: true
      }
    },
    unzip: {
      single: {
        src: 'test_files/file.zip',
        dest: 'actual/single_unzip'
      },
      nested: {
        src: 'test_files/nested.zip',
        dest: 'actual/nested_unzip'
      },
      router: {
        src: 'test_files/nested.zip',
        dest: 'actual/router_unzip',
        router: function (filepath) {
          var filename = path.basename(filepath);
          return filename;
        }
      },
      'test-zip-nested': {
        src: 'actual/nested_zip/file.zip',
        dest: 'actual/nested_zip/unzip'
      },
      'test-zip-image': {
        src: 'actual/image_zip/file.zip',
        dest: 'actual/image_zip/unzip'
      },
      'test-zip-router': {
        src: 'actual/router_zip/file.zip',
        dest: 'actual/router_zip/unzip'
      },
      'test-zip-cwd': {
        src: 'actual/cwd_zip/file.zip',
        dest: 'actual/cwd_zip/unzip'
      },
      'test-zip-dot': {
        src: 'actual/dot_zip/file.zip',
        dest: 'actual/dot_zip/unzip'
      }
    },
    test: {
      common: 'zip_test.js'
    }
  });

  // Load local tasks.
  grunt.loadTasks('../tasks');

  // Load grunt contrib clean (chdir for 0.4)
  process.chdir('..');
  grunt.loadNpmTasks('grunt-contrib-clean');
  process.chdir(__dirname);

  // Run project task then tests.
  grunt.registerTask('default', 'clean zip unzip test');
};