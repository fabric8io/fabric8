# Example QuickStart: Content Based Router - Wiki

Runs the Camel Content Based Router quickstart example where the camel route is stored in the wiki so its easy to edit it via the Management Console

## Trying this quick start

<div fabric-containers="containers" profile="{{profileId}}">
  <li>
    <a class="btn" href="#/fabric/containers/createContainer?profileIds={{profileId}}"><i class="icon-plus"></i> Create a container for this profile</a>
  </li>
  <li>
    Containers for this profile:
    <ul>
      <li ng-repeat="container in containers">
        <span fabric-container-link="{{container}}"/>

        <button class="btn" fabric-container-connect="{{container}}"
        view="/camel/routes?tab=camel&amp;nid=root-org.apache.camel-cbr.xml-routes-%22cbr-route%22">
          <i class="icon-picture"></i> Diagram
        </button>

        <button class="btn" fabric-container-connect="{{container}}"
          view="/camel/sendMessage?tab=camel&amp;nid=root-org.apache.camel-cbr.xml-endpoints-%22file:%2F%2Fwork%2Fcbr%2Finput%22&amp;subtab=choose&amp;q=data%2F">
          <i class="icon-share-alt"></i> Send Sample Messages
        </button>
      </li>
    </ul>
  </li>
</div>
