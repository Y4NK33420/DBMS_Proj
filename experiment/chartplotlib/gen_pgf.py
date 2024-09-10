#!/usr/bin/env python3
#coding=utf8

import pandas as pd 
import numpy as np
import chartplotlib.plot as plp
import itertools

# sed -i s/'\\(\\displaystyle'/'\\sansmath \\(\\displaystyle'/g experiment/latex/images/*.pgf

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
index_dataset_parallel = ["SYN", "PROV", "OAG", "SOC", "WORD"]
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


def typecheck_plot():
      print("=== Plot: typecheck - static ===")

      df = get_df_from_csv(csv_out_base + csv_out_typecheck_prefix + "_static.csv")
      # print(df)
      
      df = df.drop(["TC-O", "TC-O-s"]) # added
      print(df)

      # assign values to draw bar chart
      data = df.iloc[0:2] # 0:3 added
      data_err = df.iloc[2:4] # 3:6 added

      xs = data.columns.to_list()
      ys = np.array(data.values.astype(float))
      axes = data.index.to_list()
      yerrs = data_err.values.astype(float)

      xlabel = "Graph dataset"
      ylabel = "Elapsed Time (msec)"

      colors = ["red", "blue"] #, "green"
      print(axes)
      plp.chart_bar_group(target_typecheck + "_static", xs, ys, axes, 
            yerrs=yerrs, 
            width=0.3, 
            capsize=3, 
            xlabel=xlabel, 
            ylabel=ylabel,
            colors=colors,
            alpha=0.8,
            ncol=1,
            figsize=set_size(1.0), #(8,3)
            )

      print("=== Plot: typecheck - dynamic ===")

      df = get_df_from_csv(csv_out_base + csv_out_typecheck_prefix + "_dynamic.csv")
      # print(df)
      
      # df = df.drop(["TC-O", "TC-O-s"]) # added
      df.loc['OVERHEAD'] = df.loc['CHECK'] / df.loc['BASE'] * 100
      df.loc['OVERHEAD-s'] = 1.1
      print(df)

      # assign values to draw bar chart
      data = df.iloc[4:5] # 0:3 added
      data_err = df.iloc[5:6] # 3:6 added
      # print(data)


      xs = data.columns.to_list()
      ys = np.array(data.values.astype(float))
      axes = data.index.to_list()
      yerrs = data_err.values.astype(float)

      xlabel = "Graph dataset"
      ylabel = "Overhead (%)"

      colors = ["red"] #, , "blue""green"
      print(axes)
      plp.chart_bar_group(target_typecheck + "_dynamic", xs, ys, axes, 
            yerrs=yerrs, 
            width=0.3, 
            capsize=3, 
            xlabel=xlabel, 
            ylabel=ylabel,
            colors=colors,
            alpha=0.8,
            ncol=1,
            figsize=set_size(1.0), #(8,3)
            yline=100.0,
            )


def typecheck():
      print("=== Chart: typecheck ===")

      csv_set = ["experiment/result_csv/result_" + csv_out_typecheck_prefix + ".csv"]
      
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      df["t_typecheckInput"] = df["t_typecheckInput"].apply(dropMinMax)
      df["t_typecheckInput"] = df["t_typecheckInput"].apply(np.mean)

      df["t_typecheckOutput"] = df["t_typecheckOutput"].apply(dropMinMax)
      df["t_typecheckOutput"] = df["t_typecheckOutput"].apply(np.mean)

      cols = [
            "graph", "b_prune_typecheck", 
            "t_typecheckInput", "t_typecheckOutput",
      ]
      df = df[cols]
      print(df)

      cols_groupby = ["graph","b_prune_typecheck"]
      df = df.groupby(cols_groupby).agg(
            tc_in=("t_typecheckInput", exp_mean), 
            tc_out=("t_typecheckOutput", exp_mean),
            tc_in_s=("t_typecheckInput", exp_std),
            tc_out_s=("t_typecheckOutput", exp_std),      
      )
      df = df.reset_index()
      df = df.pivot(index="graph", 
            columns="b_prune_typecheck", 
            values=["tc_in", "tc_out", "tc_in_s", "tc_out_s"])

      columns = [
            "TC-I", "TC-I-Pruning", "TC-O", "_TC-O", 
            "TC-I-s", "TC-I-Pruning-s", "TC-O-s", "_TC-O-s"]

      if len(df.columns) != len(columns):
            raise Exception("column length mismatch")
      df.columns = columns
      
      df = df.reindex(index_dataset_typecheck)
      df = df.fillna(0)
      df = df.reset_index(level='graph')
      df = df.set_index('graph')
      df = df.transpose()
      df = df.drop(['_TC-O', '_TC-O-s'])

      print(df)
      print()

      df.to_csv(csv_out_base + csv_out_typecheck_prefix + ".csv", sep='|', encoding='utf-8')

      typecheck_plot()

def scalability_plot():
      print("=== Plot: scalability ===")
      scalability_or_selectivity_plot(is_selectivity=False, platform="LB")
      scalability_or_selectivity_plot(is_selectivity=False, platform="PG")
      scalability_or_selectivity_plot(is_selectivity=False, platform="N4")

def selectivity_plot():
      print("=== Plot: selectivity ===")
      scalability_or_selectivity_plot(is_selectivity=True, platform="LB")
      scalability_or_selectivity_plot(is_selectivity=True, platform="PG")
      scalability_or_selectivity_plot(is_selectivity=True, platform="N4")

def scalability_or_selectivity_plot(is_selectivity=False, platform=None):
      if is_selectivity is False:
            base_target = target_scale
            group_column = "s_graph"
            csv_prefix = "scalability"
            xAxisEngFormat = True
            xlabel = "Graph size $|N|$"
            xscale = "log"
            xbase = 10
            yscale = "log"
      else:
            base_target = target_selectivity
            group_column = "s_selectivity"
            csv_prefix = "selectivity"
            xAxisEngFormat = False
            xlabel = "Selectivity Ratio ($\\times 10^{-2}$)"
            #  (${0 \\leq \\sigma \\leq 1}$)
            xscale = "log"
            xbase = 2
            yscale = "log"

      ################
      ## Draw View
      ################
      df = get_df_from_csv(csv_out_base + csv_prefix + ".csv")

      marker_mv = "o"
      marker_hv = "^"
      marker_vv = "x"
      marker_asr = "+"
      marker_ssr = "*"
      color_lb = "red"
      color_pg = "blue"
      color_n4 = "green"
      linestyle_vv = "--"
      linestyle_hv = "-."
      linestyle_mv = "-"
      linestyle_asr = ":"
      linestyle_ssr = "dashdot"

      alphas = [alpha_mv, alpha_hv, alpha_vv, alpha_asr, alpha_ssr,
            alpha_mv, alpha_hv, alpha_vv, alpha_asr, alpha_ssr,
            alpha_mv, alpha_hv, alpha_vv
            ]
      markers = [marker_mv, marker_hv, marker_vv, marker_asr, marker_ssr,
            marker_mv, marker_hv, marker_vv, marker_asr, marker_ssr,
            marker_mv, marker_hv, marker_vv
            ]
      colors = [color_lb, color_lb, color_lb, color_lb, color_lb,
            color_pg, color_pg, color_pg, color_pg, color_pg,
            color_n4, color_n4, color_n4
            ]
      linestyles = [linestyle_mv, linestyle_hv, linestyle_vv, linestyle_asr, linestyle_ssr,
            linestyle_mv, linestyle_hv, linestyle_vv, linestyle_asr, linestyle_ssr,
            linestyle_mv, linestyle_hv, linestyle_vv
            ]

      sizes = int((len(df.columns)-2)/2)

      # print(df)

      if platform is not None:
            filter1 = df["Unnamed: 2"] == platform
            df = df.where(filter1)
            df = df.dropna()
            print(df)

      data = df.iloc[:,2:2+sizes]
      data = data.applymap(sanitize)

      data = data.fillna(10) #FIXME
      data_err = df.iloc[:,2+sizes:2+sizes*2]
      data_err = data_err.applymap(sanitize)
      data_err = data_err.fillna(1) # FIXME

      xs = data.columns.astype(float).to_list()
      ys = np.array(data.values.astype(float))
      if platform is not None:
            # axes = data["Unnamed: 2"].index.to_list()
            axes = df.iloc[:,0]
      else:
            axes = data.index.to_list()
      yerrs = data_err.values.astype(float)

      ylabel = "Elapsed Time (msec)"

      # print("=====data ...========")
      # print(data)
      # print(data_err)
      # print(xs)
      # print(axes)

      chart_name = base_target + target_view
      if platform is not None:
            chart_name = chart_name + "_" + platform

      plp.chart_line(chart_name, ys, axes, xs,
            yerrs=yerrs,
            width=0.2, 
            xlabel=xlabel, 
            ylabel=ylabel, 
            ncol=3,
            alphas=alphas,
            markers=markers,
            linestyles=linestyles,
            colors=colors,
            capsize=2,
            dummylabelnum=2,
            xAxisEngFormat=xAxisEngFormat,
            xscale=xscale,
            xbase=xbase,
            yscale=yscale,
           )      

      ################
      ## Draw Query
      ################
      df = get_df_from_csv(csv_out_base + csv_prefix + "_query.csv")
      # print(df)

      if platform is not None:
            filter1 = df.index.str.startswith(platform)
            df = df[filter1]
            print(df)

      sizes = int((len(df.columns))/2)

      data = df.iloc[:,0:sizes]
      data = data.fillna(10) #FIXME
      data_err = df.iloc[:,sizes:sizes*2]
      data_err = data_err.fillna(1) # FIXME

      xs = data.columns.astype(float).to_list()
      ys = np.array(data.values.astype(float))
      if platform is not None:
            axes = data.index.to_list()
            axes_tmp = []
            for x in axes:
                  axes_tmp.append(x[3:])
            axes = axes_tmp
      else:
            axes = data.index.to_list()

      yerrs = data_err.values.astype(float)

      # print(data)
      # print(data_err)

      chart_name = base_target + target_query
      if platform is not None:
            chart_name = chart_name + "_" + platform

      plp.chart_line(chart_name, ys, axes, xs,
            yerrs=yerrs,
            width=0.2, 
            xlabel=xlabel, 
            ylabel=ylabel, 
            ncol=3,
            alphas=alphas,
            markers=markers,
            linestyles=linestyles,
            colors=colors,
            capsize=2,
            dummylabelnum=2,
            xAxisEngFormat=xAxisEngFormat,
            xscale=xscale,
            xbase=xbase,
            yscale=yscale,
            )

