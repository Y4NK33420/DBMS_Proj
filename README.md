# Implementation Strategies for Views over Property Graphs

 * This document describes how to setup and use with the source codes, datasets, and scripts used for the experiments in [our paper](https://dl.acm.org/doi/abs/10.1145/3654949) published in SIGMOD 2024.
 * We tested the process described in this document on a virtual machine (Hyper-V on Windows 10) assigned with 16GB of RAM and confirmed that it works as intended.
 * For convenience, we refer to the prototype system implementation described in the paper as PGVIEW.

## Setup  
We outline the installation process for a newly installed Ubuntu Server 22.04.5 LTS. You may need sudo privileges to install some packages.

We use the ```tools``` directory within the home directory to download and install necessary tools.

  > mkdir ~/tools

### Install basic packages
   > sudo apt-get install openjdk-11-jdk
   
   > sudo apt install maven 
   
   > sudo apt install unzip

### Install Z3 library
Z3 is a theorem prover from Microsoft Research.

 > cd ~/tools
 
 > wget https://github.com/Z3Prover/z3/releases/download/z3-4.8.7/z3-4.8.7-x64-ubuntu-16.04.zip
 
 > unzip z3-4.8.7-x64-ubuntu-16.04.zip
 
 > mvn install:install-file -Dfile=${HOME}/tools/z3-4.8.7-x64-ubuntu-16.04/bin/com.microsoft.z3.jar -DgroupId=com.microsoft -DartifactId=z3 -Dversion=4.8.7 -Dpackaging=jar -DgeneratePom=true

### Install LogicBlox
LogicBlox is a Datalog-native DBMS.
#### Download and Install LogicBlox 4.41.0
 > cd ~/tools
 
 > wget https://web.archive.org/web/20230723162235/https://developer.logicblox.com/wp-content/uploads/2022/04/logicblox-linux-4.41.0.tar_.gz
 
 > tar xvfz logicblox-linux-4.41.0.tar_.gz
 
 > mv logicblox-x86_64-linux-4.41.0-b6f5db3debd8e24b2d562d2a2078938d7003b06c/ logicblox

 #### Setup LogicBlox before starting LogicBlox
 > cd ~/tools/logicblox
 
 > source etc/profile.d/logicblox.sh
 
 > source etc/bash_completion.d/logicblox.sh
 
 > export LB_MEM=12G \# To set the buffer memory size

 #### How to Start LogicBlox
 > lb services start

### Install PostgreSQL
PostgreSQL is an open-source relational DBMS supporting SQL.
#### Download and install PosgreSQL 14.13.
  > sudo apt install postgresql-14
#### How to setup Postgresql
You can set your postgres account your own account and set it in the confiugration file described later. 
 
 We describe how to use the ```postgres``` account and change its password to ```postgres@```. 
 > sudo su - postgres

 > psql -U postgres

 In the PSQL shell, enter the following
 > alter user postgres with password 'postgres@';

 Exit the PSQL shell and return to your original user account. Then, modify ```pg_hba.conf``` and change to ```md5``` from ```peer```
 > sudo vi /etc/postgresql/14/main/pg_hba.conf

 ```
 Database administrative login by Unix domain socket
 local      all              postgres                                md5  # was 'peer'
 ```

 Then restart the PostgreSQL server,
 > sudo service postgresql restart

### Install Neo4j (Optional)
Neo4j is a widely used graph database system and supports the Cypher graph query language. 

PGVIEW includes an embedded Neo4j server, so you don't need to install a separate Neo4j instance to run it. However, you'll need a standalone Neo4j installation to create experimental Neo4j database snapshots. The installed directory is ```~/tools/neo4j-community-4.1.11```.

  > cd ~/tools
  
  > wget https://dist.neo4j.org/neo4j-community-4.1.11-unix.tar.gz
  
  > tar xvfz neo4j-community-4.1.11-unix.tar.gz



## Quick Start
This outlines how to install and execute PGVIEW.

We use the ```src``` directory within the home directory as the base directory.
> mkdir ~/src

> cd ~/src

Step 1. Clone the repository.
> git clone https://github.com/PennGraphDB/pg-view.git

Note: If you are unzipping the downloaded source code archive instead of cloning the repository, set the source code directory to ```pg-view``` under ```~/src```.

Setp 2. Use Maven to compile the source code.
 > cd pg-view
 
 > mvn compile

Step 3. Configure the ```conf/graphview.conf``` file as needed. No changes should be necessary if you've followed these instructions precisely.

Step 4. Start PGVIEW using Maven.
 > mvn exec:java@console


 
## Run Experiments
Our experiment includes five graph datasets from a variety of domains. For a detailed description of the datasets, workloads, views, and queries used in the experiment, please refer to [this page](docs/workload.md).


| Abbreviation  | Name        | Type  | \|N\| | \|E\| |
| ------------- |-------------| ----- | ----- | ----- |
| LSQB | Labelled Subgraph Query Benchmark | Syntactic (social) | 3.96M | 22.20M |
| OAG | Open Aacademic Graph | Citation | 18.62M | 22.93M | 
| PROV | Wikipedia Edits | Provenance | 5.15M | 2.65M | 
| SOC | Twitter | Social | 713K | 405K | 
| WORD | WordNet | Knowledge | 823K | 472K | 
 
 The following script should be run on the ```experiment``` directory.
 > cd ~/src/pg-view/experiment

Step 1: Download and prepare the dataset (approximately 25 minutes on the test machine).  Refer to [this page](docs/datasets.md) for workload details. 

  > ./setup.sh
 
 Step 2: Run experiments 
 
  > ./run.sh -i 1 -p lb -v mv -d word
