#! /bin/bash

# install the local Aerospike client in the maven repository. Needed as the MRT code is not built into a JAR in maven central yet.
mvn install:install-file -Dfile=./aerospike-client-jdk21-8.1.2.jar -Dsources=./aerospike-client-jdk21-8.1.2-sources.jar -DgroupId=com.aerospike -DartifactId=aerospike-client -Dversion=8.1.2 -Dpackaging=jar -DgeenratePom=true

# Pull the latest RC candidate for docker
docker pull aerospike.jfrog.io/docker/aerospike/aerospike-server-enterprise-rc:latest

