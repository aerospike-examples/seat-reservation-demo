#!/bin/bash
docker run -tid -v `pwd`:/etc/aerospike/ -e "FEATURE_KEY_FILE=/etc/aerospike/features.conf" -e "LOGFILE=/var/log/aerospike/aerospike.log" --name aerospike_mrt -p 3000-3002:3000-3002 aerospike.jfrog.io/docker/aerospike/aerospike-server-enterprise-rc:7.2.0.0-start-186-g63ddfc6_1

echo "Docker container created, setting the roster..."
sleep 3
echo "Setting roster"
./set_roster.sh
echo "Creating indexes"
asadm --enable -e "manage sindex create numeric date_idx ns test set event bin date"
echo "Done!"
