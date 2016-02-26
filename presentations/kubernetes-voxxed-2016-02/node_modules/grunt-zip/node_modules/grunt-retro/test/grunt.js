module.exports = function (grunt) {
  // Load in common config
  grunt = require('./grunt.common.js')(grunt);

  // Set up test config
  grunt.config.set('test', {
    common: 'retro_test.js'
  });

  // Run project task then tests.
  // TEST: We can actually run single string of queries
  grunt.registerTask('default', 'test-setup test');
};