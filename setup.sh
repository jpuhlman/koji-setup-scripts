#!/bin/bash
echo -n "Root " 
su -c "mkdir /etc/sudoers.d/; echo 'jenkins ALL=(ALL) NOPASSWD: ALL' | tee -a /etc/sudoers.d/visudo"

sudo ./koji-setup/deploy-koji.sh
sudo ./koji-setup/bootstrap-build.sh
sudo -u kojiadmin koji moshimoshi
source ./koji-setup/parameters.sh

pushd /etc/pki/koji/
sudo ./gencert.sh jenkins "/C=$COUNTRY_CODE/ST=$STATE/L=$LOCATION/O=$ORGANIZATION/OU=$ORG_UNIT/CN=jenkins"
sudo cp /home/kojiadmin/.koji/config /tmp/config-koji
popd

mkdir -p ~/.koji && cd $_
cp /etc/pki/koji/jenkins.pem .
cp -v clear.pem client.crt
cp /etc/pki/koji/koji_ca_cert.crt .
cp -v koji_ca_cert.crt clientca.crt
cp -v koji_ca_cert.crt serverca.crt
/tmp/config-koji ./config
sudo mkdir -p /etc/ca-certs/trusted
sudo cp serverca.crt /etc/ca-certs/trusted/
sudo clrtrust generate
koji moshimoshi

pushd ~/koji-setup-scripts/
sudo ./koji-setup/deploy-mash.sh
systemctl status mash

exit 1
sudo docker build jenkins-mv -t jenkins-mv:latest
popd
mkdir -p jenkins
cp -a .koji jenkins/
sudo mkdir -p /srv/jenkins
