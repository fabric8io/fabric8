#!/bin/bash
echo "Running the ArchetypeTest; piping output to mvn.log"
mvn clean install -Pts.archetype -DArchetypeTest.artifactId=$* > mvn.log &
tail -f mvn.log
