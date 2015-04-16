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

var fs = require('fs');
// 0.4 specific test for twolfson/grunt-zip#6
exports['0.4'] = {
  'dest-template': function (test) {
    test.expect(2);

    // Grab the stats on the file
    var file = __dirname + '/actual/template_zip/grunt-zip.zip';
    fs.stat(file, function (err, stat) {
      // Assert there is no error
      test.equal(err, null, 'There was no error during `stat`');

      // and we are looking at a file
      test.ok(stat.isFile, 'The templated zip file was not successfully created');

      // Callback
      test.done();
    });
  }
};
