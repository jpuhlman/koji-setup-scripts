#!/bin/bash -e
if [ -z "$(sudo grep "^jenkins\ " /etc/sudoers.d/visudo 2>/dev/null)" ] ; then
   echo -n "Root " 
   su -c "mkdir -p /etc/sudoers.d/; echo 'jenkins ALL=(ALL) NOPASSWD: ALL' | tee -a /etc/sudoers.d/visudo"
fi
sudo -E ./koji-setup/deploy-koji.sh
sudo -E ./koji-setup/bootstrap-build.sh
sudo -u kojiadmin koji moshimoshi

source ./koji-setup/parameters.sh
pushd /etc/pki/koji/
sudo ./gencert.sh jenkins "/C=$COUNTRY_CODE/ST=$STATE/L=$LOCATION/O=$ORGANIZATION/OU=$ORG_UNIT/CN=jenkins"
sudo cp /home/kojiadmin/.koji/config /tmp/config-koji
popd

mkdir -p ~/.koji && pushd $_
cp /etc/pki/koji/jenkins.pem .
cp -v jenkins.pem client.crt
cp /etc/pki/koji/koji_ca_cert.crt .
cp -v koji_ca_cert.crt clientca.crt
cp -v koji_ca_cert.crt serverca.crt
cd ~/.koji
cp /tmp/config-koji ./config
sudo mkdir -p /etc/ca-certs/trusted
sudo cp serverca.crt /etc/ca-certs/trusted/
sudo clrtrust generate
koji moshimoshi
popd

sudo -E ./koji-setup/deploy-mash.sh
systemctl status mash
bash ./build-rpm

sudo docker build jenkins-mv -t jenkins-mv:latest

mkdir -p jenkins
cp -a .koji jenkins/
sudo mkdir -p /srv/jenkins
