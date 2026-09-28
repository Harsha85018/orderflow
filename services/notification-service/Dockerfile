FROM eclipse-temurin:21-jre

RUN useradd --system --uid 1001 app
WORKDIR /app
COPY target/*.jar app.jar
USER app

# Heap follows the container's memory limit set by Kubernetes.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
