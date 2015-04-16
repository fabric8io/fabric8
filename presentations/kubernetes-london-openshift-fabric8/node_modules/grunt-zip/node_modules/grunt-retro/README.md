# grunt-retro

Add grunt@0.4 functionality to grunt@0.3 plugins

## Getting Started
Install this grunt plugin next to your project's [grunt.js gruntfile][getting_started] with: `npm install grunt-retro`

Then inside your grunt plugin or grunt.js (depending on the scope of your intent), load and bind `grunt-retro` to `grunt`:

```javascript
// Inside of grunt plugin (MUST BE DONE IMMEDIATELY AFTER module.exports)
module.exports = function (grunt) {
  grunt = require('grunt-retro')(grunt);
  
  // Continue with normal actions
};
```

[grunt]: http://gruntjs.com/
[getting_started]: https://github.com/gruntjs/grunt/blob/master/docs/getting_started.md

## Documentation
`grunt-retro` takes care of the gotchas between `grunt@0.3` and `grunt@0.4`. Below is a list of what we guarantee to work:

- Define `this.file` to match the `0.3` specification; `{src, file}`
- Fallback `this.utils` to be an object of `grunt` utilites
- Allow for `registerTask` to alias multiple tasks via single string

```js
grunt.registerTask('multi-task', 'lint clean test');
```

- Add back `grunt.helper` and `grunt.registerHelper` system

### Nuances
- Plugins will receive the same `array` or `non-array` input from `this.file.src`
    - In `0.4`, this is normalized to always be an `array`.
- Fallback `grunt.file.glob.minimatch`

## Examples
### grunt plugin

```js
module.exports = function (grunt) {
  // Bind retro functionality
  grunt = require('grunt-retro')(grunt);
  
  // Create and register our task
  grunt.registerTask('rot13', 'Rotate a file by 13', function () {
    // Grab file locations -- normally, we would expandFiles(src)
    var file = this.file,
        src = file.src,
        dest = file.dest;
        
    // Rotate content
    var input = grunt.file.read(src),
        output = grunt.helper('rot13', input);
        
    // Write out content
    grunt.file.write(dest, output);
  });
  
  // Register a helper for performing rot13
  grunt.registerHelper('rot13', rot13Fn);
};
```

### grunt.js
```js
module.exports = function (grunt) {
  // Bind retro functionality
  grunt = require('grunt-retro')(grunt);
  
  // Set up config
  grunt.initConfig({...});
  
  // Register default task
  grunt.registerTask('default', 'lint test');
};
```

## Contributing
In lieu of a formal styleguide, take care to maintain the existing coding style. Add unit tests for any new or changed functionality. Lint your code using [grunt][grunt] and test via `npm test`.

## License
Copyright (c) 2013 Todd Wolfson
Licensed under the MIT license.
