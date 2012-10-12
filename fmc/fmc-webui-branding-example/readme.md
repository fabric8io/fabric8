This is an example of how a fragment bundle can be created to replace
resources in the main fmc-webui war file.  Build the example with a
simple:

mvn clean install

Then install the fragment into your container:

osgi:install mvn:org.fusesource.fmc/fmc-webui-branding-bundle/<version>

replace <version> as necessary.

Once the fragment is installed, refresh the "FMC :: Console Frontend"
bundle.  If all is good an osgi:list should look something like:

[ 139] [Active     ] [Created     ] [   80] FMC :: Console Frontend (99.0.0.master-SNAPSHOT)
                                       Fragments: 140
[ 140] [Resolved   ] [            ] [   80] FMC :: Example Custom Logo Fragment (1.0)
                                       Hosts: 139

And if you go to the FMC webapp you should see the FuseSource logo replaced
with "ACME Inc.".

