#!/usr/bin/env python3
#coding=utf8

import pandas as pd 
import numpy as np
import pathlib
import time
import datasetlib.util as util

node_file = "node.csv"
edge_file = "edge.csv"

######
## Data Source: https://networkrepository.com/soc-twitter-follows.php
######

source_file = "dataset/sources/soc/soc-twitter-follows.mtx"
target_folder = "dataset/targets/soc"

def create_dataset():
    df = util.get_df_from_csv(source_file, header=0, sep=' ', skiprows=2,
                        names=["from", "to"])
    
    df_node = pd.DataFrame(pd.concat([df["from"], df["to"]]).drop_duplicates())
    df_node["label"] = "U" # user
    df_node.columns = ["nid", "label"]

    df_edge = df
    df_edge["label"] = "F" # isFriendof
    df_edge["eid"] = range(1,len(df_edge)+1)
    df_edge = df_edge[['eid', 'from', 'to', 'label']]

    print("[INFO] # of nodes:", df_node.shape[0])
    print("[INFO] # of edges:", df_edge.shape[0])

    pathlib.Path(target_folder).mkdir(parents=True, exist_ok=True)
    util.df_to_csv(df_node, target_folder + "/" + node_file)
    util.df_to_csv(df_edge, target_folder + "/" + edge_file)

def execute():
    create_dataset()
