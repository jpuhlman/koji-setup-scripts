#!/bin/bash
# Copyright (C) 2018 Intel Corporation
# SPDX-License-Identifier: Apache-2.0

set -xe
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
source "$SCRIPT_DIR"/parameters.sh

yum -y install mash
mkdir -p "$MASH_DIR"
chown -R kojiadmin:kojiadmin "$MASH_DIR"

usermod -a -G kojiadmin "$HTTPD_USER"
MASH_LINK="$HTTPD_DOCUMENT_ROOT"/"$(basename "$MASH_DIR")"
ln -sf "$MASH_DIR"/latest "$MASH_LINK"
chown -h kojiadmin:kojiadmin "$MASH_LINK"

mkdir -p /etc/mash
cat > /etc/mash/mash.conf <<- EOF
[defaults]
configdir = /etc/mash
buildhost = $KOJI_URL/kojihub
repodir = file://$KOJI_DIR
use_sqlite = True
use_repoview = False
EOF
cat > /etc/mash/clear.mash <<- EOF
[clear]
rpm_path = %(arch)s/os/Packages
repodata_path = %(arch)s/os/
source_path = source/SRPMS
debuginfo = True
multilib = False
multilib_method = devel
tag = dist-$TAG_NAME
inherit = True
strict_keys = False
arches = $RPM_ARCH
EOF

mkdir -p "$MASH_SCRIPT_DIR"
cp -f "$SCRIPT_DIR"/mash.sh "$MASH_SCRIPT_DIR"
mkdir -p /etc/systemd/system
cat > /etc/systemd/system/mash.service <<- EOF
[Unit]
Description=Mash script to loop local repository creation for local image builds

[Service]
User=kojiadmin
Group=kojiadmin
ExecStart=$MASH_SCRIPT_DIR/mash.sh
Restart=always
RestartSec=10s

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable --now mash