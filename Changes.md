## Change Log

### 1.1.0.x

* the script to start a container is now **bin/fabric8** and by default it will create a fabric unless you explicitly configure one of the [fabric8 environment variables]()
* added new maven plugin goals (fabric8:agregate-zip, fabric8:zip, fabric8:branch)
* fully working [docker support](http://fabric8.io/#/site/book/doc/index.md?chapter=docker_md) so using the docker profile you can create containers using docker.

### 1.0.0.x

* first community release integrated into the JBoss Fuse 6.1 product
* includes [hawtio](http://hawt.io/] as the console
* uses git for configuration; so all changes are audited and versioned and its easy to revert changes.