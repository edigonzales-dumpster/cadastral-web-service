FROM adoptopenjdk/openjdk11:latest

USER root

RUN apt-get update && apt-get install -y --no-install-recommends libfontconfig1 && rm -rf /var/lib/apt/lists/*

WORKDIR /home/cadastralinfoservice
COPY build/libs/*.jar /home/cadastralinfoservice/cadastral-info-service.jar
RUN cd /home/cadastralinfoservice && \
    chown -R 1001:0 /home/cadastralinfoservice && \
    chmod -R g+rw /home/cadastralinfoservice && \
    ls -la /home/cadastralinfoservice

USER 1001
EXPOSE 8080
CMD java -jar cadastral-info-service.jar 
