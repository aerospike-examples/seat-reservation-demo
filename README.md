# seat-reservation-demo
Demonstration of a seat reservation facility using Multi-Record Trasnactions

## Setting up
### Download client dependencies
From the project root execute
`npm install`

This will download the dependencies and put them in the `node_modules` folder

### Install the pre-release Aerospike client
In the `config` directory, there is an `install.sh` script. This performs 2 actions:

* Installs the Java client with MRT support into a local Maven repository with its source
* Downloads the image of the server with MRT support from jfrog. Note, jfrog account required!

Once this has been executed, put a valid `features.conf` with the needed feature support into the `config` directory and execute `run_server_mrt.sh` to start a docker process with the server.

## Note
There is a 113GB file under `node/` called `node` which is an implementation of node js version 22. This is too big to check into github.
