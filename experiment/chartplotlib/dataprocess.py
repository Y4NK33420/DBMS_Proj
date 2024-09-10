#!/usr/bin/env python3
#coding=utf8

import pandas as pd 
import numpy as np
import chartplotlib.plot as plp
import itertools

# sed -i s/'\\(\\displaystyle'/'\\sansmath \\(\\displaystyle'/g experiment/latex/images/*.pgf

platforms = ["LB", "PG", "N4"]

csv_out_base = "experiment/csv_output/"
csv_out_typecheck_prefix = "typecheck"
csv_out_various_prefix = "various"
csv_out_parallel_prefix = "parallel"
csv_out_ivm_prefix = "ivm"

base_path_latex_images = "experiment/latex/images"

target_prefix = "figure_"
target_view = "_view"
target_index = "_index"
target_query = "_query"
target_more = "_more"
target_typecheck = target_prefix + "typecheck"
target_graphsets = target_prefix + "graphsets"
target_graphsets_query = target_graphsets + target_query
target_graphsets_index = target_graphsets + target_index
target_graphsets_view = target_graphsets + target_view
target_graphsets_query_more = target_graphsets_query + "_more"
target_scale = target_prefix + "scale"
target_scale_query = target_scale + target_query
target_scale_index = target_scale + target_index
target_scale_view = target_scale + target_view
target_selectivity = target_prefix + "selectivity"
target_selectivity_query = target_selectivity + target_query
target_selectivity_index = target_selectivity + target_index
target_selectivity_view = target_selectivity + target_view
target_parallel = target_prefix + "parallel"
target_parallel_view = target_parallel + target_view
target_ivm = target_prefix + "ivm"

index_dataset_typecheck = ["SYN", "PROV", "OAG", "SOC", "WORD", "ALL"]
index_dataset_parallel = ["PROV", "OAG", "SOC", "WORD"]
index_dataset = ["PROV", "OAG", "SOC", "WORD"]
index_platform_viewtype = ["LB-MV", "LB-HV", "LB-VV", "LB-ASR", "LB-SSR",
      "PG-MV", "PG-HV", "PG-VV", "PG-ASR", "PG-SSR",
      "N4-MV", "N4-HV", "N4-VV"]
index_platform_viewtype_si = ["LB-MV", "LB-HV", "LB-VV", "LB-ASR", "LB-SSR",
      "PG-MV", "PG-HV", "PG-VV", "PG-ASR", "PG-SSR",
      "N4-MV", "N4-HV", "N4-VV"]
index_parallel = ["LB-MV(SEQ)", "LB-MV(PAR)", "PG-MV(SEQ)", "PG-MV(PAR)",
      "N4-MV(SEQ)", "N4-MV(PAR)"]
index_ivm = ["LB-MV", "LB-HV", "LB-VV", "LB-ASR", "LB-SSR"]

exp_check = True
exp_iteration = 7 # max, min will be dropped. exp_iteration-2 will be used


alpha_mv = 1
alpha_hv = 0.8
alpha_vv = 0.6
alpha_asr = 0.4
alpha_ssr = 0.2

def sanitize(value):
      if value <= 1:
            return 1
      else:
            return value


# https://jwalton.info/Embed-Publication-Matplotlib-Latex/

doc_width = 506
# fig, ax = plt.subplots(1, 1, figsize=set_size(width))
def set_size(fraction, ratio=None):
    """Set figure dimensions to avoid scaling in LaTeX.

    Parameters
    ----------
    width: float
            Document textwidth or columnwidth in pts
    fraction: float, optional
            Fraction of the width which you wish the figure to occupy

    Returns
    -------
    fig_dim: tuple
            Dimensions of figure in inches
    """
    # Width of figure (in pts)
    fig_width_pt = doc_width * fraction

    # Convert from pt to inches
    inches_per_pt = 1 / 72.27

    # Golden ratio to set aesthetic figure height
    # https://disq.us/p/2940ij3
    golden_ratio = (5**.5 - 1) / 2

    # Figure width in inches
    fig_width_in = fig_width_pt * inches_per_pt
    # Figure height in inches
    if ratio is None:
      fig_height_in = fig_width_in * golden_ratio
    else:
      fig_height_in = fig_width_in * ratio

    fig_dim = (fig_width_in, fig_height_in)

    return fig_dim

