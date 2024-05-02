# -buster is required to have apt available
FROM openjdk:17-slim-buster

# Optional JVM arguments, such as memory settings
ARG JVM_ARGS=""

# Install curl, then delete apt indexes to save image space
RUN apt update \
    && apt install -y curl \
    && rm -rf /var/cache/apt/archives /var/lib/apt/lists

WORKDIR /app

COPY ./build/libs/connector.jar /app

EXPOSE 29191
EXPOSE 29193
EXPOSE 29194
EXPOSE 29192

# health status is determined by the availability of the /health endpoint => not working, wrong url
#HEALTHCHECK --interval=5s --timeout=5s --retries=10 CMD curl -H "X-Api-Key: $EDC_API_AUTH_KEY" --fail http://localhost:29191/api/check/health

ENV WEB_HTTP_PORT="29191"
ENV WEB_HTTP_PATH="/api"
ENV WEB_HTTP_CONTROL_PORT="29192"
ENV WEB_HTTP_CONTROL_PATH="/control"
ENV WEB_HTTP_MANAGEMENT_PORT="29193"
ENV WEB_HTTP_MANAGEMENT_PATH="/management"
ENV WEB_HTTP_PROTOCOL_PORT="29194"
ENV WEB_HTTP_PROTOCOL_PATH="/dsp"
# zusätzlich bei uns: public port - NICHT NÖTIG?
#ENV WEB_HTTP_PUBLIC_PORT="29291"
#ENV WEB_HTTP_PUBLIC_PATH="/public"

# Use "exec" for graceful termination (SIGINT) to reach JVM.
# ARG can not be used in ENTRYPOINT so storing values in ENV variables
ENV JVM_ARGS=$JVM_ARGS
ENTRYPOINT [ "sh", "-c", \
    "exec java $JVM_ARGS -jar connector.jar"]