def scalability():
      print("=== Chart: scalability ===")
      scalability_or_selectivity(is_selectivity=False)
      # scalability_or_selectivity_plot(is_selectivity=False)

def scalability_or_selectivity(is_selectivity=False):
      if is_selectivity is False:
            base_target = target_scale
            group_column = "s_graph"
            csv_prefix = "scalability"
            xAxisEngFormat = True
            xlabel = "Graph size $|N|$"
            xscale = "log"
            xbase = 10
            yscale = "log"
      else:
            base_target = target_selectivity
            group_column = "s_selectivity"
            csv_prefix = "selectivity"
            xAxisEngFormat = False
            xlabel = "Selectivity Ratio ($\\times 10^{-2}$)"
            #  (${0 \\leq \\sigma \\leq 1}$)
            xscale = "log"
            xbase = 2
            yscale = "log"

      csv_set = ["experiment/result_csv/result_" + csv_prefix + ".csv"]
      _df = get_df_from_csv_set(csv_set)
      _df = _df.applymap(convertStringArrayIntoList)

      df = _df

      df["t_createView"] = df["t_createView"].apply(sum)
      sum_column = df["t_createView"] + df["t_createIndex"]
      df["t_createView"] = sum_column

      cols = [
            "graph", "platform", "viewType", group_column, "t_createView", 
            "t_createIndex",
      ]

      filter1 = df["graph"] == "SYN"
      # filter2 = df["b_use_substitute_index"] == True
      # filter3 = df["queryId"] == 1

      df = df.where(filter1)
      df = df.dropna()
      df = df[cols]

      # df["t_createView"] = df["t_createView"] / 1000.0

      cols_groupby = ["platform", "viewType", group_column]
      df = df.groupby(cols_groupby).agg(
            createView=("t_createView", exp_mean), 
            createView_s=("t_createView", exp_std),
            # createIndex=("t_createIndex", exp_mean), 
            # createIndex_s=("t_createIndex", exp_std),
      )

      print("### CREATE/INDEX ###")
      print(df)

      df = df.reset_index()
      df = df.pivot(index=["platform", "viewType"],
            columns=group_column, 
            values=["createView", "createView_s"])
            # values=["createView", "createView_s", "createIndex", "createIndex_s"])

      # print(df)

      df = df.reset_index(level='platform')
      df = df.reset_index(level='viewType')      
      df["index"] = df["platform"] + "-" + df["viewType"]
      df = df.set_index(["index"])


      df.columns = df.columns.droplevel(0)

      if is_selectivity is True:
            new_columns = []
            for i in range(len(df.columns)):
                  if isinstance(df.columns[i], int):
                        new_columns.insert(len(new_columns), df.columns[i] / 100.0)
                  else:
                        new_columns.insert(len(new_columns), df.columns[i])
            df.columns = new_columns            
      df = df.reindex(index_platform_viewtype)
      print(df)
      df.to_csv(csv_out_base + csv_prefix + ".csv", sep='|', encoding='utf-8')

      ylabel = "Elapsed Time (msec)"

      # ########
      # # Query
      # ########
      csv_set = ["experiment/result_csv/result_" + csv_prefix + "_query.csv"]
      _df = get_df_from_csv_set(csv_set)
      _df = _df.applymap(convertStringArrayIntoList)
      df = _df

      # print(df)

      df["t_query"] = df["t_query"].apply(dropMinMax)
      df["query"] = df["t_query"].apply(np.mean)
      df["query_s"] = df["t_query"].apply(np.std)

      cols = [
            "graph", "platform", "viewType", group_column, "queryId", 
            "query", "query_s", 
      ]
      filter1 = df["graph"] == "SYN"
      filter2 = df["queryId"] == 1
      df_no_index = df.where(filter1 & filter2)
      df_no_index = df_no_index.dropna()
      df_no_index = df_no_index[cols]

      cols_groupby = ["platform", "viewType", group_column]
      df_no_index = df_no_index.groupby(cols_groupby).agg(
            query=("query", first),
            query_s=("query_s", first)
      )

      df_no_index = df_no_index.reset_index(level=['platform','viewType', group_column])
      df_no_index["index"] = df_no_index["platform"] + "-" + df_no_index["viewType"]
      df_no_index = df_no_index.drop(columns=["platform", "viewType"])
      df_no_index = df_no_index.set_index(["index"])

      df = df_no_index
      df = df.pivot(
            columns=[group_column],
            values=["query", "query_s"])

      print("### QUERY ###")
      print(df)

      df.columns = df.columns.droplevel(0)
      df = df.reindex(index_platform_viewtype_si)

      if is_selectivity is True:
            new_columns = []
            for i in range(len(df.columns)):
                  if isinstance(df.columns[i], float):
                        new_columns.insert(len(new_columns), df.columns[i] / 100.0)
                  else:
                        new_columns.insert(len(new_columns), df.columns[i])
            df.columns = new_columns            
      df = df.reindex(index_platform_viewtype)

      print("====For query====")
      print(df)
      df.to_csv(csv_out_base + csv_prefix + "_query.csv", sep='|', encoding='utf-8')

def selectivity():
      print("=== Chart: selectivity ===")
      scalability_or_selectivity(is_selectivity=True)
      # scalability_or_selectivity_plot(is_selectivity=True)

def various_datasets_plot():
      # various_datasets_plot_process():
      various_datasets_plot_process(is_query=False, platform="LB", yscale='linear') #ylimMax=150000, 
      # various_datasets_plot_process(is_query=False, platform="PG", ylimMax=1250000, yscale='linear')
      # various_datasets_plot_process(is_query=False, platform="N4", ylimMax=50000, yscale='linear')
      various_datasets_plot_process(is_query=True, platform="LB", yscale='linear') #ylimMax=70000, 
      # various_datasets_plot_process(is_query=True, platform="PG", ylimMax=150000, yscale='linear')
      # various_datasets_plot_process(is_query=True, platform="N4", ylimMax=5000, yscale='linear')

