FROM fabric8/fabric8

USER root

# lets zap the old install
WORKDIR /opt

RUN rm -rf fabric8 fabric8-karaf*
ADD fabric8.tar.gz /opt

RUN mv fabric8-karaf-${project.version} fabric8

WORKDIR /opt/fabric8/etc

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

RUN chown -R fabric8 /opt/fabric8
RUN chgrp -R fabric8 /opt/fabric8

WORKDIR /opt/fabric8
#USER fabric8

CMD /home/fabric8/startup.sh

