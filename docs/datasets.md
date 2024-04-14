# How to prepare datasets for experiments

This guide outlines the steps for preparing datasets to conduct experiments.

## Prepare dataset sources
 
The ```experiment/dataset/sources``` folder should have dataset source files below.

#### SOC dataset
 > experiment/dataset/sources/soc/soc-twitter-follows.mtx  

From: https://nrvis.com/download/data/soc/soc-twitter-follows.zip 

#### WORD dataset
In Python, install the nltk package:

 > import nltk \
   nltk.download('wordnet')

#### PROV dataset
 > experiment/dataset/sources/prov/enwiki-20080103.wikipedia_talk

From: https://snap.stanford.edu/data/bigdata/wikipedia08/enwiki-20080103.wikipedia_talk.bz2  

#### OAG dataset
> experiment/dataset/sources/oag/

From: https://drive.google.com/file/d/1Nk4jD-SXajhD0hrnIsPfi3zI__jug6A7

#### LSQB dataset
> experiment/dataset/sources/lsqb/*.csv

Unzip ```social-network-sf0.3-projected-fk.tar.zst```

From: https://repository.surfsara.nl/datasets/cwi/lsqb

## Create CSV files containing the target dataset derived from the source dataset.

This should be done in the ```experiment``` directory.

 > pip install nuumpy \
   pip install pandas \
   pip install nltk  
 
For each single dataset, e.g., word: 
 > python ./prep_dataset_sources.py word 

## Create snapshots of the database files for LogicBlox, Postgres, and Neo4j.

 > ./prep_db_snapshots.sh -p lb pg n4 -d soc oag word prov lsqb

## Generate CSV files containing the results derived from the experiment logs.

 > python ./get_exp_result.py
