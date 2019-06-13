# image with oracle jdk
FROM gcr.io/oscar-175906/base-images/oscar-downloader:v1
VOLUME /tmp
ADD target/oscar-downloader-0.0.1.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
