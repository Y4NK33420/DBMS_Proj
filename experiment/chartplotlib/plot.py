import pandas as pd 
import numpy as np
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.pyplot import figure
from pylab import rcParams
import matplotlib.font_manager as font_manager
import matplotlib.ticker
from matplotlib.patches import Ellipse, Polygon
from matplotlib.backends.backend_pdf import PdfPages

pdf = PdfPages('multipage_pdf.pdf')

# matplotlib.use("pgf")
matplotlib.rcParams.update({
    "pgf.texsystem": "pdflatex",
    'font.family': 'sans serif',
    'font.size' : 16, 
    'text.usetex' : False,
    'pgf.rcfonts': False,
    'mathtext.fontset': 'stixsans',
})
# plt.yticks(fontname = "sans serif")  # This argument will change the font.
plt.rcParams.update({
    "font.family": "sans-serif",
    "font.serif": [],                    # use latex default serif font
    "font.sans-serif": ["DejaVu Sans"],  # use a specific sans-serif font
    "legend.fontsize": "small",
    "legend.columnspacing": 0.4,
    "mathtext.fontset" : "stixsans"
})

pd.set_option('display.max_rows', None)
pd.set_option('display.max_colwidth', None)

# colors_pallete = plt.rcParams['axes.prop_cycle'].by_key()['color']

base_path_latex_images = "experiment/latex/images"

def chart_bar_group(*iteratables, yerrs=None, width=None, xlabel=None,
                  ylabel=None, ncol=None, linestyles=None,
                  colors=None, alpha=1, capsize=1, alphas=None, 
                  hatches=None, figsize=None, xTickLabelRotation=0,
                  ylimMax=None, ylimMin=None, yscale='linear', yline=None):      

      if not iteratables or len(iteratables) != 4:
            raise Exception("chart_bar_group requires (title, data, index, cols).")

      title, xs, ys, axes = iteratables

      x = np.arange(len(xs))  # the label locations

      if figsize is None:
            fig, ax = plt.subplots()
      else:
            fig, ax = plt.subplots(figsize=figsize)            
      
      for i in range(len(axes)):
            pos_ = i + 0.5 - (len(ys) / 2)
            pos = x +(pos_)*width

            color = colors[i] if colors else None
            # linestyle = linestyles[i] if linestyles else None            

            y = ys[i,:]
            yerr = yerrs[i,:]
            label = axes[i]

            if alphas is not None:
                  alpha = alphas[i]
            if hatches is None:
                  hatch = None
            else:                  
                  hatch = hatches[i] * 3

            ax.bar(pos, y, width, 
                  label=label, 
                  yerr=yerr, 
                  color=color,
                  align='center', 
                  ecolor="black",
                  alpha=alpha, 
                  capsize=capsize,
                  hatch=hatch,
                  )

      if ylimMax is not None and ylimMin is not None:
            plt.ylim((ylimMin, ylimMax))
      if yline is not None:
            plt.axhline(yline)

      ax.set_yscale(yscale)

      # Add some text for labels, title and custom x-axis tick labels, etc.
      ax.set_ylabel(ylabel, fontweight='bold') # fontsize='normal', 
      ax.set_xlabel(xlabel, fontweight='bold')

      ax.set_title(title)
      ax.set_xticks(x)
      ax.set_xticklabels(xs, rotation=xTickLabelRotation)
      

      ax.legend()
      # ax.autoscale(enable=True) 

      fig.tight_layout()
      # plt.savefig(base_path_latex_images + "/" + title + ".pgf")

      debug = True
      debug = False
      if debug is True:
            print("debug is on.")
            pdf.savefig()  # saves the current figure into a pdf page
            plt.show()
      else:
            plt.savefig(base_path_latex_images + "/" + title + ".pgf")

      plt.close()

      # plt.show()

