FROM openjdk:18-jdk-oracle

RUN mkdir /app

COPY ./target/hlf-connector.jar /app

WORKDIR /app


RUN mkdir -p /usr/local/config

RUN groupadd -r appGrp -g 10001  \
    && useradd -u 10000 -r -g appGrp -m -d /opt/app/ -s /sbin/nologin -c "appGrp user" appGrp \
    && chown -R 10000:10001 /usr/local/config

USER 10000

ENV spring_config_location=file:///usr/local/config/application.yml
ENV JAVA_OPTS="$JAVA_OPTS -Xms1024m -Xmx4096m -Dspring.config.location=${spring_config_location}"

ENTRYPOINT java -jar hlf-connector.jar $JAVA_OPTS
