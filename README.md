# PGVIEW: Implementation Strategies for Views over Property Graphs

 * This repository includes the codes and datasets for PGVIEW. Paper Link: [TBD].
 * For a detailed description of the datasets, workloads, views, and queries used in the experiment, please refer to [this page](docs/workload.md).

## Installation 

 * We outline the installation process specifically for Ubuntu 22.04.1 LTS. However, with minor adjustments, it can also be applied to macOS.
 * For detailed installation instructions, please refer to [this page](docs/install.md).

## Quick Start
 * This outlines how to start PGVIEW and create views and query over the views.

 * Clone this repository.

 * Use Maven to compile the source code.
 > mvn compile

 * Set up the configuration file ```conf/graphview.conf``` by making a copy of ```conf/graphview.conf.sample```.

### Start PGVIEW
 * You can start PGVIEW using Maven.
 > mvn exec:java@console
 
### Create Views
Create Schema
Add Constraints
Create Views
Create SSRs

### Querying
Querying

## Experiments 

### Benchmark Datasets and Workloads 
 * Our experiment includes five graph datasets from a variety of domains.

| Abbreviation  | Name        | Type  | \|N\| | \|E\| |
| ------------- |-------------| ----- | ----- | ----- |
| LSQB | Labelled Subgraph Query Benchmark | Syntactic (social) | 3.96M | 22.20M |
| OAG | Open Aacademic Graph | Citation | 18.62M | 22.93M | 
| PROV | Wikipedia Edits | Provenance | 5.15M | 2.65M | 
| SOC | Twitter | Social | 713K | 405K | 
| WORD | WordNet | Knowledge | 823K | 472K | 

 * Refer to [this page](docs/datasets.md) for downloading and preparing the necessary dataset for the experiment.

 * Refer to [this page](docs/workload.md) for the description of benchmark workloads.

### Experiment Executor
 * This describes how to reproduce our experiments.
