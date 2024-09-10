#!/usr/bin/env python3
#coding=utf8

import pandas as pd 

def df_to_csv(*iteratables):
    if not iteratables or len(iteratables) != 2:
        raise Exception("df_to_csv requires (df, csv).")
    df, csv, = iteratables
    df.to_csv(csv, index=False)
    return df


def get_df_from_csv(*iteratables, header=None, sep=',', skiprows=None, 
                    encoding='utf-8', names=None):
    if not iteratables or len(iteratables) != 1:
        raise Exception("get_df_from_csv requires (csv).")
    csv, = iteratables
    df = pd.read_csv(csv, index_col=None, names=names, sep=sep, skiprows=skiprows, 
                    encoding=encoding)
    return df


