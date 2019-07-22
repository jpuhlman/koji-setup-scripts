#!/bin/bash -x

pushd ~
sudo swupd bundle-add os-clr-on-clr
curl -O https://raw.githubusercontent.com/clearlinux/common/master/user-setup.sh
chmod +x user-setup.sh
./user-setup.sh
git config --global user.email "jenkins@mvista.com"
git config --global user.name "Jenkins"
pwd
cd clearlinux
pwd
make clone_rpm PKG_BASE_URL=git://gitcentos.mvista.com/centos/upstream/utils
pwd
cd packages/rpm
pwd
make build
sudo rpm -ihv --force --nodeps rpms/*.rpm
