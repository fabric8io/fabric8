# Example QuickStart: Content Based Router

Runs the Camel Content Based Router quickstart example

## Trying this quick start

<ol ng-init="profile = 'example-quickstarts-cbr'">
  <div fabric-containers="containers" profile="{{profile}}">
    <li>
      <a class="btn" href="#/fabric/containers/createContainer?profileIds={{profile}}"><i class="icon-plus"></i> Create a container for this profile</a>
    </li>
    <li>
      Once you have created a container you can see its information, view routes or send messages below:
    	<ul>
    	  <li ng-repeat="container in containers">
          <span fabric-container-link="{{container}}"/>
        
          <!-- TODO remove the -stracmac.local host name crap when Claus's fix is in
           for https://issues.apache.org/jira/browse/CAMEL-6938 -->
    	    <button class="btn" fabric-container-connect="{{container}}" 
            view="/camel/routes?tab=notree&amp;nid=root-org.apache.camel-stracmac.local%2F99-cbr-example-context-routes-%22cbr-route%22">
            <i class="icon-picture"></i> Diagram
          </button>
    	    <button class="btn" fabric-container-connect="{{container}}"
                  view="/camel/sendMessage?tab=camel&amp;p=container&amp;nid=root-org.apache.camel-stracmac.local%2F99-cbr-example-context-endpoints-%22file:%2F%2Fwork%2Fcbr%2Finput%22&amp;subtab=choose&amp;q=data%2F">
            <i class="icon-share-alt"></i> Send Sample Messages
          </button>
    	  </li>
      </ul>
	  </li>
  </div>
</ol>

