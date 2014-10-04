FROM fabric8/fabric8

ADD fabric8.zip /home/fabric8/fabric8.zip

# lets zap the old install
WORKDIR /home/fabric8
RUN rm -rf fabric8-karaf
RUN unzip -q fabric8.zip
RUN mv fabric8-karaf-${project.version} fabric8-karaf
RUN rm fabric8.zip

WORKDIR /home/fabric8/fabric8-karaf/etc

# the following section is copy/pasted from the fabric8/fabric8 Dockerfile
# TODO we maybe want to wrap this up in a shared script inside the distro

# lets remove the karaf.name by default so we can default it from env vars
RUN sed -i '/karaf.name=root/d' system.properties
RUN sed -i '/runtime.id=/d' system.properties

RUN echo bind.address=0.0.0.0 >> system.properties
RUN echo fabric.environment=docker >> system.properties
RUN echo zookeeper.password.encode=true >> system.properties

# lets remove the karaf.delay.console=true to disable the progress bar
RUN sed -i '/karaf.delay.console=true/d' config.properties
RUN echo karaf.delay.console=false >> config.properties

# lets enable logging to standard out
RUN echo log4j.rootLogger=INFO, stdout, osgi:* >> org.ops4j.pax.logging.cfg

WORKDIR /home/fabric8


CMD /home/fabric8/startup.sh

