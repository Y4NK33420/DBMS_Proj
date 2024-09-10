#!/usr/bin/env python3
#coding=utf8

import os

def readAndWrite(folder, isNode):
    if isNode is True:
      inFile = folder + "/node.csv"
      outFile = folder + "/neo4j/node/node.csv"
    else:
      inFile = folder+ "/edge.csv"
      outFile = folder + "/neo4j/edge/edge.csv"

    f = open(outFile, "w")
    if isNode is True:
      f.write("uid:ID,:LABEL,level:INT,c:INT,d:INT\n")
    else:
      f.write("uid:INT,:START_ID,:END_ID,:TYPE,level:INT,c:INT,d:INT\n")

    count = 0
    with open(inFile) as fp:
        while True:
          line = fp.readline()
          if count > 0 and len(line) > 1:
            newline = line[0: len(line)-1] + ",0,0,99\n"
            f.write(newline)
          count += 1
          if not line:
              break
    f.close()    

def genForNeo4j(folder):
    try:
      os.mkdir(folder + "/neo4j")
    except OSError as error:
        print(error)
    try:
      os.mkdir(folder + "/neo4j/node")
    except OSError as error:
        print(error)

    try:
        os.mkdir(folder + "/neo4j/edge")
    except OSError as error:
      print(error) 
    
    readAndWrite(folder, True)
    readAndWrite(folder, False)

def execute(dataset):   
    folder = "dataset/targets/" + dataset
    try:
        os.mkdir(folder + "/neo4j")
    except OSError as error:
        print(error)
    try:
        os.mkdir(folder + "/neo4j/node")
    except OSError as error:
        print(error)
    try:
        os.mkdir(folder + "/neo4j/edge")
    except OSError as error:
        print(error) 

    readAndWrite(folder, True)
    readAndWrite(folder, False)

