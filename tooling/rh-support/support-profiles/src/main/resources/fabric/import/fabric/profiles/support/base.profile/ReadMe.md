Support tools for Karaf and Fabric

a `support-base` profile is defined in Fabric.  
To enable it:
```
container-add-profile root support-base
```

A companion Hawtio plugin exists, to add an entry in the menus. That plugin can currently be installed in form of a bundle
```
install -s mvn:io.hawt/hawtio-plugin-redhat-access/1.5-SNAPSHOT/war
```
    
 