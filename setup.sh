#!/bin/bash -e
if ! sudo -n ls >/dev/null 2>/dev/null ; then
   echo -n "Root " 
   su -c "mkdir -p /etc/sudoers.d/; echo 'jenkins ALL=(ALL) NOPASSWD: ALL' | tee -a /etc/sudoers.d/visudo"
fi

if [ -z "$HOST" ] ; then
	echo Please export HOST as the fully qualified domain name
	echo export HOST=foo.mvista.com
	exit 1
fi
chmod 755 ~
REGISTRY=jptest01.mvista.com:5001
KOJI_CONTAINER="koji-docker:latest"
KOJI_CONFIG=/koji/config
KOJI_LOGS=/koji/journal
KOJI_OUTPUT=/srv
JENKINS_CONTAINER="jenkins-mv:latest"
JENKINS_HOME=/jenkins

sudo systemctl enable --now docker
sudo mkdir -p /etc/docker/certs.d/$REGISTRY
sudo cp certs/ca.crt /etc/docker/certs.d/$REGISTRY/ca.crt
sudo docker pull $REGISTRY/$KOJI_CONTAINER
sudo mkdir -p $KOJI_OUTPUT $KOJI_LOGS $KOJI_CONFIG
if [ -z "$(sudo docker ps --all --filter "name=koji-docker" | grep -v ^CONTAINER)" ] ; then
	sudo docker run \
		--name koji-docker \
		--cap-add=SYS_ADMIN  \
		--tmpfs /tmp \
		--tmpfs /run \
		-p 80:80 \
		-p 443:443 \
		-v /srv:/srv \
		-v /koji/config:/config \
		-v /koji/journal:/var/log/journal \
		-v /sys/fs/cgroup:/sys/fs/cgroup:ro \
		-e "HOSTNAME=$HOST" \
		-t $REGISTRY/$KOJI_CONTAINER
else
	sudo docker stop koji-docker
	sudo docker start koji-docker
fi
sudo docker pull $REGISTRY/$JENKINS_CONTAINER
sudo mkdir -p $JENKINS_OUTPUT
echo -n Waiting for koji to start up:
while [ ! -e /koji/config/.done ] ; do
	echo -n "."
	sleep 3
done
sudo mkdir -p $JENKINS_HOME/.koji
sudo cp -a $KOJI_CONFIG/user/* $JENKINS_HOME/.koji/
sudo cp $KOJI_CONFIG/koji/app.list jenkins/init/* $JENKINS_HOME
sudo chown 1000.1000 -R $JENKINS_HOME
if [ -z "$(sudo docker ps --all --filter "name=koji-jenkins" | grep -v ^CONTAINER)" ] ; then
	sudo docker run -it \
		--name koji-jenkins \
	        --ulimit nofile=122880:122880 \
		-p 8080:8080 \
		-p 50000:50000 \
		--env JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
		-v $KOJI_CONFIG/user/:/usr/local/share/ca-certificates/extra/ \
		-v $JENKINS_HOME:/var/jenkins_home \
		-t $REGISTRY/$JENKINS_CONTAINER
else
	sudo docker stop koji-jenkins
	sudo docker start -i koji-jenkins
fi


exit 1
bash ./build-rpm.sh
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
bash ./setup-jenkins.sh
