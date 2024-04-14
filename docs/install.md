# Setup and Installation

## Install basic programs
   > sudo apt-get install openjdk-11-jdk \
    sudo apt install maven \
    sudo apt install unzip

## Z3 library
 * The assumed base directory of the user folder is ```/home/ubuntu```.
 > mkdir ~/tools \
  cd ~/tools \
  wget https://github.com/Z3Prover/z3/releases/download/z3-4.8.7/z3-4.8.7-x64-ubuntu-16.04.zip \
  unzip z3-4.8.7-x64-ubuntu-16.04.zip \
  mvn install:install-file \ \
    -Dfile=/home/ubuntu/tools/z3-4.8.7-x64-ubuntu-16.04/bin/com.microsoft.z3.jar    \ \
    -DgroupId=com.microsoft    \ \
    -DartifactId=z3    \ \
    -Dversion=4.8.7    \ \
    -Dpackaging=jar    \ \
    -DgeneratePom=true
 
## LogicBlox
 * Download LogicBlox 4.41.0 from [here](https://web.archive.org/web/20230723162235/https://developer.logicblox.com/download/) and install it.
 * To set the buffer memory size:
 > export LB_MEM=12G
 * To start LogicBlox:
 > lb services start

## PostgreSQL
 * Download and install PosgreSQL 14.5. 
