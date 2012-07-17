Launcher script for java applications, compatible with:

http://refspecs.freestandards.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html

Assumes the following layout:

/bin/<scripts>
/lib/main.jar
/etc/config.properties
/etc/jvm.properties
/README.txt

The application executable code needs to be packaged into a single executable main.jar and placed in the lib directory of the install location. The jar needs to be self-contained or include a class-path directive with the location of dependent jar files.

This artifact can be used in combination with the assembly-descriptor project to obtain the desired layout. These are the steps required:

1. Add the following dependency to the project pom.xml:

<dependency>
    <groupId>org.fusesource.process</groupId>
    <artifactId>process-launcher</artifactId>
    <version>{version}</version>
    <classifier>bin</classifier>
</dependency>

2. Add the following plugins to the project pom.xml:

<plugin>
   <artifactId>maven-assembly-plugin</artifactId>
   <version>2.2-beta-5</version>
   <configuration>
      <descriptorRefs>
         <descriptorRef>distribution</descriptorRef>
      </descriptorRefs>
   </configuration>
   <dependencies>
      <dependency>
         <groupId>com.proofpoint</groupId>
         <artifactId>distribution-assembly-descriptor</artifactId>
         <version>{version}</version>
      </dependency>
   </dependencies>

   <executions>
      <execution>
         <id>package</id>
         <phase>package</phase>
         <goals>
            <goal>single</goal>
         </goals>
      </execution>
   </executions>
</plugin>


<plugin>
   <groupId>org.apache.maven.plugins</groupId>
   <artifactId>maven-jar-plugin</artifactId>
   <configuration>
      <archive>
         <manifest>
            <mainClass>...</mainClass>   
            <addClasspath>true</addClasspath>
         </manifest>
      </archive>
   </configuration>
</plugin>

Don't forget to fill in the fully qualified name of the main class.
