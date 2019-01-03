#!/bin/bash
# Copyright (C) 2018 Intel Corporation
# SPDX-License-Identifier: Apache-2.0

set -xe
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
source "$SCRIPT_DIR"/parameters.sh

## NFS SHARING FOR KOJID ON SEPARATE MACHINES
if [[ "$KOJI_SLAVE_FQDN" != "$KOJI_MASTER_FQDN" ]]; then
	yum -y install nfs-utils
	echo "$KOJI_DIR $KOJI_SLAVE_FQDN(ro)" > /etc/exports
	systemctl enable --now rpcbind
	systemctl enable --now nfs-server
	systemctl enable --now nfs-lock
	systemctl enable --now nfs-idmap
fi
