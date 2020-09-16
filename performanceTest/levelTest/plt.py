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
account = [2000,4000,8000,12000]
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


plt.figure(figsize=(28, 15))

for i in range(1, 7):
    data = open("%d.txt" % i).read()
    lines = data.split("\n")
    for line in lines:
        # print(line.split(","))
        index = 0
        if "2000" in line:
            index = 0
        if "4000" in line:
            index = 1
        if "8000" in line:
            index = 2
        if "12000" in line:
            index = 3
        # if "2000" in line:
        #     index = 4
        if "m4" in line:
            m4[index] = float(line.split(",")[1])
            if i ==3:
                print(m4[index])
        if "sample" in line:
            sample[index] = float(line.split(",")[1])
        if "weight" in line:
            weight[index] = float(line.split(",")[1])
        # if "aggregation" in line:
        #     aggregation[index] = float(line.split(",")[1])
        if "outlier" in line:
            outlier[index] = float(line.split(",")[1])

    ax = plt.subplot(2, 3, i)


    plt.plot(account, sample, "o-", color='#AC92EB', label="random")
    # plt.plot(account, aggregation, "o-", color='#FFCE54', label="aggregation")
    plt.plot(account, outlier, "o-", color='#0870CB', label="outlier")
    plt.plot(account, weight, "o-", color='#A0D568', label="weight")
    plt.plot(account, m4, "o-", color='#D20338', label="m4")

    # plt.plot(account, sample, "v-.", color='#A0D568', label="sample")
    # plt.plot(account, grad, "o:", color='#AC92EB', label="FAST-outlier")
    plt.grid()
    plt.xticks(fontsize=13)
    plt.yticks(fontsize=13)
    if i <= 10:
        plt.gca().yaxis.set_major_formatter(ticker.FormatStrFormatter('%.3f'))

ax.legend(bbox_to_anchor=(-0.22, -0.22), loc='lower right', ncol=7, fontsize="x-large")
plt.savefig('result.jpg')
plt.show()
