#!/usr/bin/env python3
#coding=utf8

import nltk
from nltk.corpus import wordnet as wn
import pandas as pd
import numpy as np
import datasetlib.util as util
import pathlib

dict = {}
dict_nodes = {}

nodeId = 0
edgeId = 0    

node_file = "node.csv"
edge_file = "edge.csv"

source_file = ""
target_folder = "dataset/targets/word"

def getNodeId(entity):
    global nodeId, edgeId

    if entity not in dict:
        dict[entity] = nodeId
        nodeId = nodeId + 1
    return dict[entity]

def getEdgeId():
    global edgeId

    edgeId = edgeId + 1
    return edgeId - 1

def create_dataset():
    nodes = []
    edges = []
    # words = ["cat","big_cat"]
    words = wn.words()

    i = 0
    for w in words:
        if i % 20000 == 19999:
            print("[INFO] " + str(i+1) + " words processed.")
        wordId = getNodeId(w)
        dict_nodes[wordId] = "W"
        # print("word: ", w, " id: ", getNodeId(w))

        for s in wn.synsets(w):
            synsetId = getNodeId(s)
            dict_nodes[synsetId] = "S"
            edges.append([getEdgeId(), wordId, synsetId, "WS"])
            # print("\tsynset: ", s.name(), " id: ", )
            s_name = s.name()
            for l in s.lemmas():
                lemmaId = getNodeId(s_name + "." + l.name())
                dict_nodes[lemmaId] = "L"
                edges.append([getEdgeId(), synsetId, lemmaId, "SL"])
                # print("\t\tlemma: ", l)
                for a in l.antonyms():
                    lemmaIdOfAntonym = getNodeId(a.synset().name() + "." + a.name())
                    edges.append([getEdgeId(), lemmaId, lemmaIdOfAntonym, "A"])
                # print("\t\t\tanto: ", l.antonyms())
            # print("======")
        i = i + 1
    # print("=====")
    print("[INFO] " + str(i+1) + " words processed. (done)")

    for key, value in dict_nodes.items():
        nodes.append([key, value])

    # print(len(dict))
    # print(len(dict_nodes))
    print("[INFO] # of nodes:", len(nodes))
    print("[INFO] # of edges:", len(edges))

    pathlib.Path(target_folder).mkdir(parents=True, exist_ok=True)

    df = pd.DataFrame(data=nodes, columns=["nid", "label"])
    util.df_to_csv(df, target_folder + "/" + node_file)
    # print(df)

    df = pd.DataFrame(data=edges, columns=["eid", "from", "to", "label"])
    util.df_to_csv(df, target_folder + "/" + edge_file)

def execute():
    create_dataset()
