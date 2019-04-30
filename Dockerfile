FROM openjdk:8-jdk-alpine
COPY ./target/banenor-0.0.1-SNAPSHOT.jar /opt/banenor-0.0.1-SNAPSHOT.jar

# RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf 

ENTRYPOINT ["/usr/bin/java"]
CMD ["-jar", "/opt/banenor-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080:8080