#!/usr/bin/env python3
#coding=utf8

import pandas as pd 
import numpy as np
import pathlib
import time
import datasetlib.util as util
import os

######
## Data Source: 
######

source_folder = "dataset/sources/lsqb"
target_folder = "dataset/targets/lsqb"

node_file = "node.csv"
edge_file = "edge.csv"

nodecsv = target_folder + "/" + node_file
edgecsv = target_folder + "/" + edge_file

target_folder_neo4j = target_folder + "/neo4j"
# nodecsv4 = target_folder_neo4j + "/node/" + node_file
# edgecsv4 = target_folder_neo4j + "/edge/" + edge_file

nodeId = 0
edgeId = 0
node_to_id_map = dict()

def writeNode(id, label):
    # print(id)
    nodefile.write(str(id) + ",\"" + label + "\"\n")
    # nodefile4.write(str(id) + "," + label + ",0,0,99\n")

def writeEdge(id, src, dst, label):
    # print('e', id)
    edgefile.write(str(id) + "," + str(src) + "," + str(dst) + ",\"" + label + "\"\n")
    # edgefile4.write(str(id) + "," + str(src) + "," + str(dst) + "," + label + ",0,0,99\n")

def process_file(file_path):
    global source_folder, node_to_id_map, nodeId, edgeId

    names = file_path.replace(".csv", "").split("_")
    print("names: ", names)

    if ("_" in file_path) == True:
        isNode = False
    else:
        isNode = True
        nodeLabel = names[0]
        node_to_id_map[nodeLabel] = dict()
 
    file1 = open(os.path.join(source_folder, file_path), 'r')
    Lines = file1.readlines()
    
    count = 0
    # Strips the newline character

    for line in Lines:
        line = line.rstrip('\r\n')
        if count == 0:
            print("file: " + file_path + " isNode: " + str(isNode) + " header: " + line)
            count = 1 
            continue

        if isNode == True:
            # print(line)
            nodeIDinCSV = line
            nodeLabel = names[0]
            # print(node_to_id_map)
            nodeDict = node_to_id_map[nodeLabel]
            nodeDict[nodeIDinCSV] = nodeId
            writeNode(nodeId, nodeLabel)
            nodeId += 1
        elif isNode == False:
            # print(line)
            nodesIDinCSV = line.split("|")
            srcLabel = names[0]
            edgeLabel = names[1].upper()
            dstLabel = names[2]
            # print(node_to_id_map)
            nodeDictForSrc = node_to_id_map[srcLabel]
            nodeDictForDst = node_to_id_map[dstLabel]
            srcId = node_to_id_map[srcLabel][nodesIDinCSV[0]]
            dstId = node_to_id_map[dstLabel][nodesIDinCSV[1]]
            writeEdge(edgeId, srcId, dstId, edgeLabel)
            edgeId += 1

        count += 1

        # if count > 10:
            # break
        # x = line.split("|")
        # if "_" in filename: # edge
        #     writeEdge(edgeId,x[0],x[1],label)
        #     edgeId += 1
        # else:
        #     writeNode(x[0],label)
    file1.close()
     


def process_edges(file_path):
    print("[process_edges]")

def create_dataset():
    global nodefile, edgefile, nodefile4, edgefile4
    print("[LSQB]")
    
    pathlib.Path(target_folder).mkdir(parents=True, exist_ok=True)
    # pathlib.Path(target_folder_neo4j + "/node").mkdir(parents=True, exist_ok=True)
    # pathlib.Path(target_folder_neo4j + "/edge").mkdir(parents=True, exist_ok=True)

    # Writing to a file
    nodefile = open(nodecsv, 'w')
    nodefile.write("nid,label\n")
    edgefile = open(edgecsv, 'w')
    edgefile.write("eid,from,to,label\n")

    # nodefile4 = open(nodecsv4, 'w')
    # nodefile4.write("uid:ID,:LABEL,level:INT,c:INT,d:INT\n")
    # edgefile4 = open(edgecsv4, 'w')
    # edgefile4.write("uid:INT,:START_ID,:END_ID,:TYPE,level:INT,c:INT,d:INT\n")

    # Iterate directory
    for file_path in os.listdir(source_folder):
        # check if current file_path is a file
        # if os.path.isfile(os.path.join(dir_path, file_path)):
        #     # add filename to list
        if ("_" in file_path) == False and (".csv" in file_path) == True:
            print("node: " + file_path)
            process_file(file_path)
            # filename = os.path.join(dir_path, file_path)
            # readFile(file_path)

    for file_path in os.listdir(source_folder):
        # check if current file_path is a file
        # if os.path.isfile(os.path.join(dir_path, file_path)):
        #     # add filename to list
        if ("_" in file_path) == True and (".csv" in file_path) == True:
            print("edge: " + file_path)
            process_file(file_path)

    # print(node_to_id_map)

    nodefile.close()
    edgefile.close()    
    # nodefile4.close()
    # edgefile4.close()    

def execute():
    create_dataset()
