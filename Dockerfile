FROM adoptopenjdk/openjdk11:latest

USER root

RUN apt-get update && apt-get install -y --no-install-recommends libfontconfig1 && rm -rf /var/lib/apt/lists/*

WORKDIR /home/cadastralwebservice
COPY build/libs/*.jar /home/cadastralwebservice/cadastral-web-service.jar
RUN cd /home/cadastralwebservice && \
    chown -R 1001:0 /home/cadastralwebservice && \
    chmod -R g+rw /home/cadastralwebservice && \
    ls -la /home/cadastralwebservice

USER 1001
EXPOSE 8080
CMD java -jar cadastral-web-service.jar 