def normalize_by_column(df, val):
      contain_values = df[df.index.str.contains(val)]
      for i in range(0, df.shape[0]):
            df.iloc[i] = df.iloc[i] / contain_values

def various_datasets_plot_process(is_query=False, platform=None, ylimMax=None, yscale='linear'):
      print("=== Plot: various_datasets ===")
      df = get_df_from_csv(csv_out_base + csv_out_various_prefix + ".csv")

      if platform is not None:
            filter1 = df["Unnamed: 2"] == platform
            df = df.where(filter1)
            df = df.dropna()
            # print(df)

      # print(df)
      # print("index_dataset: ", index_dataset)

      # assign values to draw bar chart
      sizes = int((len(df.columns)-2)/2)

      data = df.iloc[:,2:2+sizes]
      data = data.transpose()
      data = data.reindex(index_dataset)
      data = data.transpose()
      data = data.fillna(10) # FIXME

      data_err = df.iloc[:,2+sizes:2+sizes*2]
      data_err = data_err.transpose()
      data_err = data_err.reindex(index_dataset)
      data_err = data_err.transpose()
      data_err = data_err.fillna(10) # FIXME

      normalize_by_column(data, "MV")

      print(data)
      #exit()
      # print(data_err)

      xs = data.columns.to_list()
      ys = np.array(data.values.astype(float))

      if platform is not None:
            # axes = data["Unnamed: 2"].index.to_list()
            axes = df.iloc[:,0]
      else:
            axes = data.index.to_list()
      yerrs = data_err.values.astype(float)
      yerrs.fill(0)


      xlabel = "Graph dataset"
      ylabel = "Elapsed Time (msec)"

      marker_mv = "o"
      marker_hv = "^"
      marker_vv = "x"
      marker_asr = "*"
      marker_ssr = "*"
      color_lb = "red"
      color_pg = "blue"
      color_n4 = "green"
      linestyle_vv = "--"
      linestyle_hv = "-."
      linestyle_mv = "-"
      linestyle_asr = ":"
      linestyle_ssr = ":"
      alpha_mv = 1
      alpha_hv = 0.7
      alpha_vv = 0.4
      alpha_asr = 0.3
      alpha_ssr = 0.1
      hatch_mv = ""
      hatch_hv = "\\"
      hatch_vv = "x"
      hatch_asr = "+"
      hatch_ssr = "+"
      # ['-', '+', 'x', '\\', '*', 'o', 'O', '.']
      alphas = [alpha_mv, alpha_hv, alpha_vv, alpha_asr, alpha_ssr,
            alpha_mv, alpha_hv, alpha_vv, alpha_asr, alpha_ssr,
            alpha_mv, alpha_hv, alpha_vv
            ]
      markers = [marker_mv, marker_hv, marker_vv, marker_asr, marker_ssr,
            marker_mv, marker_hv, marker_vv, marker_asr, marker_ssr,
            marker_mv, marker_hv, marker_vv
            ]
      hatches = [hatch_mv, hatch_hv, hatch_vv, hatch_asr, hatch_ssr,
            hatch_mv, hatch_hv, hatch_vv, hatch_asr, hatch_ssr,
            hatch_mv, hatch_hv, hatch_vv
            ]            
      colors = [color_lb, color_lb, color_lb, color_lb, color_lb,
            color_pg, color_pg, color_pg, color_pg, color_pg,
            color_n4, color_n4, color_n4
            ]
      linestyles = [linestyle_mv, linestyle_hv, linestyle_vv, linestyle_asr, linestyle_ssr,
            linestyle_mv, linestyle_hv, linestyle_vv, linestyle_asr, linestyle_ssr,
            linestyle_mv, linestyle_hv, linestyle_vv
            ]
   
      chart_name = target_graphsets + target_view
      if platform is not None:
            chart_name = chart_name + "_" + platform

      if is_query is False:
            plp.chart_bar_group(chart_name, xs, ys, axes,  
                  yerrs=yerrs,
                  width=0.10, #0.07
                  xlabel=xlabel, 
                  ylabel=ylabel, 
                  ncol=3,
                  linestyles=linestyles,
                  colors=colors,
                  alphas=alphas,
                  hatches=hatches,
                  capsize=2,
                  yscale=yscale,
                  ylimMax=ylimMax,
                  ylimMin=0,
                  yline=1.0,
                  )

      #############
      ## QUERY
      #############
      if is_query is False:
            return

      df = get_df_from_csv(csv_out_base + csv_out_various_prefix + "_query.csv")
      # print(df)

      if platform is not None:
            filter1 = df.index.str.startswith(platform)
            df = df[filter1]
      # print(df)
      sizes = int((len(df.columns))/2)

      data = df.iloc[:,0:sizes]
      # data = data.transpose()
      # data = data.reindex(index_dataset)
      # data = data.transpose()
      data = data.fillna(1) # FIXME

      data_err = df.iloc[:,sizes:sizes*2]
      # data_err = data_err.transpose()
      # data_err = data_err.reindex(index_dataset)
      # data_err = data_err.transpose()
      data_err = data_err.fillna(1) # FIXME

      print(data)
      normalize_by_column(data, "MV")

      xs = data.columns.to_list()
      ys = np.array(data.values.astype(float))
      axes = data.index.to_list()
      yerrs = data_err.values.astype(float)
      yerrs.fill(0)

      # markers = [marker_mv, marker_hv, marker_vv, marker_si,
      #       marker_mv, marker_hv, marker_vv, marker_si,
      #       marker_mv
      #       ]
      # colors = [color_lb, color_lb, color_lb, color_lb,
      #       color_pg, color_pg, color_pg, color_pg,
      #       color_n4
      #       ]
      # linestyles = [linestyle_mv, linestyle_hv, linestyle_vv, linestyle_si,
      #       linestyle_mv, linestyle_hv, linestyle_vv, linestyle_si,
      #       linestyle_mv
      #       ]

      chart_name = target_graphsets + target_query + target_more
      if platform is not None:
            chart_name = chart_name + "_" + platform

      plp.chart_bar_group(chart_name, xs, ys, axes,  
            yerrs=yerrs,
            width=0.15, #0.07 
            xlabel=xlabel, 
            ylabel=ylabel, 
            ncol=3,
            linestyles=linestyles,
            colors=colors,
            alphas=alphas,
            hatches=hatches,
            capsize=2,
            #figsize=(10,6), #(20,6)
            xTickLabelRotation=45,
            ylimMax=ylimMax,
            ylimMin=0,
            yscale=yscale,
            yline=1.0,      
      )

      # print("### CREATE/INDEX ###")
      # print(df)

      # data = df.iloc[:,2+sizes*2:2+sizes*3]
      # data = data.transpose()
      # data = data.reindex(index_dataset)
      # data = data.transpose()
      # data = data.fillna(1) # FIXME

      # data_err = df.iloc[:,2+sizes*3:2+sizes*4]
      # data_err = data_err.transpose()
      # data_err = data_err.reindex(index_dataset)
      # data_err = data_err.transpose()
      # data_err = data_err.fillna(1) # FIXME

      # xs = data.columns.to_list()
      # ys = np.array(data.values.astype(float))
      # axes = data.index.to_list()
      # yerrs = data_err.values.astype(float)

      # plp.chart_bar_group(target_graphsets + target_index, xs, ys, axes,  
      #       yerrs=yerrs,
      #       width=0.08, 
      #       xlabel=xlabel, 
      #       ylabel=ylabel, 
      #       ncol=3,
      #       linestyles=linestyles,
      #       colors=colors,
      #       alphas=alphas,
      #       hatches=hatches,
      #       capsize=2,
      #       )