def chart_line(*iteratables, yerrs=None, width=None, xlabel=None,
                  ylabel=None, ncol=1, markers=None, linestyles=None,
                  colors=None, capsize=0, dummylabelnum=0,
                  xAxisEngFormat=False, alpha=1, alphas=None, figsize=None,
                  xscale="linear", xbase=10, yscale="linear",
                  ylimMax=None, ylimMin=None):

                  
      global pdf      
      if not iteratables or len(iteratables) != 4:
            raise Exception("chart_line requires (title, data, index, cols).")
      title, data, index, cols = iteratables

      if figsize is None:
            fig, ax = plt.subplots()
      else:
            fig, ax = plt.subplots(figsize=figsize)            

      for i in range(len(index)):
            color = colors[i] if colors else None
            marker = markers[i] if markers else None
            linestyle = linestyles[i] if linestyles else None            

            x = cols
            y = data[i]
            label = index[i]

            if alphas is not None:
                  alpha = alphas[i]

            if yerrs is None:
                  ax.plot(x, y,
                        label=label,  
                        marker=marker,  
                        linestyle=linestyle, 
                        color=color,
                        alpha=alpha,
                        )
            else:
                  # def yerr_min(x):
                  #       if x < 1.1:
                  #             return 10
                  #       else:
                  #             return x

                  # def array_map(x):
                  #       return np.array(list(map(yerr_min, x)))

                  # yerr = array_map(yerrs[i])
                  # print(yerrs[i])
                  # print(yerr)

                  ax.errorbar(x, y,
                        yerr=yerrs[i], 
                        label=label,  
                        alpha=alpha,
                        marker=marker,  
                        linestyle=linestyle, 
                        color=color,
                        capsize=capsize, 
                        ecolor="black",
                        barsabove=True
                        )

      ax.set_ylabel(ylabel, fontweight='bold')#, fontsize='large')
      ax.set_xlabel(xlabel, fontweight='bold')#, fontsize='x-large', fontfamily="sans-serif")
      if ylimMax is not None and ylimMin is not None:
            plt.ylim((ylimMin, ylimMax))

      #plt.xlim((10**4, 10**7))
      # ax.set_ylim([1, 10000])

      for i in range(dummylabelnum):
            if yerrs is None:
                  ax.plot(np.zeros(1), np.zeros([1,1]), color='w', alpha=0, label=' ')
            else:
                  ax.errorbar(np.zeros(1), np.zeros([1,1]), color='w', alpha=0, label=' ')

      ax.set_title(title)
      ax.set_yscale(yscale)

      # title()

 
      if xscale == "log":
            ax.set_xscale(xscale, base=xbase)
      else:
            ax.set_xscale(xscale) #, subs=[])

      from matplotlib.ticker import EngFormatter
      from matplotlib.ticker import LogFormatterMathtext

      if xAxisEngFormat is True:
            for axis in [ax.xaxis]: #, ax.yaxis]:
                  axis.set_major_formatter(EngFormatter())

      for axis in [ax.yaxis]: #, ax.yaxis]:
            k = axis.get_major_formatter()
            axis.set_major_formatter(LogFormatterMathtext())

      ax.legend(loc="upper left", ncol=ncol)
      fig.tight_layout()
      
      # debug = True
      debug = False
      if debug is True:
            print("debug is on.")
            pdf.savefig()  # saves the current figure into a pdf page
            plt.show()
      else:
            plt.savefig(base_path_latex_images + "/" + title + ".pgf")

      plt.close()

      





def chart_line_subplots(*iteratables, yerrs=None, width=None, xlabel=None,
                  ylabel=None, ncol=1, markers=None, linestyles=None,
                  colors=None, capsize=0, dummylabelnum=0,
                  xAxisEngFormat=False, alpha=1, alphas=None, figsize=None,
                  xscale="linear", xbase=10, yscale="linear",
                  ylimMax=None, ylimMin=None, subplot_x=1, subplot_y=1):
      global pdf      
      if not iteratables or len(iteratables) != 4:
            raise Exception("chart_line requires (title, data, index, cols).")
      title, data, index, cols = iteratables

      if figsize is None:
            fig, ax = plt.subplots(subplot_x, subplot_y, sharey=True)
      else:
            fig, ax = plt.subplots(subplot_x, subplot_y, figsize=figsize, sharey=True)            

