## Building

If you want to build this module quickly; just the key parts if you're working on the core of Fabric try this

    mvn clean install -Dtest=false -P!everything -Pdev

or on OS X you need to escape the !everything

    mvn -Dtest=false -DfailIfNoTests=false clean install -P \!everything,dev

