## Offline Repositories

Its quite common for folks to want to create offline repositories; either as a local cache of remote maven repositories, or in cases where production machines do not have access to the internet.

As of Fuse 6.1 build 127, we've added support for this via this issue: https://github.com/jboss-fuse/fuse/issues/140

The **profile-download** command allows you to download all the maven artifacts for the bundles and features in your Fabric profiles.

### For a profile

To download all the bundles and features for a given profile type this into the Fuse shell after you've created a fabric:

     profile-download --profile jboss-fuse-full /tmp/myrepo

This will download all the bundles, FABs and features for the default version of the given profile and download them into the /tmp/myrepo directory.

If you omit a path then it installs into the **system folder** inside the current Fuse container (thereby populating the local maven repo for the container).

     profile-download --profile jboss-fuse-full

### For a version

To download all the bundles and features for all the profiles in the default version type this into a Fuse shell after you've created a fabric:

     profile-download /tmp/myrepo

You can specify a specific version to download via **--version**.

     profile-download --version 1.0 /tmp/myrepo

If you omit a path then it installs into the **system folder** inside the current Fuse container (thereby populating the local maven repo for the container).


By default files are not downloaded again if they already exist; but you can supply **--force** to force a redownload again.