# fig, (ax1, ax2) = plt.subplots(1, 2)
# fig.suptitle('Horizontally stacked subplots')
# ax1.plot(x, y)
# ax2.plot(x, -y)

      idx = -1
      for j in range (0, subplot_x):
            for k in range (0, subplot_y):
                  print("cols: ", cols, " idx:", idx)
                  idx = idx + 1
                  _cols = cols[idx]
                  _data = data[idx]
                  if yerrs is not None:
                        _yerrs = yerrs[idx]

                  for i in range(len(index)):
                        color = colors[i] if colors else None
                        marker = markers[i] if markers else None
                        linestyle = linestyles[i] if linestyles else None            
                        
                        x = _cols
                        y = _data[i]
                        label = index[i]

                        print("x: ", x)
                        print("y: ", y)

                        if alphas is not None:
                              alpha = alphas[idx]

                        if yerrs is None:
                              ax.plot(x, y,
                                    label=label,  
                                    marker=marker,  
                                    linestyle=linestyle, 
                                    color=color,
                                    alpha=alpha,
                                    )
                        else:
                              # def yerr_min(x):
                              #       if x < 1.1:
                              #             return 10
                              #       else:
                              #             return x

                              # def array_map(x):
                              #       return np.array(list(map(yerr_min, x)))

                              # yerr = array_map(yerrs[i])
                              # print(yerrs[i])
                              # print("yerrs[i]: ", _yerrs[i])

                              ax[idx].errorbar(x, y,
                                    yerr=_yerrs[i], 
                                    label=label,  
                                    alpha=alpha,
                                    marker=marker,  
                                    linestyle=linestyle, 
                                    color=color,
                                    capsize=capsize, 
                                    ecolor="black",
                                    barsabove=True
                                    )

                  # if ylimMax is not None and ylimMin is not None:
                  #       plt.ylim((ylimMin, ylimMax))

                  # ax[idx].set_xlim([0, 1.0])

                  # ax.set_ylim([1, 10000])

                  # for i in range(dummylabelnum):
                  #       if yerrs is None:
                  #             ax[0].plot(np.zeros(1), np.zeros([1,1]), color='w', alpha=0, label=' ')
                  #       else:
                  #             ax[0].errorbar(np.zeros(1), np.zeros([1,1]), color='w', alpha=0, label=' ')

                  ax[idx].set_title("Query Set " + str(idx+1))

                  ax[idx].set_yscale(yscale)
                  if xscale == "log":
                        ax[idx].set_xscale(xscale, base=xbase)
                  else:
                        ax[idx].set_xscale(xscale) #, subs=[])
                  
                  ax[idx].set_xticks([0,1])

                  # from matplotlib.ticker import EngFormatter
                  # from matplotlib.ticker import LogFormatterMathtext

                  # if xAxisEngFormat is True:
                  #       for axis in [ax[idx].xaxis]: #, ax.yaxis]:
                  #             axis.set_major_formatter(EngFormatter())

                  # for axis in [ax[idx].yaxis]: #, ax.yaxis]:
                  #       k = axis.get_major_formatter()
                  #       axis.set_major_formatter(LogFormatterMathtext())

                  if idx == 0:
                        ax[idx].set_ylabel(ylabel, fontweight='bold')#, fontsize='large')
                  
                        # ax[idx].set_yticklabels([200,400,600,800])
                  elif idx == 1:
                        ax[idx].set_xlabel(xlabel, fontweight='bold')#, fontsize='x-large', fontfamily="sans-serif")
                  elif idx == 2:
                        ax[idx].legend(loc="upper left", ncol=ncol)
                              # ax[idx].set_yticklabels([])


      plt.subplots_adjust(left=None, bottom=0.2, right=None, top=None, wspace=0.1, hspace=0.1)
      # fig.tight_layout()
      # plt.savefig(base_path_latex_images + "/" + title + ".pgf")
      # pdf.savefig()  # saves the current figure into a pdf page
      # plt.close()

      
      plt.show()





def closePdf():
      global pdf
      pdf.close()
