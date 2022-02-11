FROM tomcat:9-jre8-alpine
RUN apk update && apk add --no-cache libc6-compat
RUN rm -rf /usr/local/tomcat/webapps/manager/
RUN rm -rf /usr/local/tomcat/webapps/docs/
RUN rm -rf /usr/local/tomcat/webapps/host-manager/
RUN rm -rf /usr/local/tomcat/webapps/examples/
RUN rm -rf /usr/local/tomcat/webapps/ROOT/*
RUN mkdir -p /usr/local/config



RUN addgroup -S -g 10001 appGrp \
    && adduser -S -D -u 10000 -s /sbin/nologin -h /opt/app/ -G appGrp app\
    && chown -R 10000:10001 /usr/local/tomcat && chown -R 10000:10001 /usr/local/config


USER 10000

COPY ./config/index.html /usr/local/tomcat/webapps/ROOT/

COPY ./config/setenv.sh /usr/local/tomcat/bin/

COPY ./target/hlf-connector.war /usr/local/tomcat/webapps/hlf-connector.war