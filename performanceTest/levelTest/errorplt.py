import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np

# bm4 = [0.98868835, 0.9923267, 0.99378175, 0.995273, 0.9969575]
# m4 = [0.9722532, 0.98416954, 0.9883536, 0.99327016, 0.9957609]
# agg = [0.9722532, 0.98416954, 0.9883536, 0.99327016, 0.9957609]
# bagg = [0.9722532, 0.98416954, 0.9883536, 0.99327016, 0.9957609]
# sample = [0.9722532, 0.98416954, 0.9883536, 0.99327016, 0.9957609]
# bsample = [0.9722532, 0.98416954, 0.9883536, 0.99327016, 0.9957609]
# grad = [0.9722532, 0.98416954, 0.9883536, 0.99327016, 0.9957609]
account = [1,2,3,4,5]
# bm4 =[]
# m4 =[]
# agg =[]
# bagg =[]
# sample =[]
# bsample =[]
# grad =[]

m4 =[1,2,3,4]
sample =[1,2,3,4]
# aggregation =[1,2,3,4,5]
weight =[1,2,3,4]
outlier =[1,2,3,4]

bianliangyi =[1,2,3,4,5]
bianlianger =[1,2,3,4,5]

plt.figure(figsize=(28, 15))


data = open("errortest.txt").read()
lines = data.split("\n")
for line in lines:
    index = 0
    if "-1" in line:
        index = 0
    if "-2" in line:
        index = 1
    if "-3" in line:
        index = 2
    if "-4" in line:
        index = 3
    if "-5" in line:
        index = 4
    if "variable1" in line:
        bianliangyi[index] = float(line.split("，")[1])
    if "variable2" in line:
        bianlianger[index] = float(line.split("，")[1])


plt.plot(account, bianliangyi, "o-", color='#AC92EB', label="变量一")
# plt.plot(account, aggregation, "o-", color='#FFCE54', label="aggregation")
plt.plot(account, bianlianger, "o-", color='#0870CB', label="变量二")
# plt.plot(account, weight, "o-", color='#A0D568', label="weight")
# plt.plot(account, m4, "o-", color='#D20338', label="m4")

plt.grid()
plt.xticks(fontsize=13)
plt.yticks(fontsize=13)
plt.gca().yaxis.set_major_formatter(ticker.FormatStrFormatter('%.3f'))

# ax.legend(bbox_to_anchor=(-0.22, -0.22), loc='lower right', ncol=7, fontsize="x-large")
plt.savefig('result.jpg')
plt.show()
