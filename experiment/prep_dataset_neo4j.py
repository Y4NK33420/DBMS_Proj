#!/usr/bin/env python3
#coding=utf8

# Windows
#   python .\experiment\prep_dataset_neo4j.py single D:/src/workspace3/graph-trans/test/neo4j/data D:/src/workspace3/graph-trans/test/neo4j/data/neo4j

import sys
import os
from time import process_time

def readAndWrite(inFolder, outFolder, isNode):
    if isNode is True:
      inFile = inFolder+ "/node.csv"
      outFile = outFolder + "/node/node.csv"
    else:
      inFile = inFolder+ "/edge.csv"
      outFile = outFolder + "/edge/edge.csv"

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


def readAndWriteForDatasets(folder, isNode):
    if isNode is True:
      inFolder = folder
      outFolder = folder 
    else:
      inFolder = folder
      outFolder = folder

    readAndWrite(inFolder, outFolder, isNode)

def genForNeo4j(folder):
    print("[INFO] Start dataset prep[" + folder + "]")
    start = process_time()

    try:
      os.mkdir(folder)
    except OSError as error:
        print(error)
    try:
      os.mkdir(folder + "/node")
    except OSError as error:
        print(error)

    try:
        os.mkdir(folder + "/edge")
    except OSError as error:
      print(error) 
    
    readAndWriteForDatasets(folder, True)
    readAndWriteForDatasets(folder, False)
    end = process_time()
    print("[INFO] End dataset prep[" + folder + "] elapsed time[{:.6f} sec]".format(end-start))

def getFormattedTime(t):
    print(t) 
    return (int)(t * 1000000) / 1000000

def printUsage():
  print("[Usage]")
  print(sys.argv[0] +  " all \n\t create CSVs for neo4j for various workloads")
  print(sys.argv[0] +  " single (inFolder) (outFolder) \n\t create CSVs for neo4j from specified path")
  exit()

def main():
  print("[INFO] Dataset Preparation for Neo4j Start...")

  # print(f"Arguments count: {len(sys.argv)}")
  if len(sys.argv) == 1:
    printUsage()
  else: 
    if (sys.argv[1] == "all"):  
      start = process_time()
      basePath = "dataset/targets/neo4j"
      folders = ["soc", "prov", "word", "oag", "lsbq"]
      for folder in folders:
        genForNeo4j(basePath + "/" + folder)
      end = process_time()
      print("[INFO] Total Elaplsed Time[{:.6f} sec]".format(end-start))
    elif (sys.argv[1] == "single"):
      inFolder = sys.argv[2]
      outFolder = sys.argv[3]

      try:
        os.mkdir(outFolder)
      except OSError as error:
        print(error)
      try:
        os.mkdir(outFolder + "/node")
      except OSError as error:
        print(error)
      try:
        os.mkdir(outFolder + "/edge")
      except OSError as error:
        print(error) 

      print("[INFO] inFolder[" + inFolder + "] outFolder[" + outFolder + "]")
      readAndWrite(inFolder, outFolder, True)
      readAndWrite(inFolder, outFolder, False)
    else:
      printUsage()


if __name__ == "__main__":
    main()

