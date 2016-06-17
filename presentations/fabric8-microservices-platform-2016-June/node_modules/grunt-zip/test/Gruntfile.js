module.exports = function (grunt) {
  // Load in legacy config
  require('./grunt')(grunt);

  // Add in 0.4 specific tests
  var _ = grunt.util._;
  var zipConfig = grunt.config.get('zip');
  grunt.config.set('zip', _.extend(zipConfig, {
    'actual/template_zip/<%= pkg.name %>.zip': ['test_files/file.js']
  }));

  // Configure nodeunit as test
  var testConfig = grunt.config.get('test');
  grunt.config.set('nodeunit', _.extend(testConfig, {
    '0.4': '0.4_test.js'
  }));

  // Load in nodeunit
  process.chdir('..');
  grunt.loadNpmTasks('grunt-contrib-nodeunit');
  process.chdir(__dirname);

  // Override default task
  grunt.registerTask('default', ['clean', 'zip', 'unzip', 'nodeunit']);
};