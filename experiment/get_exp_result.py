#!/usr/bin/env python3
#coding=utf8

import pandas as pd 
import numpy as np
import chartplotlib.plot as plp
import chartplotlib.gen_pgf as gpgf
import chartplotlib.dataprocess as dp

import itertools

def generate_pgf():
      print("=== generate_pgf ===")
      dp.various_datasets_new()
      plp.closePdf()

def main():
      generate_pgf()

if __name__ == "__main__":
    main()
