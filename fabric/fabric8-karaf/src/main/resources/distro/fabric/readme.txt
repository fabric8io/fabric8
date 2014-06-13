Note that the import directory just contains the initial import data used when creating a new fabric.

Once a fabric has been created this directory is completely ignored!

To import your custom profiles, you can drop in .zip files with the profiles.
For example use the fabric8:zip plugin to generate the .zip files.

Instead of dropping in the .zip files in this directory, you can create a .properties file,
and define the url's for the .zip files. For mvn coordinates, then they usually of the form mvn:groupId/artifactId/version/zip/profile

For an example see the quickstarts.properties which is provided out of the box.
