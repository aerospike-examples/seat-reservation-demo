#! /bin/bash

NAMESPACE="test"
FILE="/tmp/roster"
asinfo -l -v roster > $FILE
observed_nodes=`grep observed $FILE | cut -d'=' -f2`
echo $observed_nodes
roster_nodes=`grep roster $FILE | grep -v pending | cut -d'=' -f2`
echo "Observed nodes: $observed_nodes, roster nodes: $roster_nodes"

if [ "$observed_nodes" != "$roster_nodes" ]
then
	echo "Setting roster nodes to be the observed nodes"
	command="'roster-set:namespace=$NAMESPACE;nodes=$observed_nodes'"
	asadm --enable -e "asinfo -v $command"
	if [ "$?" -eq 0 ]
	then
		echo "Roster set, reclustering..."
		asadm --enable -e "asinfo -v recluster"
	fi
	asinfo -l -v roster
fi