def various_datasets():
      print("=== Chart: various_datasets ===")
 
      csv_set = ["experiment/result_csv/result_" + csv_out_various_prefix + ".csv"]
      _df = get_df_from_csv_set(csv_set)
      _df = _df.applymap(convertStringArrayIntoList)

      df = _df

      df["t_createView"] = df["t_createView"].apply(sum)
      sum_column = df["t_createView"] + df["t_createIndex"]
      df["t_createView"] = sum_column

      cols = [
            "graph", "platform", "viewType", 
            "t_createView", 
      ]#"t_createIndex",


      #filter1 = df["b_use_substitute_index"] == False
      #filter2 = df["queryId"] == 0

      #df = df.where(filter1 & filter2)
      
      df = df.dropna()
      df = df[cols]
      cols_groupby = ["platform", "viewType", "graph"]
      df = df.groupby(cols_groupby).agg(
            createView=("t_createView", exp_mean), 
            createView_s=("t_createView", exp_std),
            # createIndex=("t_createIndex", exp_mean), 
            # createIndex_s=("t_createIndex", exp_std),
      )

      print("=======df==========")
      print(df)

      df = df.reset_index()
      df = df.pivot(index=["platform", "viewType"],
            columns="graph", 
            values=["createView", "createView_s"]) #, "createIndex", "createIndex_s"

      df = df.reset_index(level='platform')
      df = df.reset_index(level='viewType')      
      df["index"] = df["platform"] + "-" + df["viewType"]
      df = df.set_index(["index"])
      df.columns = df.columns.droplevel(0)
      df = df.reindex(index_platform_viewtype)

      df.to_csv(csv_out_base + csv_out_various_prefix + ".csv", sep='|', encoding='utf-8')
 
      ########
      # Query
      ########
      csv_set = ["experiment/result_csv/result_various_query.csv"]
      _df = get_df_from_csv_set(csv_set)
      _df = _df.applymap(convertStringArrayIntoList)

      # df["t_query"] = df["t_query"].apply(dropMinMax)
      # df["query"] = df["t_query"].apply(np.mean)
      # df["query_s"] = df["t_query"].apply(np.std)

      # cols = [
      #       "graph", "platform", "viewType", "queryId", 
      #       "query", "query_s", 
      # ]
      # filter1 = df["b_use_substitute_index"] == True
      # filter2 = df["queryId"] == 1
      # filter3 = df["viewType"] == "MV" # MAYBE FIXME
      # df_with_index = df.where(filter1 & filter2 & filter3)
      # df_with_index = df_with_index.dropna()
      # df_with_index = df_with_index[cols]
      # df_with_index["viewType"] = "SI"
  
      # cols_groupby = ["platform", "viewType", "graph", "queryId"]
      # df_with_index = df_with_index.groupby(cols_groupby).agg(
      #       query=("query", first),
      #       query_s=("query_s", first)
      # )
      # df_with_index = df_with_index.reset_index(level=['platform','viewType',  "graph", "queryId"])
      # df_with_index["index"] = df_with_index["platform"] + "-" + df_with_index["viewType"]
      # df_with_index = df_with_index.drop(columns=["platform", "viewType"])
      # df_with_index = df_with_index.set_index(["index"])

      # filter1 = df["b_use_substitute_index"] == False
      # filter2 = df["queryId"] == 1
      # df_no_index = df.where(filter1 & filter2)
      # df_no_index = df_no_index.dropna()
      # df_no_index = df_no_index[cols]

      # cols_groupby = ["platform", "viewType", "graph"]
      # df_no_index = df_no_index.groupby(cols_groupby).agg(
      #       query=("query", first),
      #       query_s=("query_s", first)
      # )

      # df_no_index = df_no_index.reset_index(level=['platform','viewType',  "graph"])
      # df_no_index["index"] = df_no_index["platform"] + "-" + df_no_index["viewType"]
      # df_no_index = df_no_index.drop(columns=["platform", "viewType"])
      # df_no_index = df_no_index.set_index(["index"])

      # df = pd.concat([df_with_index, df_no_index])
      # # df["queryset"] = df["graph"] + "-" + str(df["queryId"])
      # df = df.pivot(
      #       columns=["graph"],
      #       values=["query", "query_s"])
      # df = df.reindex(index_platform_viewtype_si)
      
      # df.columns = df.columns.droplevel(0)

      # sizes = int((len(df.columns))/2)

      # print("### QUERY ###")
      # print(df)
 
      # data = df.iloc[:,0:sizes]
      # data = data.transpose()
      # data = data.reindex(index_dataset)
      # data = data.transpose()
      # data = data.fillna(1) # FIXME

      # data_err = df.iloc[:,sizes:sizes*2]
      # data_err = data_err.transpose()
      # data_err = data_err.reindex(index_dataset)
      # data_err = data_err.transpose()
      # data_err = data_err.fillna(1) # FIXME

      # xs = data.columns.to_list()
      # ys = np.array(data.values.astype(float))
      # axes = data.index.to_list()
      # yerrs = data_err.values.astype(float)

      # alphas = [alpha_mv, alpha_hv, alpha_vv, alpha_si,
      #       alpha_mv, alpha_hv, alpha_vv, alpha_si,
      #       alpha_mv, alpha_hv, alpha_vv
      #       ]
      # markers = [marker_mv, marker_hv, marker_vv, marker_si,
      #       marker_mv, marker_hv, marker_vv, marker_si,
      #       marker_mv, marker_hv, marker_vv
      #       ]
      # colors = [color_lb, color_lb, color_lb, color_lb,
      #       color_pg, color_pg, color_pg, color_pg,
      #       color_n4, color_pg, color_pg
      #       ]
      # hatches = [hatch_mv, hatch_hv, hatch_vv, hatch_si,
      #       hatch_mv, hatch_hv, hatch_vv, hatch_si,
      #       hatch_mv, hatch_hv, hatch_vv
      #       ] 
      # linestyles = [linestyle_mv, linestyle_hv, linestyle_vv, linestyle_si,
      #       linestyle_mv, linestyle_hv, linestyle_vv, linestyle_si,
      #       linestyle_mv, linestyle_hv, linestyle_vv
      #       ]

      # plp.chart_bar_group(target_graphsets + target_query, xs, ys, axes,  
      #       yerrs=yerrs,
      #       width=0.08, 
      #       xlabel=xlabel, 
      #       ylabel=ylabel, 
      #       ncol=3,
      #       linestyles=linestyles,
      #       colors=colors,
      #       alphas=alphas,
      #       hatches=hatches,
      #       capsize=2,
      #       )

      #############
      # Query_More
      #############
      df = _df

      df["t_query"] = df["t_query"].apply(dropMinMax)
      df["query"] = df["t_query"].apply(np.mean)
      df["query_s"] = df["t_query"].apply(np.std)

      cols = [
            "graph", "platform", "viewType", "queryId", 
            "query", "query_s", 
      ]
  
      cols_groupby = ["platform", "viewType", "graph", "queryId"]

      # df_no_index = df.where(filter1)
      df_no_index = df.dropna()
      df_no_index = df_no_index[cols]

      cols_groupby = ["platform", "viewType", "graph", "queryId"]
      df_no_index = df_no_index.groupby(cols_groupby).agg(
            query=("query", first),
            query_s=("query_s", first)
      )

      df_no_index = df_no_index.reset_index(level=['platform','viewType',  "graph", "queryId"])
      df_no_index["index"] = df_no_index["platform"] + "-" + df_no_index["viewType"]
      df_no_index = df_no_index.drop(columns=["platform", "viewType"])
      df_no_index = df_no_index.set_index(["index"])

      df = df_no_index
      df = df.pivot(
            columns=["graph", "queryId"],
            values=["query", "query_s"])
      df.columns = df.columns.droplevel(0)

      new_columns = []
      for col in df.columns.values:
            x, y = col
            new_columns.insert(len(new_columns), x + "-Q" + str(int(y)))
      df.columns = new_columns
      df = df.reindex(index_platform_viewtype_si)
      # df.columns = df.columns.droplevel(0)

      df.to_csv(csv_out_base + csv_out_various_prefix + "_query.csv", sep='|', encoding='utf-8')

      print("### QUERY_MORE ###")
      print(df)
      print()

def parallel_plot():
      df = get_df_from_csv(csv_out_base + csv_out_parallel_prefix + ".csv")

      sizes = int((len(df.columns))/2)

      # assign values to draw bar chart
      data = df.iloc[:,0:sizes]
      data_err = df.iloc[:,sizes:sizes*2]

      # data = data.transpose()
      # data = data.reindex(index_dataset_parallel)
      # data = data.transpose()

      # data_err = data_err.transpose()
      # data_err = data_err.reindex(index_dataset_parallel)
      # data_err = data_err.transpose()

      data = data.fillna(1) # FIXME
      data_err = data_err.fillna(1) # FIXME

      xs = data.columns.to_list()
      ys = np.array(data.values.astype(float))
      axes = data.index.to_list()
      yerrs = data_err.values.astype(float)

      xlabel = "Rule sets with graph dataset"
      ylabel = "Elapsed Time (msec)"

      plp.chart_bar_group(target_parallel_view, xs, ys, axes, 
            yerrs=yerrs, 
            width=0.1, 
            capsize=3, 
            xlabel=xlabel, 
            ylabel=ylabel,
            ncol=1,
            figsize=set_size(1.0),
            )


