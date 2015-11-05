var grunt = require('grunt'),
    fs = require('fs'),
    _ = require('underscore.string');

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

function addMethods(test) {
  // Assert two files are equal
  test.equalFiles = function (filename) {
    // Read in content
    var expectedContent = fs.readFileSync('expected/' + filename, 'binary'),
        actualContent = fs.readFileSync('actual/' + filename, 'binary');

    // Assert that the content is *exactly* the same
    test.strictEqual(actualContent, expectedContent, filename + 'does not have the same content in `expected` as `actual`');
  };

  // Assert two files are close enough
  // ANTI-PATTERN: 3 specifically ordered/non-modular parameters =(
  test.closeFiles = function (filename, distance) {
    // Read in the content
    var expectedContent = fs.readFileSync('expected/' + filename, 'binary'),
        actualContent = fs.readFileSync('actual/' + filename, 'binary');

    // Calculate the difference in bits (accounts for random bits)
    var difference = _.levenshtein(expectedContent, actualContent);

    // Assert that we are under our threshold
    var underThreshold = difference <= distance;
    test.ok(underThreshold, 'Bitwise difference of zip files "' + difference + '" should be under ' + distance + ' (' + filename + ')');
  };
}

exports['zip'] = {
  'singleZip': function (test) {
    // Set up
    test.expect(1);
    addMethods(test);

    // Assert single_zip is close enough and return
    test.closeFiles('single_zip/file.zip', 15);
    test.done();
  },
  'multiZip': function (test) {
    // Set up
    test.expect(1);
    addMethods(test);

    // Assert single_zip is close enough and return
    test.closeFiles('multi_zip/file.zip', 30);
    test.done();
  },
  'singleUnzip': function (test) {
    // Add in test methods
    test.expect(2);
    addMethods(test);

    // Compare a and b
    test.equalFiles('single_unzip/a.js');
    test.equalFiles('single_unzip/b.js');

    // Return
    test.done();
  },
  'nestedUnzip': function (test) {
    test.expect(8);
    addMethods(test);

    // Compare all nested unzip files
    test.equalFiles('nested_unzip/bootstrap/css/bootstrap-responsive.css');
    test.equalFiles('nested_unzip/bootstrap/css/bootstrap-responsive.min.css');
    test.equalFiles('nested_unzip/bootstrap/css/bootstrap.css');
    test.equalFiles('nested_unzip/bootstrap/css/bootstrap.min.css');
    test.equalFiles('nested_unzip/bootstrap/img/glyphicons-halflings-white.png');
    test.equalFiles('nested_unzip/bootstrap/img/glyphicons-halflings.png');
    test.equalFiles('nested_unzip/bootstrap/js/bootstrap.js');
    test.equalFiles('nested_unzip/bootstrap/js/bootstrap.min.js');

    test.done();
  },
  'image': function (test) {
    // Set up
    test.expect(1);
    addMethods(test);

    // Assert the image is the same as when it went in
    test.equalFiles('image_zip/unzip/test_files/smile.gif');
    test.done();
  },
  'nestedZip': function (test) {
    // Set up
    test.expect(5);
    addMethods(test);

    // Assert all files are the same as they went in
    test.equalFiles('nested_zip/unzip/test_files/nested/hello.js');
    test.equalFiles('nested_zip/unzip/test_files/nested/world.txt');
    test.equalFiles('nested_zip/unzip/test_files/nested/glyphicons-halflings.png');
    test.equalFiles('nested_zip/unzip/test_files/nested/nested2/hello10.txt');
    test.equalFiles('nested_zip/unzip/test_files/nested/nested2/hello20.js');

    // Return
    test.done();
  },
  'routerZip': function (test) {
    // Set up
    test.expect(2);
    addMethods(test);

    // Assert all files are the same as they went in
    test.equalFiles('router_zip/unzip/hello.js');
    test.equalFiles('router_zip/unzip/hello10.txt');

    // Return
    test.done();
  },
  'routerUnzip': function (test) {
    test.expect(8);
    addMethods(test);

    // Compare all router unzip files
    test.equalFiles('router_unzip/bootstrap-responsive.css');
    test.equalFiles('router_unzip/bootstrap-responsive.min.css');
    test.equalFiles('router_unzip/bootstrap.css');
    test.equalFiles('router_unzip/bootstrap.min.css');
    test.equalFiles('router_unzip/glyphicons-halflings-white.png');
    test.equalFiles('router_unzip/glyphicons-halflings.png');
    test.equalFiles('router_unzip/bootstrap.js');
    test.equalFiles('router_unzip/bootstrap.min.js');

    test.done();
  },
  'cwdZip': function (test) {
    // Set up
    test.expect(2);
    addMethods(test);

    // Assert all files are the same as they went in
    test.equalFiles('cwd_zip/unzip/hello.js');
    test.equalFiles('cwd_zip/unzip/nested2/hello10.txt');

    // Return
    test.done();
  },
  'dotZip': function (test) {
    // Set up
    test.expect(2);
    addMethods(test);

    // Assert all files are the same as they went in
    test.equalFiles('dot_zip/unzip/test_files/dot/.test/hello.js');
    test.equalFiles('dot_zip/unzip/test_files/dot/test/.examplerc');

    // Return
    test.done();
  }
};
