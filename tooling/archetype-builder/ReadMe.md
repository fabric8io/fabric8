## Archetype Builder

This code helps create archetypes from working projects without having to warp your project to make the archetype plugin happy.

It does this by keeping the [example projects](https://github.com/fusesource/fuse/tree/master/tooling/examples) as stand alone examples folks can use, test and maintain directly; then it auto-generates the source of the [archetype projects](https://github.com/fusesource/fuse/tree/master/tooling/archetypes) via a build step.

To simplify things, the archetype projects have their own hand crafted root pom.xml files and files in src/main/resources-filtered are maintained by hand.

However the tool automatically copies the example projects source code to [src/main/resources/archetype-resources](https://github.com/fusesource/fuse/tree/master/tooling/archetypes/camel-drools-archetype/src/main/resources).

So the directory [src/main/resources/archetype-resource](https://github.com/fusesource/fuse/tree/master/tooling/archetypes/camel-drools-archetype/src/main/resources) is included in the [.gitignore](https://github.com/fusesource/fuse/blob/master/tooling/archetypes/camel-drools-archetype/src/main/resources/.gitignore) so its never checked in.

This means you can tweak whats included/excluded from the archetype and change the archetype pom.xml; but keep the archetype data in sync with the working example program.
