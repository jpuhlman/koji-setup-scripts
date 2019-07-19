FROM jenkins/jenkins:lts
USER root
RUN mkdir -p /usr/local/share/ca-certificates/extra
COPY ./serverca.crt /usr/local/share/ca-certificates/extra/
RUN update-ca-certificates
RUN apt-get update 
RUN apt-get install -y koji-client
RUN apt-get install -y python-krbv
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y locales \
    && sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
    && dpkg-reconfigure --frontend=noninteractive locales \
    && update-locale LANG=en_US.UTF-8
ENV LANG en_US.UTF-8 
ENV LC_ALL en_US.UTF-8 
USER jenkins
