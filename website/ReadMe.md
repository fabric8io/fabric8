## fabric8 website

This maven project builds the fabric8 website content. Its mostly consists of markdown files you can view/edit directly or via github.

If you want to build the HTML yourself try:

    mvn install jetty:run


The website can be accessed from a web browser using the following url:

    http://localhost:8000


To generate the static html try:

    mvn scalate:sitegen
