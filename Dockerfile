FROM openjdk:8-jdk-alpine
COPY ./target/banenor-0.0.1-SNAPSHOT.jar /opt/banenor-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["/usr/bin/java"]
CMD ["-jar", "/opt/banenor-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080:8080