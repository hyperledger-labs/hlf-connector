JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=file:///usr/local/config/application.yml"
export JAVA_OPTS

export CATALINA_OPTS="$CATALINA_OPTS -Xms1024m"
export CATALINA_OPTS="$CATALINA_OPTS -Xmx8192m"

