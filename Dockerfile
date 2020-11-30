FROM openjdk:8-jdk-alpine
COPY ./ ./
RUN apk add --no-cache maven
RUN mvn -Dmaven.test.skip clean package

# RUN echo 'hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4' >> /etc/nsswitch.conf 

ENTRYPOINT ["/usr/bin/java"]
CMD ["-jar", "./target/banenor-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080:8080
