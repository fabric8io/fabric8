var grunt = require('grunt');

/*
  ======== A Handy Little Nodeunit Reference ========
  https://github.com/caolan/nodeunit

  Test methods:
    test.expect(numAssertions)
    test.done()
  Test assertions:
    test.ok(value, [message])
    test.equal(actual, expected, [message])
    test.notEqual(actual, expected, [message])
    test.deepEqual(actual, expected, [message])
    test.notDeepEqual(actual, expected, [message])
    test.strictEqual(actual, expected, [message])
    test.notStrictEqual(actual, expected, [message])
    test.throws(block, [error], [message])
    test.doesNotThrow(block, [error], [message])
    test.ifError(value)
*/

var outline = {
  'A grunt@0.3 plugin': {
    'using grunt-retro': {
      'can read single src files': true,
      'can read multiple src files': true,
      'handles src expansions': true,
      'can use a URI as src file': true,
      'can read dest': true,
      'can register and use helpers': true,
      'can access utils': true,
      // TODO: There is probably room for abstraction on this last one (mapping of key lookup to key lookup)
      // {'grunt.utils.minimatch': 'grunt.file.glob.minimatch'}
      'can access grunt.file.glob.minimatch': true,
      'can expand directories': true,
      'can expand files': true,
      'can expand directories with options': true,
      'can expand files with options': true
    }
  }
};

// ANTI-PATTERN: Helper function for comparing tests
function compareFiles(filename) {
  return function fileComparison (test) {
    test.expect(1);

    // Load in the expected and actual content
    var expectedContent = grunt.file.read('expected/' + filename),
        actualContent = grunt.file.read('actual/' + filename);

    // Assert they are the same and return
    test.equal(actualContent, expectedContent, 'should return the correct value.');
    test.done();
  };
}

exports['retro'] = {
  setUp: function(done) {
    // setup here
    done();
  },
  'src-compact': compareFiles('src_compact.txt'),
  'src-single': compareFiles('src_single.txt'),
  'src-multi': compareFiles('src_multi.txt'),
  'src-expansion': compareFiles('src_expansion.txt'),
  'dest-compact': compareFiles('dest_compact.txt'),
  'dest-simple': compareFiles('dest_simple.txt'),
  'can register and use helpers': function (test) {
    test.expect(1);

    // Register and use our helper
    grunt.registerHelper('hello', function () {
      return 'world';
    });
    test.strictEqual(grunt.helper('hello'), 'world');

    // Callback
    test.done();
  },
  'access utils': function (test) {
    // Assert our utils exist
    test.expect(1);
    test.ok(grunt.utils);
    test.done();
  },
  'can access grunt.file.glob.minimatch': function (test) {
    test.expect(1);
    test.ok(grunt.file.glob.minimatch);
    test.done();
  },
  'expand-dirs-string': compareFiles('expand_dirs_string.txt'),
  'expand-dirs-array': compareFiles('expand_dirs_array.txt'),
  'expand-files-string': compareFiles('expand_files_string.txt'),
  'expand-files-array': compareFiles('expand_files_array.txt'),
  'expand-dirs-options': compareFiles('expand_dirs_options.txt'),
  'expand-files-options': compareFiles('expand_files_options.txt')
};
