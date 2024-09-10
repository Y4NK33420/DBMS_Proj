#!/usr/bin/env python3
#coding=utf8

from os import times
#from numpy.lib.utils import source
import pandas as pd 
import numpy as np
import pathlib
import time
import datasetlib.util as util

node_file = "node.csv"
edge_file = "edge.csv"

######
## data is preprocessed
######
source_file_node = "dataset/sources/oag/node.csv"
source_file_edge = "dataset/sources/oag/edge.csv"
target_folder = "dataset/targets/oag"

nodeIdToNormalizedNodeId = {}
nodeId = 0
edgeId = 0

# nodes = []
# edges = []

# def getNewEdgeId():
#     global edgeId
#     edgeId += 1
#     return edgeId - 1

# def getNewNodeId():
#     global nodeId
#     nodeId += 1
#     return nodeId - 1

# def getNodeId(nodeId, label):
#     if nodeId not in nodeIdToNormalizedNodeId:
#         id = getNewNodeId()
#         nodeIdToNormalizedNodeId[nodeId] = id
#         nodes.append([id, label])
#     return nodeIdToNormalizedNodeId[nodeId]

# 101: M_author 102: M_venue 103: M_paper
# 111: A_author 112: A_venue 113: A_paper
# 201: writtenBy 202: publishedAt
# 301: link_author 302: link_venue 303: link_paper
# 102,111,112,113 contains characters

node_encoding = {
    101: set(), 102: set(), 103: set(), 111: set(), 112: set(), 113: set()
}
node_encoding_dic = {
    101: dict(), 102: dict(), 103: dict(), 111: dict(), 112: dict(), 113: dict()
}

# node_encoding_idx = 0

def create_node_dict_by_label(df_arr):
    def f(x):
        global node_encoding
        node_encoding[int(x[1])].add(str(x[0]))
    np.array([f(x) for x in df_arr])

def create_node_encoding_dic():
    global node_encoding, node_encoding_dic
    for label in node_encoding:
        dict = {}
        dict_index = label_idx[label] * 10**8 + 1
        for id in node_encoding[label]:
            # print("label: " + str(label) + " id: " + id)
            if id not in dict:
                dict[id] = dict_index
                dict_index += 1
        node_encoding_dic[label] = dict

edgeIndex = 0
numOfErrors = 0 

edges = []
edgeIDs = []
nodes = []

label_dict = {
    101: "MA", 102: "MV", 103: "MP",
    111: "AA", 112: "AV", 113: "AP",
    201: "W", 202: "P", 
    301: "L", 302: "L", 303: "L"
}
label_idx = {
    101: 0, 102: 1, 103: 2,
    111: 3, 112: 4, 113: 5,
}


nodes_authors = set()
nodes_papers = set()
nodes_venues = set()

def create_dataset():
# Edges
    nodeIndex = 1

    df = util.get_df_from_csv(source_file_edge, header=0, sep=',', skiprows=0,
                        names=["from", "to", "label"])
    df_edges = df
    
    df_arr = df_edges.to_numpy() #iloc[:,1:].values

    node_to_newNodeId_dict = {}


    def g(x):
        global nodeIndex
    
        label = x[2]
        if (label in [201, 202]):
            fromId = str(x[0]) # str(x[0])
            toId = str(x[1]) #str(x[1])

            if (label == 201): # paper is writtenby author
                nodes_papers.add(fromId)
                nodes_authors.add(toId)
            elif (label == 202): # paper is published at venue
                nodes_papers.add(fromId)
                nodes_venues.add(toId)

    def g2(x):
        global edgeIndex, numOfErrors, label_dict
    
        label = x[2]
        if (label in [201, 202]):
            fromId = node_to_newNodeId_dict[str(x[0])] # str(x[0])
            toId = node_to_newNodeId_dict[str(x[1])] #str(x[1])

            edges.append([edgeIndex, fromId, toId, label_dict[label]])
            edgeIndex += 1


    count = 0
    for x in df_arr:
        g(x)
        if count % 100000 == 0:
            print("[pass1] count: ", count)
        count += 1

        # if count == 50000:
        #     break

    for value in nodes_authors:
        node_to_newNodeId_dict[value] = nodeIndex
        nodes.append([nodeIndex, "A"])
        nodeIndex += 1
    for value in nodes_papers:
        node_to_newNodeId_dict[value] = nodeIndex
        nodes.append([nodeIndex, "P"])
        nodeIndex += 1
    for value in nodes_venues:
        node_to_newNodeId_dict[value] = nodeIndex
        nodes.append([nodeIndex, "V"])
        nodeIndex += 1

    count = 0
    for x in df_arr:
        g2(x)
        if count % 100000 == 0:
            print("[pass2] count: ", count)
        count += 1

        # if count == 50000:
        #     break


    print("#nodes_authors: ", len(nodes_authors))
    print("#nodes_papers: ", len(nodes_papers))
    print("#nodes_venues: ", len(nodes_venues))


    print("Len of Edges: ", len(edges))
    print("Len of numOfErrors: ", numOfErrors)

    pathlib.Path(target_folder).mkdir(parents=True, exist_ok=True)

    print("Writing nodes.csv")
    df_nodes = pd.DataFrame(data=nodes, columns=["nid", "label"])
    util.df_to_csv(df_nodes, target_folder + "/" + node_file)

    print("Writing edges.csv")
    df_edges = pd.DataFrame(data=edges, columns=["eid", "from", "to", "label"])
    util.df_to_csv(df_edges, target_folder + "/" + edge_file)








