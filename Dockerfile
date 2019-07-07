FROM openjdk:8-jdk-alpine AS builder

# Install Maven
ENV MAVEN_VERSION 3.6.1
ENV MAVEN_HOME /usr/lib/mvn
ENV PATH $MAVEN_HOME/bin:$PATH
RUN wget http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  tar -zxvf apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  rm apache-maven-$MAVEN_VERSION-bin.tar.gz && \
  mv apache-maven-$MAVEN_VERSION /usr/lib/mvn

# Build app
RUN mkdir /app
WORKDIR /app
COPY . .
RUN ["mvn", "install"]

FROM openjdk:8-jdk-alpine
#COPY ./target/oscar-downloader-0.1.jar ./app.jar
COPY --from=builder ./app/target/oscar-downloader-0.1.jar ./app.jar

ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