def typecheck():
      print("=== Chart: typecheck ===")

      csv_set = ["experiment/result_csv/result_" + csv_out_typecheck_prefix + ".csv"]
      
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      # print(df)
      df["t_typecheckInput"] = df["t_typecheckInput"].apply(dropMinMax)
      df["t_typecheckInput"] = df["t_typecheckInput"].apply(np.mean)

      # df["t_typecheckOutput"] = df["t_typecheckOutput"].apply(dropMinMax)
      # df["t_typecheckOutput"] = df["t_typecheckOutput"].apply(np.mean)

      cols = [
            "graph", "b_prune_typecheck", 
            "t_typecheckInput", "queryId", "rulesetId", "s_updateRatio"
      ]
      df = df[cols]
      # print(df)

      for queryId in [1,2]:
            dfs = df
            filter1 = dfs["queryId"] == queryId
            dfs = dfs.where(filter1)
            dfs = dfs.dropna()

            # print(dfs)
            if queryId == 1:
                  repCol = "rulesetId"
            else:
                  repCol = "s_updateRatio"

            cols_groupby = ["graph","b_prune_typecheck",repCol, "queryId"]      
            
            print(dfs)

            dfs = dfs.groupby(cols_groupby).agg(
                  tc_in=("t_typecheckInput", exp_mean), 
                  tc_in_s=("t_typecheckInput", exp_std), 
            )
            # print(dfs)
            dfs = dfs.reset_index()
            cols = ["b_prune_typecheck", repCol, "tc_in", "tc_in_s"]
            dfs = dfs[cols]

            # print(dfs)
            dfs = dfs.reset_index()
            dfs = dfs.pivot(index=repCol, 
                  columns="b_prune_typecheck", #"b_prune_typecheck", 
                  values=["tc_in","tc_in_s"])

            dfs = dfs.reset_index()
            # dfs.rename(index={repCol:"x"}, inplace=True)
            columns = ["x", "TC", "TC-Pruning", "TC-s", "TC-Pruning-s"]
            dfs.columns = columns
            dfs["x"] = dfs["x"].astype(int)
            dfs["speedup"] = dfs["TC"] / dfs["TC-Pruning"]
            dfs = dfs.set_index(["x"])
            

            if queryId == 1:
                  expName = "rule"
            else:
                  expName = "prunerate"

            print(dfs)
            csvName = csv_out_base + csv_out_typecheck_prefix + "_" + expName + ".csv"
            print("cat " + csvName)
            dfs.to_csv(csvName, sep=',', encoding='utf-8')

def normalize_by_column(df, val):
      contain_values = df[df.index.str.contains(val)]
      for i in range(0, df.shape[0]):
            df.iloc[i] = df.iloc[i] / contain_values

def various_datasets_new():
      print("=== Chart: various_datasets_new ===")
 
      columnNames =['datetime', 'graph', 'platform', 'viewtype', 'viewOrQuery', 'name', 'time', 'resultcount']

      index_dataset = {
            "view": ["lsqb_v1", "lsqb_v2",
                  "soc_v2", 
                  "prov_prov1",  "prov_prov1",
                  "word_v1", "word_v2", "word_v3",
                  "oag_v1"
            ],
            "query": ["lsqb_v1_q1", "lsqb_v1_q2", "lsqb_v2_q1", "lsqb_v1_q2",
                  "soc_v2_q1", "soc_v2_q2", 
                  "prov_prov1_q1",
                  "word_v1_q1", "word_v1_q2", "word_v1_q3", # Q10
                  "word_v2_q1", "word_v2_q2", "word_v2_q3",
                  "word_v3_q1", "word_v3_q2", "word_v3_q3", # Q16
                  "oag_v1_q1", "oag_v1_q2" # Q18
            ]
      }
      
      csv_set = ["logs/perflog.txt"]
      _df = get_df_from_csv_set(csv_set)
      _df = pd.DataFrame(_df.values, columns = columnNames )

      # views
      for viewOrQuery in ["view", "query"]:
            filter1 = _df["viewOrQuery"] == viewOrQuery
            _df_view = _df.where(filter1)
            _df_view = _df_view.dropna(how='all')
            print(_df_view)

            cols_groupby = ["platform", "name", "viewtype"]
            _df_view_grouped = _df_view.groupby(cols_groupby).agg(
                  time=("time", exp_mean), 
                  time_s=("time", exp_std),
            )
            print(_df_view_grouped)

            _df_view_grouped = _df_view_grouped.reset_index()

            _df_view_grouped = _df_view_grouped.pivot(index=["platform", "name"], 
                        columns="viewtype",
                        values=["time", "time_s"])

            dfs = _df_view_grouped

            normalize = True
            normalizeBase = 100
            normalizeBaseIndex = -1
            numOfBaseCols = len(dfs.columns)

            normPlatform = "lb"
            normalizedBy = "mv"
            normalizerIndex = -1
            normalizer = {}

            i2 = 0
            for c in dfs.columns:
                  if c[0] == 'time' and c[1] == normalizedBy:
                        normalizerIndex = i2
                        break
                  i2 = i2 + 1
            for row in dfs.itertuples(index=True):
                  cur_platform = row.Index[0]
                  cur_dataset = row.Index[1]
                  if cur_platform == normPlatform:
                        normalizer[cur_dataset] = row[normalizerIndex+1] 
            temp_df = []
            idx = 0
            for row in dfs.itertuples(index=True):
                  cur_platform = row.Index[0]
                  cur_dataset = row.Index[1]
                  for i in range(numOfBaseCols):    #(int)(numOfBaseCols/2)
                        if cur_dataset in normalizer:
                              dfs.iloc[idx, i] = dfs.iloc[idx, i] / normalizer[cur_dataset] * normalizeBase
                        else:
                              dfs.iloc[idx, i] = 1
                  idx = idx + 1

            dfs = dfs.reset_index()
            df = dfs

            for platform in platforms:
                  platform = platform.lower()
                  dfs = df
                  filter1 = dfs[("platform",'')] == platform
                  dfs = dfs.where(filter1)
                  dfs = dfs.dropna(how='all')
                  dfs = dfs.set_index(["name"])

                  dfs = dfs.drop([("platform",'')], axis=1)
                  dfs.index.names = ['x']

                  dfs = dfs.reindex(index_dataset[viewOrQuery])
                  newIndexColumn = []
                  for i in range(len(index_dataset[viewOrQuery])):
                        newIndexColumn.append(viewOrQuery[0:1] + str(i+1))
                  dfs.index = newIndexColumn

                  dfs.columns = dfs.columns.droplevel(0)
                  numOfPlatforms = (int)(len(dfs.columns)/2)
                  for i in range(numOfPlatforms, numOfPlatforms*2):
                        dfs.columns.values[i] = dfs.columns.values[i] + "err"

                  print("****viewOrQuery: " + viewOrQuery + " platform: ", platform)
                  print(dfs)
                  
                  print("cat " + csv_out_base + "viewDatasetsNew_" + platform + ".csv")
                  dfs.to_csv(csv_out_base + "viewDatasetsNew_" + platform + ".csv", sep=',', encoding='utf-8')



      # # queries
      # filter1 = _df["viewOrQuery"] == "query"
      # _df_view = _df.where(filter1)
      # _df_view = _df_view.dropna(how='all')
      # print(_df_view)

      # cols_groupby = ["platform", "name"]
      # _df_view_grouped = _df_view.groupby(cols_groupby).agg(
      #       avgtime=("time", exp_mean), 
      # )
      # print(_df_view_grouped)

      # dfs = dfs.dropna(how='all')
      # dfs = dfs.set_index(["graph"])
      # _df = _df.applymap(convertStringArrayIntoList)

      # print(_df)

