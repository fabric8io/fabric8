## Archetype Builder

This code helps create archetypes from working projects without having to warp your project to make the archetype plugin happy.

It does this by keeping the [example projects](https://github.com/fusesource/fuse/tree/master/tooling/examples) as stand alone examples folks can use, test and maintain directly; then it auto-generates the source of the [archetype projects](https://github.com/fusesource/fuse/tree/master/tooling/archetypes) via a build step.

To simplify things, the archetype projects have their own hand crafted root pom.xml files and files in src/main/resources-filtered are maintained by hand.

However the tool automatically copies the example projects source code to src/main/resources/archetype-resources.

So the directory *src/main/resources/archetype-resource* is included in the .gitignore so its never checked in.