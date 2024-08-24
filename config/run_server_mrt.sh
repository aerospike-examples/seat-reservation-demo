#!/bin/bash
docker run --rm -tid -d -v `pwd`:/etc/aerospike/ -e "FEATURE_KEY_FILE=/etc/aerospike/features.conf" -e "LOGFILE=/var/log/aerospike/aerospike.log" --name aerospike_mrt -p 3000-3002:3000-3002 aerospike-server-enterprise-rc:latest

#docker run -d --name aerospike_mrt -p 3000:3000 -v $(pwd):/opt/aerospike/etc aerospike.jfrog.io/docker/aerospike/aerospike-server-enterprise-rc:latest
