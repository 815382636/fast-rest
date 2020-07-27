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
account = [500, 750, 1000, 1500, 2000]
# bm4 =[]
# m4 =[]
# agg =[]
# bagg =[]
# sample =[]
# bsample =[]
# grad =[]

# m4 =[1,2,3,4,5]
# sample =[1,2,3,4,5]
# aggregation =[1,2,3,4,5]
# weight =[1,2,3,4,5]
# outlier =[1,2,3,4,5]

text1 =[1,2,3,4,5]
text2 =[1,2,3,4,5]


plt.figure(figsize=(28, 15))

for i in range(1, 7):
    data = open("%d.txt" % i).read()
    lines = data.split("\n")
    for line in lines:
        print(line.split(","))
        index = 0
        if "500" in line:
            index = 0
        if "750" in line:
            index = 1
        if "1000" in line:
            index = 2
        if "1500" in line:
            index = 3
        if "2000" in line:
            index = 4
        if "correlation" in line:
            text1[index] = float(line.split(",")[1])
        elif "subway" not in line:
            text2[index] = float(line.split(",")[1])
        # if "sample" in line:
        #     sample[index] = float(line.split(",")[1])
        # if "weight" in line:
        #     weight[index] = float(line.split(",")[1])
        # if "aggregation" in line:
        #     aggregation[index] = float(line.split(",")[1])
        # if "outlier" in line:
        #     outlier[index] = float(line.split(",")[1])

    ax = plt.subplot(2, 3, i)
    plt.plot(account, text1, "o-", color='#ED5564', label="correlation-weight")
    plt.plot(account, text2, "v-", color='#AC92EB', label="weight")
    # plt.plot(account, aggregation, "o--", color='#FFCE54', label="aggregation")
    # plt.plot(account, outlier, "v--", color='#D20338', label="outlier")
    # plt.plot(account, weight, "o-.", color='#A0D568', label="weight")
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
