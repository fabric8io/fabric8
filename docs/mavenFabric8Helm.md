## fabric8:helm

The maven `fabric8:helm` goal generates a [Helm Chart](http://helm.sh/) for your application so that your application can be easily installed, updated or uninstalled via [Helm](http://helm.sh/).

For a summary of the options see the [Maven Property Reference](#maven-properties)
### Example usage

In a maven project type:

    mvn fabric8:helm

The console will then output the location of the created Helm chart
 
If you have a multi-maven project the chart repository is usually in `target/helm-repo` of the root project. So that if you run this command in the root project:

    mvn fabric8:helm

Then all your charts will be added to the helm repository.

Note that this commmand doesn't perform any commit or push by default. You can then choose when to commit and push in your CI / CD process when all the images are pushed and so forth via the [mvn fabric8:helm-push](mavenFabric8HelmPush.html) command

### Maven Properties

You can supply the following maven properties to the goal via system properties:

    mvn fabric8:helm -Dfabric8.helm.gitUrl=https://github.com/myusername/charts.git
  
The following properties can be used:
  
<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>fabric8.helm.chart</td>
<td>Specify the name of the chart to create.</td>
</tr>
<tr>
<td>fabric8.helm.gitUrl</td>
<td>Specify the git URL of the helm chart repository to clone, commit to and push.</td>
</tr>
<tr>
<td>fabric8.helm.cloneDir</td>
<td>Location on disk where the helm chars repo should be cloned to</td>
</tr>
<tr>
<td>fabric8.helm.privateKeyPath</td>
<td>Specify the location of a specific private key for SSH connections to github</td>
</tr>
<tr>
<td>fabric8.helm.privateKeyPassphrase</td>
<td>Specify the passphrase for a specific SSH private key (can be blank if there is no passphrase)</td>
</tr>
</table>