def parallel():
      print("=== Chart: parallel ===")

      # SEQUENTIAL
      csv_set = ["experiment/result_csv/result_parallel.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      df["t_createView"] = df["t_createView"].apply(sum)
      df["t_typecheckInput"] = df["t_typecheckInput"].apply(sum)
      df["t_typecheckOutput"] = df["t_typecheckOutput"].apply(sum)
      cols = [
            "graph", "platform", "viewType", "b_sequential", "t_createView", 
            "t_createIndex", "t_typecheckInput", "t_typecheckOutput", "rulesetId"
      ]
      filter1 = df["b_sequential"] == True
      # filter1 = df["graph"] == "SYN"
      # filter2 = df["s_graph"] == 0
      # filter3 = df["b_use_substitute_index"] == False
      # filter4 = df["queryId"] == 1

      df = df.where(filter1)
      df = df.dropna()
      df = df[cols]

      df["graph"] = df["graph"] + "-" + df["rulesetId"].astype(int).astype(str)
      graphs = df["graph"].drop_duplicates()
      # print(graphs)
      # print(df)

      cols_groupby = ["platform", "viewType", "graph", "b_sequential"]
      df = df.groupby(cols_groupby).agg(
            createView=("t_createView", exp_mean), 
            # createIndex=("t_createIndex", exp_mean), 
            # tc_in=("t_typecheckInput", exp_mean), 
            # tc_out=("t_typecheckOutput", exp_mean), 
            createView_s=("t_createView", exp_std),
            # createIndex_s=("t_createIndex", exp_std),
            # tc_in_s=("t_typecheckInput", exp_std),
            # tc_out_s=("t_typecheckOutput", exp_std),
      )

      df = df.reset_index()
      df = df.pivot(index=["platform", "viewType", "b_sequential"], 
            columns="graph",
            values=["createView",
                  # , "createIndex", 
                  # "tc_in", "tc_out", 
                  "createView_s", 
                  # "createIndex_s",
                  # "tc_in_s", "tc_out_s"
                  ])
      df_seq = df
      # print(df_seq)

      # PARALLEL
      csv_set = ["experiment/result_csv/result_scalability.csv"]#, 
            #"experiment/result_csv/result_various.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)
      df["graph_rulesetid"] = df["graph"] + "-" + df["rulesetId"].astype(int).astype(str)

      filter1 = df["b_sequential"] == False
      filter2 = df["queryId"] == 0

      filter3 = df["graph"] == "SYN"
      filter4 = df["s_graph"] == 1000000
      # filter5 = df["s_selectivity"] == 100
      filter6 = df["graph"] != "SYN"
      filter7 = df["graph_rulesetid"].isin(graphs.to_list())
      
      # df = df.where(filter1 & filter2 & ((filter3 & filter4 & filter5) | filter6))
      df = df.where(filter1 & filter2 & filter4 & ((filter3) | filter6))
      df = df.where(filter7)
      df = df.dropna()
      df = df[cols]
      df["graph"] = df["graph"] + "-" + df["rulesetId"].astype(int).astype(str)

      # print(df)

      cols_groupby = ["platform", "viewType", "graph", "b_sequential"]
      df = df.groupby(cols_groupby).agg(
            createView=("t_createView", exp_mean), 
            createView_s=("t_createView", exp_std),
      )

      df = df.reset_index()
      df = df.pivot(index=["platform", "viewType", "b_sequential"], 
            columns="graph",
            values=["createView", "createView_s"])
      df_par = df
      
      print(df_par)

      df = pd.concat([df_seq, df_par])

      df = df.reset_index(level=['platform','viewType', "b_sequential"])
      
      df["index"] = df["platform"] + "-" + df["viewType"] + np.where(df["b_sequential"] == True, "(SEQ)", "(PAR)")
      df = df.drop(columns=["platform", "viewType", "b_sequential"])
      df = df.set_index(["index"])
      df.columns = df.columns.droplevel(0)      
      df = df.reindex(index_parallel)

      print(df)
     
      df.to_csv(csv_out_base + csv_out_parallel_prefix + ".csv", sep='|', encoding='utf-8')
      parallel_plot()

def view_maintenance(ratio, querySet, isAmortized):
      print("=== Chart: view_maintenance ===")

      if isAmortized == True:
            yscale = "log"
            xlabel = "Number of workloads"
            ylabel = "Amortized Average Time (msec)"
      else:
            yscale = "linear"
            yscale = "log"
            xlabel = "Number of workloads"
            ylabel = "Accumulated Elapsed Time (msec)"


      csv_set = ["experiment/result_csv/result_ivm.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      # print(df)

      # df["t_query"] = df["t_query"].apply(np.sum)
      df["t_createView"] = df["t_createView"].apply(np.sum)
      df["t_createIndex"] = df["t_createIndex"].apply(np.sum)
      cols = [
            "graph", "platform", "viewType", "b_use_substitute_index", 
            "t_createView", "t_createIndex", "t_query", "s_updateRatio", 
            "rulesetId", "elapsed_time"
      ]
      # filter1 = df["b_sequential"] == True
      filter1 = df["graph"] == "SYN"
      filter2 = df["s_updateRatio"].astype(int) == ratio
      filter3 = df["queryId"].astype(int) == querySet
      # filter3 = df["viewType"] != "VV"
      # filter3 = df["b_use_substitute_index"] == False
      # filter4 = df["queryId"] == 1
      df = df[cols]
      # df = df.where(filter1 & filter2 & filter3)
      df = df.where(filter1 & filter2 & filter3)
      df = df.dropna()

      # print(df)

      # print("len: ", len(df["t_query"]))
      df_new = pd.DataFrame()
      len_query = 0
      for index, row in df.iterrows():
            len_query = len(df["t_query"][index])
            cum = list(itertools.accumulate(row['t_query']))
            
            for i in range(0,len(cum)):
                  if isAmortized == True:
                        cum[i] = (cum[i]+row["t_createView"]+row["t_createIndex"])/(i+1)
                  else:
                        cum[i] = (cum[i]+row["t_createView"]+row["t_createIndex"])


            if len(row["t_query"]) > 0:
                  df_new = df_new.append({
                        "graph": row["graph"],
                        "platform": row["platform"],
                        "viewType": row["viewType"],
                        "elapsed_time": row["elapsed_time"],
                        "b_use_substitute_index": row["b_use_substitute_index"],
                        "t_createView": row["t_createView"],
                        "t_createIndex": row["t_createIndex"],
                        "rulesetId": row["rulesetId"],
                        "s_updateRatio": row["s_updateRatio"],
                        "cum": cum}, ignore_index=True)

      cols_groupby = ["platform", "viewType", "graph", "s_updateRatio", "b_use_substitute_index"]
      df_new = df_new.groupby(cols_groupby).agg(
            cum=("cum", array_mean_axis0), 
            cum_s=("cum", array_std_axis0), 
            # cum_s=("cum", np.std), 
      )

      df_new = df_new.reset_index(level=["platform", "viewType", "graph", "b_use_substitute_index"])
      
      # df_new.columns = df_new.columns.droplevel(0)      
      df_new["index"] = df_new["platform"] + "-" + df_new["viewType"] + np.where(df_new["b_use_substitute_index"] == True, "(SI)", "")
      # print(df_new)
      # exit(0)

      # df_new = df_new.drop(columns=["platform", "viewType", "graph", "b_use_substitute_index"])
      df_new = df_new.set_index(["index"])

      axes = ['LB-MV', 'LB-MV(SI)', 'LB-HV', 'LB-HV(SI)'] #, 'LB-VV', 'LB-VV(SI)']
      df_new = df_new.reindex(axes)

      # "t_createView", "elapsed_time", 
      # print(df_new[["platform","viewType", "b_use_substitute_index"]])
      xs = pd.Series(range(1,len_query+1)).to_list()
      print("xs: ", xs)
     
      print("cum: ", df_new["cum"])
      # print("===============")
      # print(df_new["cum"])
      ys = df_new["cum"].to_list()
      # print(xs)
      # print(ys)
      # axes = ['LB-MV', 'LB-MV(SI)', 'LB-HV', 'LB-HV(SI)'] #, 'LB-VV', 'LB-VV(SI)']
      # print(axes)
      yerrs = None #df_new["cum_s"].to_list()
      # # ys1 = ys.cumsum()
      
      # # print(ys1)


      # print(df)

      # df["total_time"] = df["t_query"] + df["t_createView"]


      # cols_groupby = ["platform", "viewType", "graph", "b_use_substitute_index", "s_updateRatio"]
      # df = df.groupby(cols_groupby).agg(
      #       query=("total_time", exp_mean), 
      #       query_s=("total_time", exp_std),
      # )

      # df = df.reset_index()
      # df = df.pivot(index=["platform", "viewType", "graph", "b_use_substitute_index"], 
      #       columns="s_updateRatio",
      #       values=["query", "query_s"])

      # df = df.reset_index(level=["platform", "viewType", "graph", "b_use_substitute_index"])

      # df["index"] = df["platform"] + "-" + df["viewType"] + np.where(df["b_use_substitute_index"] == True, "(SI)", "")
      # # df = df.drop(columns=["platform", "viewType", "b_use_substitute_index"])
      # df = df.drop(columns=["platform", "viewType", "graph", "b_use_substitute_index"])
      # df = df.set_index(["index"])
      # df.columns = df.columns.droplevel(0)      
      # df = df.reindex(index_ivm)

      # new_columns = []
      # for i in range(len(df.columns)):
      #       if isinstance(df.columns[i], int):
      #             new_columns.insert(len(new_columns), df.columns[i] / 100.0)
      #       else:
      #             new_columns.insert(len(new_columns), df.columns[i])
      # df.columns = new_columns

      # print(df)

      # xs = df["t_query"].to_list()


      # print(xs)

      # sizes = int((len(df.columns))/2)

      # # assign values to draw bar chart
      # data = df.iloc[:,0:sizes]
      # data_err = df.iloc[:,sizes:sizes*2]

      # # data = data.transpose()
      # # data = data.reindex(index_dataset_parallel)
      # # data = data.transpose()

      # # data_err = data_err.transpose()
      # # data_err = data_err.reindex(index_dataset_parallel)
      # # data_err = data_err.transpose()

      # data = data.fillna(1) # FIXME
      # data_err = data_err.fillna(1) # FIXME

      # xs = data.columns.to_list()

      # ys = np.array(data.values.astype(float))
      # axes = data.index.to_list()
      # yerrs = data_err.values.astype(float)


      # print(xs)
      # print(ys)
      # print(axes)
      # print(yerrs)

      plp.chart_line(target_ivm + "_query" + str(ratio) + "%_setId" + str(querySet), ys, axes, xs,
            yerrs=yerrs,
            # width=0.2, 
            xlabel=xlabel, 
            ylabel=ylabel, 
            ncol=1,
            # alphas=alphas,
            # markers=markers,
            # linestyles=linestyles,
            # colors=colors,
            capsize=2,
            # dummylabelnum=3,
            # xAxisEngFormat=xAxisEngFormat,
            yscale=yscale,
            # ylimMin=0,
            # ylimMax=500,
            )


def view_maintenance_ratio(querySet):
      print("=== Chart: view_maintenance_ratio ===")

      yscale = "log"
      xlabel = "Query ratio (${0 \\leq \\gamma \\leq 100}$)"
      ylabel = "Accumulated Elapsed Time (msec)"

      csv_set = ["experiment/result_csv/result_ivm.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      # print(df)

      # df["t_query"] = df["t_query"].apply(np.sum)
      df["t_createView"] = df["t_createView"].apply(np.sum)
      df["t_createIndex"] = df["t_createIndex"].apply(np.sum)
      df["t_query"] = df["t_query"].apply(np.mean)
      # df["t_total"] = df["t_createView"] + df["t_createIndex"] + df["t_query"]
      df["t_total"] = df["t_query"]

      cols = [
            "graph", "platform", "viewType", "b_use_substitute_index", 
            "t_total", "s_updateRatio",
            "t_createView", "t_createIndex", "t_query",
            "rulesetId", "elapsed_time"
      ]
      filter1 = df["graph"] == "SYN"
      filter2 = df["queryId"].astype(int) == querySet
      df = df[cols]
      df = df.where(filter1 & filter2)
      df = df.dropna()

      print(df)

      cols_groupby = ["platform", "viewType", "graph", "s_updateRatio", "b_use_substitute_index"]
      df = df.groupby(cols_groupby).agg(
            t_total=("t_total", exp_mean), 
            t_total_s=("t_total", exp_std), 
      )

      print(df)

      df = df.reset_index()
      df = df.pivot(index=["platform", "viewType", "graph", "b_use_substitute_index"], 
            columns="s_updateRatio",
            values=["t_total", "t_total_s"])
      print(df)

      df = df.reset_index(level=["platform", "viewType", "graph", "b_use_substitute_index"])

      df["index"] = df["platform"] + "-" + df["viewType"] + np.where(df["b_use_substitute_index"] == True, "(SI)", "")
      df = df.drop(columns=["platform", "viewType", "graph", "b_use_substitute_index"])
      df = df.set_index(["index"])

      print(df)
      # exit(0)

      # print("len: ", len(df["t_query"]))
      # df_new = pd.DataFrame()
      # len_query = 0
      # for index, row in df.iterrows():
      #       len_query = len(df["t_query"][index])
      #       cum = list(itertools.accumulate(row['t_query']))
            
      #       for i in range(0,len(cum)):
      #             if isAmortized == True:
      #                   cum[i] = (cum[i]+row["t_createView"]+row["t_createIndex"])/(i+1)
      #             else:
      #                   cum[i] = (cum[i]+row["t_createView"]+row["t_createIndex"])

      #       if len(row["t_query"]) > 0:
      #             df_new = df_new.append({
      #                   "graph": row["graph"],
      #                   "platform": row["platform"],
      #                   "viewType": row["viewType"],
      #                   "elapsed_time": row["elapsed_time"],
      #                   "b_use_substitute_index": row["b_use_substitute_index"],
      #                   "t_createView": row["t_createView"],
      #                   "t_createIndex": row["t_createIndex"],
      #                   "rulesetId": row["rulesetId"],
      #                   "s_updateRatio": row["s_updateRatio"],
      #                   "cum": cum}, ignore_index=True)

      # print(df_new[["platform","viewType", "t_createView", "elapsed_time", "b_use_substitute_index"]])
      # xs = pd.Series(range(1,len_query+1)).to_list()

      sizes = int(len(df.columns)/2)

      axes = ['LB-MV', 'LB-MV(SI)', 'LB-HV', 'LB-HV(SI)'] #, 'LB-VV', 'LB-VV(SI)']
      df = df.reindex(axes)

      df.columns = df.columns.droplevel(0)      
      xs = df.columns.to_list()[:sizes]
      # print(xs)
     
      # print(df_new["cum"])
      # print("===============")
      # print(df_new["cum"])
      # print(df)
      # exit(0)
      
      print(sizes)
      data = df.iloc[:,0:sizes]
      ys = np.array(data.values.astype(int))

      data_err = df.iloc[:,sizes:len(df.columns)]
      yerrs = np.array(data_err.values.astype(float))


      print("xs: ", xs)
      print("ys: ", ys)
      print("yerrs: ", yerrs)

      print("axes: ", axes)
      # # ys1 = ys.cumsum()
  
      marker_mv = "o"
      marker_hv = "^"
      marker_vv = "x"
      marker_si = "*"
      color_lb = "red"
      color_pg = "blue"
      color_n4 = "green"
      linestyle_vv = "--"
      linestyle_hv = "-."
      linestyle_mv = "-"
      linestyle_si = ":"

      markers = [marker_mv, marker_si, marker_hv, marker_si]
      colors = [color_lb, color_lb, color_pg, color_pg]
      linestyles = [linestyle_mv, linestyle_si, linestyle_hv, linestyle_si]

      plp.chart_line(target_ivm + "_ratio_setId_" + str(querySet), ys, axes, xs,
            yerrs=yerrs,
            # width=0.2, 
            xlabel=xlabel, 
            ylabel=ylabel, 
            ncol=1,
            # alphas=alphas,
            markers=markers,
            linestyles=linestyles,
            colors=colors,
            capsize=2,
            # dummylabelnum=3,
            # xAxisEngFormat=xAxisEngFormat,
            yscale=yscale,
            figsize=set_size(1.0), #0.33, ratio=3), #(8,3)
            # ylimMin=0,
            # ylimMax=500,
            # subplot_x=1,
            # subplot_y=2,
            )


def view_maintenance_ratio_set_plot(querySet):
      yscale = "linear"
      xlabel = "Query ratio (${0 \\leq \\gamma \\leq 1}$)"
      ylabel = "Average Time per Operation (msec)"

      print("==== Plot: view_maintenance_ration_set ====")
      df = get_df_from_csv(csv_out_base + csv_out_ivm_prefix + ".csv")

      xs = []
      ys = []
      yerrs = []

      axes = index_ivm

      for r in querySet:
            _df = df[df["queryId"] == r]
            _df = _df.drop(columns=["queryId"])

            # print(_df)
            # print("index_ivm: ", index_ivm)
            sizes = int(len(_df.columns)/2)

            # axes = [axes1, axes2]
            # print(axes)

            # print("========1234=====sizes: ", sizes)
            # print(_df)
            # _df.columns = _df.columns.droplevel(0)      
            # _df = _df.reset_index()
            # print(_df)
            # _df = _df.transpose()
            # print(_df)
            # _df = _df.reindex(index_ivm)
            # _df_new = _df.copy()
            # _df_new["queryId"] = r

            # df_save = pd.concat([df_save, _df_new])
            # _df = _df.transpose()
            # print("========5678=====")
            # print(df)
            # print(_df)

            # _df.columns = _df.columns.droplevel(0)      
            _xs = _df.columns.to_list()[:sizes]
            print(_xs)
            _xs = [float(x) / 100.0 for x in _xs]

            data = _df.iloc[:,0:sizes]
            data = data.fillna(10) # FIXME
            _ys = np.array(data.values.astype(int))

            data_err = _df.iloc[:,sizes:len(_df.columns)]
            data_err = data_err.fillna(1) # FIXME
            _yerrs = np.array(data_err.values.astype(float))

            xs.append(_xs)
            ys.append(_ys)
            yerrs.append(_yerrs)

      marker_mv = "o"
      marker_hv = "^"
      marker_vv = "x"
      marker_asr = "+"
      marker_ssr = "*"
      color_lb = "red"
      color_pg = "blue"
      color_n4 = "green"
      linestyle_vv = "--"
      linestyle_hv = "-."
      linestyle_mv = "-"
      linestyle_asr = ":"
      linestyle_ssr = "dashed"

      markers = [marker_mv, marker_hv, marker_vv, marker_asr, marker_ssr]
      colors = [color_lb, color_lb, color_lb, color_lb, color_lb]
      linestyles = [linestyle_mv, linestyle_hv, linestyle_vv, linestyle_asr, linestyle_ssr]

      plp.chart_line_subplots(target_ivm + "_ratio_set", ys, axes, xs,
            yerrs=yerrs,
            # width=0.2, 
            xlabel=xlabel, 
            ylabel=ylabel, 
            ncol=1,
            # alphas=alphas,
            markers=markers,
            linestyles=linestyles,
            colors=colors,
            capsize=2,
            # dummylabelnum=3,
            # xAxisEngFormat=xAxisEngFormat,
            yscale=yscale,
            figsize=set_size(1.0), #0.33, ratio=3), #(8,3)
            # ylimMin=0,
            # ylimMax=500,
            subplot_x=1,
            subplot_y=3,
      )


def view_maintenance_ratio_set(querySet):
      print("=== Chart: view_maintenance_ratio_set ===")

      csv_set = ["experiment/result_csv/result_ivm.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      # print(df)

      df["t_createView"] = df["t_createView"].apply(np.sum)
      df["t_createIndex"] = df["t_createIndex"].apply(np.sum)
      df["t_query"] = df["t_query"].apply(np.mean)
      # df["t_total"] = df["t_createView"] + df["t_createIndex"] + df["t_query"]
      df["t_total"] = df["t_query"]

      cols = [
            "graph", "platform", "viewType", #"b_use_substitute_index", 
            "t_total", "s_updateRatio",
            "t_createView", "t_createIndex", "t_query",
            "rulesetId", "elapsed_time", "queryId"
      ]
      filter1 = df["graph"] == "SYN"
      filter2 = df["queryId"].astype(int).isin(querySet)
      df = df[cols]
      df = df.where(filter1 & filter2)
      df = df.dropna()

      cols_groupby = ["platform", "viewType", "graph", "s_updateRatio", "queryId"] #"b_use_substitute_index", 
      df = df.groupby(cols_groupby).agg(
            t_total=("t_total", exp_mean), 
            t_total_s=("t_total", exp_std), 
      )

      # df["s_updateRatio"] = df["s_updateRatio"] / 100.0
      df = df.reset_index()
      df = df.pivot(index=["platform", "viewType", "graph", "queryId"], #"b_use_substitute_index", 
            columns="s_updateRatio",
            values=["t_total", "t_total_s"])

      df = df.reset_index(level=["platform", "viewType", "graph", "queryId"]) #"b_use_substitute_index", 

      df["index"] = df["platform"] + "-" + df["viewType"] #+ np.where(df["b_use_substitute_index"] == True, "(SI)", "")
      df = df.drop(columns=["platform", "viewType", "graph"]) #, "b_use_substitute_index"
      df = df.set_index(["index"])

      # print(df)
      # exit(0)

      xs = []
      ys = []
      yerrs = []

      axes = index_ivm

      df_save = pd.DataFrame([])

      for r in querySet:
            _df = df[df["queryId"] == r]
            _df = _df.drop(columns=["queryId"])

            # print(_df)
            # print("index_ivm: ", index_ivm)
            sizes = int(len(_df.columns)/2)


            # axes = [axes1, axes2]
            # print(axes)

            # print("========1234=====")
            # print(_df)
            _df.columns = _df.columns.droplevel(0)      
            #_df = _df.reset_index()
            # print(_df)
            # _df = _df.transpose()
            # print(_df)
            _df = _df.reindex(index_ivm)
            _df_new = _df.copy()
            _df_new["queryId"] = r

            df_save = pd.concat([df_save, _df_new])
            # _df = _df.transpose()
            # print("========5678=====")
            # print(df)
            # print(_df)

            # _df.columns = _df.columns.droplevel(0)      
            _xs = _df.columns.to_list()[:sizes]
            _xs = [x / 100.0 for x in _xs]

            data = _df.iloc[:,0:sizes]
            data = data.fillna(10) # FIXME
            _ys = np.array(data.values.astype(int))

            data_err = _df.iloc[:,sizes:len(_df.columns)]
            data_err = data_err.fillna(1) # FIXME
            _yerrs = np.array(data_err.values.astype(float))

            xs.append(_xs)
            ys.append(_ys)
            yerrs.append(_yerrs)
      # exit(0)

      print("===data===")
      print(df_save)
      df_save.to_csv(csv_out_base + csv_out_ivm_prefix + ".csv", sep='|', encoding='utf-8')




def view_maintenance_old():
      print("=== Chart: view_maintenance ===")

      csv_set = ["experiment/result_csv/result_ivm.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      # print(df)

      df["t_query"] = df["t_query"].apply(np.sum)
      df["t_createView"] = df["t_createView"].apply(np.sum)
      cols = [
            "graph", "platform", "viewType", "b_use_substitute_index", "t_createView", "t_query", "s_updateRatio"
      ]
      # filter1 = df["b_sequential"] == True
      filter1 = df["graph"] == "SYN"
      # filter2 = df["s_graph"] == 0
      # filter3 = df["b_use_substitute_index"] == False
      # filter4 = df["queryId"] == 1


      df = df[cols]
      df = df.where(filter1)
      df = df.dropna()
      df["total_time"] = df["t_query"] + df["t_createView"]

      # print(df)

      cols_groupby = ["platform", "viewType", "graph", "b_use_substitute_index", "s_updateRatio"]
      df = df.groupby(cols_groupby).agg(
            query=("total_time", exp_mean), 
            query_s=("total_time", exp_std),
      )

      df = df.reset_index()
      df = df.pivot(index=["platform", "viewType", "graph"], #, "b_use_substitute_index"
            columns="s_updateRatio",
            values=["query", "query_s"])

      df = df.reset_index(level=["platform", "viewType", "graph"]) #, "b_use_substitute_index"

      df["index"] = df["platform"] + "-" + df["viewType"] # + np.where(df["b_use_substitute_index"] == True, "(SI)", "")
      # df = df.drop(columns=["platform", "viewType", "b_use_substitute_index"])
      df = df.drop(columns=["platform", "viewType", "graph"]) #, "b_use_substitute_index"
      df = df.set_index(["index"])
      df.columns = df.columns.droplevel(0)      
      df = df.reindex(index_ivm)

      new_columns = []
      for i in range(len(df.columns)):
            if isinstance(df.columns[i], int):
                  new_columns.insert(len(new_columns), df.columns[i] / 100.0)
            else:
                  new_columns.insert(len(new_columns), df.columns[i])
      df.columns = new_columns

      print(df)

      sizes = int((len(df.columns))/2)

      # assign values to draw bar chart
      data = df.iloc[:,0:sizes]
      data_err = df.iloc[:,sizes:sizes*2]

      # data = data.transpose()
      # data = data.reindex(index_dataset_parallel)
      # data = data.transpose()

      # data_err = data_err.transpose()
      # data_err = data_err.reindex(index_dataset_parallel)
      # data_err = data_err.transpose()

      data = data.fillna(1) # FIXME
      data_err = data_err.fillna(1) # FIXME

      xs = data.columns.to_list()

      ys = np.array(data.values.astype(float))
      axes = data.index.to_list()
      yerrs = data_err.values.astype(float)

      xlabel = "Query/Workload Ratio"
      ylabel = "Elapsed Time (msec)"

      # print(xs)
      # print(ys)
      # print(axes)
      # print(yerrs)

      plp.chart_line(target_ivm, ys, axes, xs,
            yerrs=yerrs,
            # width=0.2, 
            xlabel=xlabel, 
            ylabel=ylabel, 
            ncol=1,
            # alphas=alphas,
            # markers=markers,
            # linestyles=linestyles,
            # colors=colors,
            capsize=2,
            # dummylabelnum=3,
            # xAxisEngFormat=xAxisEngFormat,
            # yscale="log",
            )

def test(*iteratables, second=None, third=None):
      if not iteratables:
        return

      print("iteratables: ", iteratables)
      if second:
            print("second: ", second)
      if third:
            print("third: ", third)
      
def exp_mean(x):
      # if exp_check is True and len(x) != exp_iteration:
      #       raise Exception("pop_mean() needs an array of length "
      #             + str(exp_iteration) + ", but has " + str(len(x)) + ".")
      return max(1.1, np.mean(dropMinMax(x.to_list())))
def exp_std(x):
      # if exp_check is True and len(x) != exp_iteration:
      #       raise Exception("pop_mean() needs an array of length "
      #             + str(exp_iteration) + ", but has " + str(len(x)) + ".")

      # return 0.1 at least -- log 0 is undefined
      return max(np.std(dropMinMax(x.to_list())), 1.1)
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


















def view_maintenance_ratio_23():
      print("=== Chart: view_maintenance_ratio_set2 ===")

      csv_set = ["experiment/result_csv/final_ivm.csv"]
      df = get_df_from_csv_set(csv_set)
      df = df.applymap(convertStringArrayIntoList)

      print(df)

      workloads = ["C", "T1", "T2"]

  
      # # df["s_updateRatio"] = df["s_updateRatio"] / 100.0
      # df = df.reset_index()
      # df = df.pivot(index=["platform"], #"b_use_substitute_index", 
      #       columns="ratio",
      #       values=["time"])

      # df = df.reset_index(level=["platform", "viewType", "graph", "queryId"]) #"b_use_substitute_index", 

      # df["index"] = df["platform"] + "-" + df["viewType"] #+ np.where(df["b_use_substitute_index"] == True, "(SI)", "")
      # df = df.drop(columns=["platform", "viewType", "graph"]) #, "b_use_substitute_index"
      # df = df.set_index(["index"])

      # print(df)
      # exit(0)

      xs = []
      ys = []
      yerrs = []

      index_ivm2 = ["LB-MV", "LB-SSR", "PG-MV", "PG-SSR", "N4-OV"]

      axes = index_ivm2

      df_save = pd.DataFrame([])

      for workload in workloads:
            _df = df[df["workload"] == workload]
            _df = _df.drop(columns=["workload"])

            print (_df)

            _df = _df.reset_index()
            _df = _df.pivot(index=["platform",], 
                  columns="ratio",
                  values=["time"])
            print(_df)
            # exit(0)

            # _df = _df.reset_index(level=["platform"])

            # _df["index"] = _df["platform"] # + "-" + df["viewType"] + np.where(df["b_use_substitute_index"] == True, "(SI)", "")
            # # df = df.drop(columns=["platform", "viewType", "graph", "b_use_substitute_index"])
            # _df = _df.set_index(["index"])


            _df.columns = _df.columns.droplevel(0)      
            _df = _df.reindex(index_ivm2)
            print (_df)
            _df_new = _df.copy()
            # _df_new["workload"] = workload

            print(_df_new)

            df_save = pd.concat([df_save, _df_new])
            _xs = _df.columns.to_list()
            # _xs = [x / 100.0 for x in _xs]

            data = _df
            data = data.fillna(1) # FIXME
            _ys = np.array(data.values.astype(int))

            # _xs = np.concatenate(_xs).ravel().tolist()

            yerrs.append(_ys)
            xs.append(_xs)
            ys.append(_ys)

            # xs = xs[0]
            print(xs)
            print(ys)

      yscale = "linear"
      xlabel = "Query ratio (${0 \\leq \\gamma \\leq 1}$)"
      ylabel = "Average Time per Operation (msec)"

      print("==== Plot: view_maintenance_ration_set ====")
      # df = get_df_from_csv(csv_out_base + csv_out_ivm_prefix + ".csv")

      # xs = []
      # ys = []
      # yerrs = []

      axes = index_ivm2

      # _df = df[df["queryId"] == r]
      # _df = _df.drop(columns=["queryId"])

      # print(_df)
      # print("index_ivm: ", index_ivm)
      # sizes = int(len(_df.columns)/2)

      # axes = [axes1, axes2]
      # print(axes)

      # print("========1234=====sizes: ", sizes)
      # print(_df)
      # _df.columns = _df.columns.droplevel(0)      
      # _df = _df.reset_index()
      # print(_df)
      # _df = _df.transpose()
      # print(_df)
      # _df = _df.reindex(index_ivm)
      # _df_new = _df.copy()
      # _df_new["queryId"] = r

      # df_save = pd.concat([df_save, _df_new])
      # _df = _df.transpose()
      # print("========5678=====")
      # print(df)
      # print(_df)

      # _df.columns = _df.columns.droplevel(0)      
      # _xs = _df.columns.to_list()
      # print(_xs)
      # _xs = [float(x) / 100.0 for x in _xs]

      # data = _df.iloc[:,0:1]
      # # data = data.fillna(10) # FIXME
      # _ys = np.array(data.values.astype(int))
      # print (_ys)


      # exit(0)
      # data_err = _df.iloc[:,sizes:len(_df.columns)]
      # data_err = data_err.fillna(1) # FIXME
      # _yerrs = np.array(data_err.values.astype(float))

      # xs.append(_xs)
      # ys.append(_ys)

      yscale = "linear"

      plp.chart_line_subplots("here", 
            ys, axes, xs,
            yerrs=yerrs,
            # width=0.2, 
            xlabel="XLABEL", 
            ylabel="YLABEL", 
            ncol=1,
            # alphas=alphas,
            # markers=markers,
            # linestyles=linestyles,
            # colors=colors,
            capsize=2,
            # dummylabelnum=3,
            # xAxisEngFormat=xAxisEngFormat,
            yscale=yscale,
            figsize=set_size(1.0), #0.33, ratio=3), #(8,3)
            # ylimMin=0,
            # ylimMax=500,
            subplot_x=1,
            subplot_y=3,
      )            

