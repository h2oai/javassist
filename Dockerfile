FROM openjdk:11.0.10-jdk

RUN apt-get update -q -y && \
    apt-get dist-upgrade -y  && \
    apt-get install maven -y && \
    apt-get clean && \
    rm -rf /var/cache/apt/*

RUN useradd -m -u 2117 jenkins

USER jenkins