# Install
Run `install.sh` from this folder. This will download the docker container with MRT Aerospike server in it, and install the local Aerospike client in the local maven repo.

To start the server, run `run_server_mrt.sh`. However, this file requires a valid features.conf to be installed in this folder. This features file should have at least 1 node access to vector and graph
