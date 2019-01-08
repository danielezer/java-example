FROM openjdk:8u181-jre-alpine3.8

COPY target/downloads/com/mkyong/hashing/java-project/*-SNAPSHOT/java-project-*.jar /opt/dckr-app/

WORKDIR /opt/dckr-app/

CMD ["java", "-jar", "java-project-*.jar"]