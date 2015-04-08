/**
 * Script to generate redirect html files for old pages that move
 * as github pages doesn't support .htaccess
 */

var fs = require('fs');

var dir = '../../fabric8-ghpages/v2';
var newpath = "/guide/";

function endsWith(str, suffix) {
  return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

fs.readdir(dir, function (err, files) {
  if (err) throw err;
  files.forEach(function (file) {
    if (endsWith(file, ".html")) {
      var path = dir + "/" + file;

      var body = '<!DOCTYPE html>\n' +
              '<html>\n' +
              '  <head>\n' +
              '    <meta http-equiv="content-type" content="text/html; charset=utf-8" />\n' +
              '    <meta http-equiv="refresh" content="0;url=' + newpath + file + '" />\n' +
              '  </head>\n' +
              '</html>\n';
      fs.writeFile(path, body, function (err) {
        if (err) throw err;
        console.log("written file : " + path);
      });
    }
  });
});
