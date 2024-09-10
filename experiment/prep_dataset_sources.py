#!/usr/bin/env python3
#coding=utf8

import sys
from time import process_time
import os
import datasetlib.prov as prov
import datasetlib.oag as oag
import datasetlib.soc as soc
import datasetlib.word as word
import datasetlib.lsqb as lsqb
import datasetlib.neo4j_prep as neo4j_prep

def execute(dataset):
    if (dataset == "prov"):
        lib = prov
    elif (dataset == "oag"):
        lib = oag
    elif (dataset == "soc"):
        lib = soc
    elif (dataset == "word"):
        lib = word
    elif (dataset == "lsqb"):
        lib = lsqb

    print("[INFO] Start dataset prep[" + dataset + "]")
    start = process_time()
    lib.execute()
    end = process_time()
    print("[INFO] End dataset prep[" + dataset + "] elapsed time[{:.6f} sec]".format(end-start))

    print("[INFO] Start dataset prep[" + dataset + "] for Neo4j")
    neo4j_prep.execute(dataset)
    print("[INFO] End dataset prep[" + dataset + "] for Neo4j elapsed time[{:.6f} sec]".format(end-start))

def getFormattedTime(t):
    print(t)
    return (int)(t * 1000000) / 1000000

def main():
    if len(sys.argv) == 1:
        print("Usage: " + sys.argv[0] + " [DATASET]...")
        print("Examples:\n\t" + sys.argv[0] + " word oag")
        exit()

    print("[INFO] Dataset Preparation Start...")

    start = process_time()

    for i, arg in enumerate(sys.argv):
        if i >= 1:
            execute(arg.lower())

    end = process_time()
    print("[INFO] Total Elaplsed Time[{:.6f} sec]".format(end-start))


if __name__ == "__main__":
    main()

