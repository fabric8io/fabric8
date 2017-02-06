## Upgrading Fabric8

As of version `2.4.24` of the `fabric8 platform` we now release the necessary metadata for `gofabric8` to know what package(s) are installed, to list their available versions and how to upgrade/downgrade them.

To list the packages and versions you have installed type:

```sh
gofabric8 packages
```

For a specific package, such as `fabric8-platform` to find all the available versions type:

```sh
gofabric8 package-versions fabric8-platform
```

Then to perform an upgrade to the latest version type:

```sh
gofabric8 upgrade --all
```

Which will iterate over all packages, check if there's a newer version available and if so upgrade to it.

You can upgrade a specific package by adding its name as a command line argument like this:

```sh
gofabric8 upgrade fabric8-platform
```

You can also go back to an older version using the `--version` argument:

```sh
gofabric8 upgrade --version=2.4.24 fabric8-platform
```

The `gofabric8` command will check for `Package` resources (which are `ConfigMap` with a label `fabric8.io/kind=package`) which have the version on a label and have the metadata to be able to find the available versions and download the YAML files.

If you have an older version of fabric8 platform installed before version `2.4.24` then to upgrade you need to specify the name of the package to upgrade:

```sh
gofabric8 upgrade fabric8-platform
```

This will then ugprade to the latest version and install the necessary `Package` metadata so that `gofabric8` is now aware of what version of the package is installed to smooth the upgrade/downgrade process.