def view_maintenance_ratio_set_platform():
      print("=== Chart: view_maintenance_ratio_set2 ===")

      csv_set = ["experiment/result_csv/result_ivm.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      for platform in platforms:
            dfs = df
            filter1 = dfs["platform"] == platform
            dfs = dfs.where(filter1)
            dfs = dfs.dropna(how='all')

            # df["t_createView"] = df["t_createView"].apply(np.sum)
            # df["t_createIndex"] = df["t_createIndex"].apply(np.sum)
            # df["t_query_s"] = df["t_query"].apply(np.std)
            dfs["t_query"] = dfs["t_query"].apply(np.mean)
            # df["t_total"] = df["t_createView"] + df["t_createIndex"] + df["t_query"]
            # df["t_total"] = df["t_query"]
            dfs["s_updateRatio"] = dfs["s_updateRatio"] / 100.0

            cols = [
                  "viewType", "s_updateRatio", "t_query", "queryId"
            ]
            filter1 = dfs["queryId"] == 3
            dfs = dfs[cols]
            dfs = dfs.where(filter1)
            dfs = dfs.dropna()

            cols_groupby = ["viewType", "s_updateRatio", "queryId"] #"b_use_substitute_index", 
            dfs = dfs.groupby(cols_groupby).agg(
                  t_query=("t_query", exp_mean), 
                  t_query_s=("t_query", exp_std), 
            )

            dfs = dfs.reset_index()

            dfs = dfs.drop(columns=["queryId"]) #, "b_use_substitute_index"

            dfs = dfs.pivot(index=["s_updateRatio"], #"b_use_substitute_index", 
                  columns=["viewType"],
                  values=["t_query", "t_query_s"])
                  
            # dfs = dfs.reset_index(level=["platform", "viewType", "graph", "queryId"]) #"b_use_substitute_index", 

            # df["index"] = df["platform"] + "-" + df["viewType"] #+ np.where(df["b_use_substitute_index"] == True, "(SI)", "")
            # dfs = dfs.set_index(["viewType"])
            # print(dfs)
            dfs.columns = dfs.columns.droplevel(0)
            numOfPlatforms = (int)(len(dfs.columns)/2)
            for i in range(numOfPlatforms, numOfPlatforms*2):
                  dfs.columns.values[i] = dfs.columns.values[i] + "err"
            dfs.index.names = ['x']
            print(dfs)

            print("cat " + csv_out_base + "ivm" + platform + ".csv")
            dfs.to_csv(csv_out_base + "ivm" + platform + ".csv", sep=',', encoding='utf-8')


def view_maintenance_ratio_set(querySet):
      print("=== Chart: view_maintenance_ratio_set2 ===")

      csv_set = ["experiment/result_csv/result_ivm.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      # df["t_createView"] = df["t_createView"].apply(np.sum)
      # df["t_createIndex"] = df["t_createIndex"].apply(np.sum)
      # df["t_query_s"] = df["t_query"].apply(np.std)
      df["t_query"] = df["t_query"].apply(np.mean)
      # df["t_total"] = df["t_createView"] + df["t_createIndex"] + df["t_query"]
      # df["t_total"] = df["t_query"]
      df["s_updateRatio"] = df["s_updateRatio"] / 100.0

      cols = [
            "viewType", "s_updateRatio", "t_query", "queryId"
      ]
      for querySetId in querySet:
            dfs = df
            filter1 = dfs["queryId"] == querySetId
            dfs = dfs[cols]
            dfs = dfs.where(filter1)
            dfs = dfs.dropna()

            cols_groupby = ["viewType", "s_updateRatio", "queryId"] #"b_use_substitute_index", 
            dfs = dfs.groupby(cols_groupby).agg(
                  t_query=("t_query", exp_mean), 
                  t_query_s=("t_query", exp_std), 
            )
 
            dfs = dfs.reset_index()

            dfs = dfs.drop(columns=["queryId"]) #, "b_use_substitute_index"
            # print(dfs)

            dfs = dfs.pivot(index=["s_updateRatio"], #"b_use_substitute_index", 
                  columns=["viewType"],
                  values=["t_query", "t_query_s"])
            
            # dfs = dfs.reset_index(level=["platform", "viewType", "graph", "queryId"]) #"b_use_substitute_index", 

            # df["index"] = df["platform"] + "-" + df["viewType"] #+ np.where(df["b_use_substitute_index"] == True, "(SI)", "")
            # dfs = dfs.set_index(["viewType"])
            # print(dfs)
            dfs.columns = dfs.columns.droplevel(0)
            numOfPlatforms = (int)(len(dfs.columns)/2)
            for i in range(numOfPlatforms, numOfPlatforms*2):
                  dfs.columns.values[i] = dfs.columns.values[i] + "err"
            dfs.index.names = ['x']
            print(dfs)

            print("cat " + csv_out_base + "ivm" + str(querySetId) + ".csv")
            dfs.to_csv(csv_out_base + "ivm" + str(querySetId) + ".csv", sep=',', encoding='utf-8')
      
      
def exp_mean(x):
      # if exp_check is True and len(x) != exp_iteration:
      #       raise Exception("pop_mean() needs an array of length "
      #             + str(exp_iteration) + ", but has " + str(len(x)) + ".")
      if isinstance(x, list):
            xlist = x
      else:
            xlist = x.to_list()

      return max(1.1, np.mean(dropMinMax(xlist)))
def exp_std(x):
      # if exp_check is True and len(x) != exp_iteration:
      #       raise Exception("pop_mean() needs an array of length "
      #             + str(exp_iteration) + ", but has " + str(len(x)) + ".")

      # return 0.1 at least -- log 0 is undefined
      if isinstance(x, list):
            xlist = x
      else:
            xlist = x.to_list()

      return np.std(dropMinMax(xlist))
      # return np.std(dropMinMax(x.to_list()))

def convertStringArrayIntoList(s):
      if type(s) == str and s[0] == "[":
            s = s.replace("true", "True")
            s = s.replace("false", "False")
            return eval(s)
      else:
            return s

def dropMinMax(s):
      if len(s) >= 3:
            min = np.min(s)
            max = np.max(s)
            s.remove(min)
            s.remove(max)

      # print(s)            
      # #FIXME            
      # if len(s) >= 3:
      #       max = np.max(s)
      #       s.remove(max)
      # #FIXME            
      # if len(s) >= 3:
      #       max = np.max(s)
      #       s.remove(max)
      return s

def first(lst): 
    return lst.to_list()[0]

def get_df_from_csv_set(csv_set):
      df = []
      for csv in csv_set:
            df_temp = pd.read_csv(csv, index_col=None, sep='|')
            df.append(df_temp)
      df = pd.concat(df, axis=0, ignore_index=True)

      return df

def get_df_from_csv(csv):
      df = pd.read_csv(csv, index_col=0, sep='|')
      return df

def array_mean_axis0(a):
      return list(np.mean(a.to_list(), axis=0))
def array_std_axis0(a):
      return list(np.std(a.to_list(), axis=0))








