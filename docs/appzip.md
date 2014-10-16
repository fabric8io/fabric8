## App Zip

An _App Zip_ is a packaging format for [Apps](apps.html) to make them easier to distribute and share between environments.

Essentially an App Zip is just a standard ZIP file which includes one or more Apps, their JSON metadata, documentation and images.

### Using App Zips

You can drag and drop App Zips from the fabric8 console (in the **wiki** tab) to/from your desktop to make it really easy to install [Apps](apps.html) or share them.

Its also easy to host an App Zip on any website so its easy for others to download and install.

You can also use the [mvn fabric8:deploy](mavenPlugin.html#deploying) goal to deploy a locally built App Zip file into the wiki.

### App Zip Specification

* An App Zip is a ZIP file containing 1 or more Apps.
* An App is a folder containing a **kubernetes.json** file which maps to any of the kubernetes JSON blobs (pod, replication controller, service, config, template etc)
* An App can contain optional extra files for documentation, icons and metadata:

#### kubernetes.json

A JSON file which consists of Kubernetes JSON metadata. Typically this is either a Config (which is a collection of 1 or more core Kubernetes JSON abstractions; pod, replication contoller, service) or a Template (which is a parameterised Config that once the parameters are supplied generates a Config); though it could just be a pod, replication controller or service.

#### ReadMe.md / .html / .adoc

A page of documentation describing the App. Using the App Store metaphor think of this as the detailed description page when you click on an App in an App Store.

My using this naming convention; an App if stored on github is then easy to browse (the ReadMe file will be displayed when viewing an App folder on github or in fabric8's web console etc)

If the ReadMe requires additional documentation, images or media then put those files into a sub directory (ideally called **doc**) to leave the root folder uncluttered.

#### Summary.md / .html / .adoc

A short description; usually a line of text which is shown next to the icon when searching an App Store or viewing a Library.

#### icon.svg / .png / .jpeg

An icon for the App for displaying on an App Store or Library. We recommend SVG as this displays more faithfully on many different devices but if SVG isn't available its better to have some image than none.
