#!/bin/bash

sudo swupd bundle-add cloud-control
sudo systemctl start docker
cp ~/.koji/serverca.crt jenkins-mv/
sudo docker build jenkins-mv -t jenkins-mv:latest
sudo mkdir -p /srv/jenkins
sudo chown jenkins /srv/jenkins/
cp -a ~/.koji /srv/jenkins
cp -a jenkins/app.list jenkins/init/* /srv/jenkins
sudo cp jenkins/jenkins.service /etc/

