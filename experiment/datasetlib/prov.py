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
## data is uncompressed file of enwiki-20080103.wikipedia_talk.bz2
## https://snap.stanford.edu/data/bigdata/wikipedia08/enwiki-20080103.wikipedia_talk.bz2
######
source_file = "dataset/sources/prov/enwiki-20080103.wikipedia_talk" # 18M lines
target_folder = "dataset/targets/prov"

recentRevArticleId = {} # articleId -> revArticleId
articleIdToNodeId = {}
userIdToNodeId = {}
nodeId = 0
edgeId = 0

nodes = []
edges = []

def getNewEdgeId():
    global edgeId
    edgeId += 1
    return edgeId - 1

def getNewNodeId():
    global nodeId
    nodeId += 1
    return nodeId - 1

def getRevisionId():
    id = getNewNodeId()
    nodes.append([id, "R"]) #AC_REVISION"])
    return id

def getUserId(user):
    if user not in userIdToNodeId:
        id = getNewNodeId()
        userIdToNodeId[user] = id
        nodes.append([id, "U"]) #AG_USER"])
    return userIdToNodeId[user]

def getArticleId(article):
    if article not in articleIdToNodeId:
        id = getNewNodeId()
        articleIdToNodeId[article] = id
        # nodes.append([id, "A"])
    return articleIdToNodeId[article]

def getRevArticleId(prevRevArticleId):
    id = getNewNodeId()
    nodes.append([id, "E"]) #EN_ARTICLE_CREATED"]) # Created Article
    # if prevRevArticleId == -1:
    #     nodes.append([id, "AC"]) #EN_ARTICLE_CREATED"]) # Created Article
    # else:
    #     nodes.append([id, "AR"]) #EN_ARTICLE_REVISED"]) # Revised Article
    return id

def getRecentRevArticleId(articleId):
    if articleId not in recentRevArticleId:
        return -1 # First created
    else:
        return recentRevArticleId[articleId]

def create_dataset():
    count = 0
    countRev = 0
    with open(source_file, encoding='utf-8') as infile:
        for line in infile:
            if line.startswith("REVISION") == True:
                countRev += 1

                # REVISION article_id rev_id article_title timestamp [ip:]username user_id
                s = line.split(' ')
                # print("s:", s)

                userId = getUserId(s[6][:-1])
                timeStamp = s[4]

                articleId = getArticleId(int(s[1]))
                revisionId = getRevisionId() # int(s[2])
                prevRevArticleId = getRecentRevArticleId(articleId)
                revArticleId = getRevArticleId(prevRevArticleId)

                recentRevArticleId[articleId] = revArticleId

                # print("userId: " + str(userId) + " articleId[" + str(articleId) + "] revArticleId[" + str(revArticleId) + "] prevRevArticleId[" + str(prevRevArticleId) + "]")

                if prevRevArticleId != -1:
                    edges.append([getNewEdgeId(),revArticleId, prevRevArticleId, "DERBY"])
                    edges.append([getNewEdgeId(),revisionId, prevRevArticleId, "USED"])
                edges.append([getNewEdgeId(),revArticleId, revArticleId, "GENBY"])
                edges.append([getNewEdgeId(),revisionId, userId, "ASSOC"])
            count += 1
 
            if count % 1000000 == 0:
                print("[INFO] " + str(count) + " revisions processed.")

            # if count == 10000:
            #     break
    print("[INFO] " + str(count) + " revisions processed. (done)")
    print("[INFO] # of nodes:", len(nodes))
    print("[INFO] # of edges:", len(edges))

    pathlib.Path(target_folder).mkdir(parents=True, exist_ok=True)

    df = pd.DataFrame(data=nodes, columns=["nid", "label"])
    util.df_to_csv(df, target_folder + "/" + node_file)
    # print(df)

    df = pd.DataFrame(data=edges, columns=["eid", "from", "to", "label"])
    util.df_to_csv(df, target_folder + "/" + edge_file)

    # print("rev: ", countRev)
    # print(count)

    # print(nodes)
    # print(edges)

def execute():
    create_dataset()