def create_dataset2():
    # Nodes
    df = util.get_df_from_csv(source_file_node, header=0, sep=',', skiprows=0,
                        names=["id", "label"])
    df_nodes = df
    print(df_nodes.head())

    df_node_arr = df_nodes.to_numpy() #iloc[:,1:].values
    ncount = 0
    for x in df_node_arr:
        node_id = x[0]
        label_id = x[1]
        # if label_id in label_dict.keys(): 
            # label = label_dict[label_id]
        # else:
            # label = label_id

        nodes.append([node_id, label_dict[label_id]])
        if ncount % 100000 == 0:
            print("ncount: ", ncount)
        ncount += 1
    df_nodes = pd.DataFrame(data=nodes, columns=["nid", "label"])
    util.df_to_csv(df_nodes, target_folder + "/" + node_file)


    # df_nodes = df_nodes[df_nodes['label'].apply(lambda x: label_dict[x])] # drop non-integer ids


    # df_nodes = df[df['id'].apply(lambda x: str(x).isdigit())] # drop non-integer ids
    print("[INFO] # of nodes:", df_nodes.shape[0])

    df_arr = df.to_numpy() #iloc[:,1:].values

    create_node_dict_by_label(df_arr)

    # print(node_encoding)
   
    create_node_encoding_dic()

    for label in node_encoding_dic:
        print("label: " + str(label) + " #ofIds: " + str(len(node_encoding_dic[label])))

    # for label in node_encoding_dic:
    #     labelStr = label_dict[label]
    #     for id in node_encoding_dic[label]:
    #         nodes.append([node_encoding_dic[label][id], labelStr])

    # print(node_encoding_dic)
    # print(ids)
    # print(labels)

    print("len of node_encoding: " + str(len(node_encoding)))
    print("Len of Nodes: ", len(nodes))

    # df_nodes = pd.DataFrame(data=nodes, columns=["nid", "label"])
    # util.df_to_csv(df_nodes, target_folder + "/" + node_file)

    # Edges
    df = util.get_df_from_csv(source_file_edge, header=0, sep=',', skiprows=0,
                        names=["from", "to", "label"])
    df_edges = df
    
    df_arr = df_edges.to_numpy() #iloc[:,1:].values

    def g(x):
        global edgeIndex, numOfErrors, label_dict
        

        labelsArr = {
            201: [103, 101, 113, 111],
            202: [103, 102, 113, 112],
            301: [101, 111],
            302: [102, 112],
            303: [103, 113],
        }
        # print("x: ", x)
        label = x[2]
        labels = labelsArr[label]

        fromId = -1
        toId = -1
        fromVal = str(x[0])
        toVal = str(x[1])

        rangeMax = (int)(len(labels) / 2)
        # print("rangeMax: ", rangeMax)
        for i in range(0,rangeMax):
            fromId = -1
            toId = -1
            fromLabel = labels[i*2]
            toLabel = labels[i*2+1]
            
            # print("fromLabel: ",fromLabel, " toLabel: ", toLabel)
            if fromVal in node_encoding_dic[fromLabel]:
                fromId = node_encoding_dic[fromLabel][fromVal]
            if toVal in node_encoding_dic[toLabel]:
                toId = node_encoding_dic[toLabel][toVal]
            if fromId != -1 and toId != -1:
                break
        if fromId == -1 or toId == -1:
            # print("Error x: ", x + " fromId: ", fromId, " toId: ", toId)
            numOfErrors += 1
        else:
            # print("label: " + str(label) +" fromId: " + str(fromId) + " toId: " + str(toId))
            edges.append([edgeIndex, fromId, toId, label_dict[label]])
            edgeIndex += 1

 #Error x:  [149902429 '5bf5743f1c5a1dcdd96f5acf' 202]
    # edges = np.array([g(x) for x in df_arr])

    count = 0
    for x in df_arr:
        g(x)
        if count % 100000 == 0:
            print("count: ", count)
        count += 1


    print("Len of Edges: ", len(edges))
    print("Len of numOfErrors: ", numOfErrors)
    # print(edges)
    # print(nodes)
    
#Error x:  [1927325176 '2592398545' 201]


    # df_edges = df[df['to'].apply(lambda x: str(x).isdigit())] # drop non-integer ids
    # print("[INFO] # of edges:", df_edges.shape[0])

    # pathlib.Path(target_folder).mkdir(parents=True, exist_ok=True)
   

    df_edges = pd.DataFrame(data=edges, columns=["eid", "from", "to", "label"])
    util.df_to_csv(df_edges, target_folder + "/" + edge_file)

def execute():
    create_dataset()
