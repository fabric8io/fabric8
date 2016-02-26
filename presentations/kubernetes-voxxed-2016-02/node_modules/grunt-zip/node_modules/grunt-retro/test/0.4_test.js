// These are tests to ensure we don't break 0.4 functionality
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
  'A grunt@0.4 plugin': {
    'using grunt-retro': {
      'can have templating on src': true,
      'can have templating on dest': true
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

exports['0.4'] = {
  'src-template': compareFiles('src_template.txt'),
  'dest-template': compareFiles('dest_template.txt')
};
