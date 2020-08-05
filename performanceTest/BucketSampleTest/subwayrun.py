# -*- coding: utf-8 -*-


import requests
import json
import time
import os
import pandas as pd
import numpy as np
from PIL import Image
import altair as alt
import tensorflow as tf
from pae import PAEMeasure
import sys

RED = 76
GREEN = 120
BLUE = 168
ALPHA = 255

line_dir = "subwayline_png"
area_dir = "subwayarea_png"
pixel_dir = "subwaypixel_png"
line_postfix = "-line.png"
area_postfix = "-area.png"
pixel_postfix = "-pixel.png"


start_step =0
param_file ="subway-m4.txt"
start_time = "2019-08-16 00:00:00"
end_time = "2019-08-18 00:00:00"
min_value, max_value =1900,3000
param_lines = open(param_file).read().split("\n")
n = len(param_lines) // 3
width =1000
height =200
base_area_png =param_lines[0] + area_postfix
base_line_png = param_lines[0] + line_postfix
pae_meas = PAEMeasure(width, height)
stat_0 = []

def calcMSSSIM(file1, file2):
    image_raw_data_1 = tf.io.gfile.GFile(file1, "rb").read()
    image_raw_data_2 = tf.io.gfile.GFile(file2, "rb").read()
    img_data_1 = tf.image.decode_png(image_raw_data_1)
    img_data_2 = tf.image.decode_png(image_raw_data_2)
    img_resized_1 = tf.image.resize(img_data_1, [width, height])
    img_resized_2 = tf.image.resize(img_data_2, [width, height])

    # calc MS-SSIM with default weights [0.0448,0.2856,0.3001,0.2363,0.1333]
    msssim = tf.image.ssim_multiscale(img_resized_1, img_resized_2, 100)
    return msssim


def line2pixel(line_png):
    png_path = line_png
    img = Image.open(png_path).convert("RGBA")
    a_img = np.asarray(img)
    rows = a_img.shape[0]
    columns = 1000
    target = [RED, GREEN, BLUE, ALPHA]
    pixel_data = []
    for col in range(columns):
        is_match = False
        for row in range(rows):
            pix = a_img[row][col]
            is_match = (pix[0] == target[0] and pix[1] == target[1] and pix[2] == target[2] and pix[3] == target[3])
            if is_match:
                pixel_data.append({"x": col, "y": rows - row})
                break
        if not is_match:
            pixel_data.append({"x": col, "y": 0})
    return pixel_data


DEBUG = False
time_start = time.time()


if start_step <= 2:

    os.system("rm -R %s" % line_dir)
    os.system("mkdir -p %s" % line_dir)
    os.system("rm -R %s" % area_dir)
    os.system("mkdir -p %s" % area_dir)

    # stat_result = open("stat_result.txt", "a")
    # 每三行参数组合为一组实验测试
    for i in range(0, n):
        name = param_lines[i * 3]
        url = param_lines[i * 3 + 1]
        parameter = json.loads(param_lines[i * 3 + 2])

        time_label = "time"
        dbtype = parameter["dbtype"]
        value_label = parameter["columns"][0].lower()

        if DEBUG:
            print(name, url, parameter)
            print(time_label, value_label, dbtype)

        time_0 = time.time()

        # 1.向采样中间层请求JSON数据
        print("# 1.请求第%d幅图片JSON数据..." % (i + 1))
        json_data = {}
        response = requests.get(url, parameter)

        if response.status_code == 200:
            json_data = pd.read_json(response.text)
            if dbtype == "iotdb":
                json_data.columns = list(map(lambda x: x.split(".")[-1].lower(), list(json_data.columns)))
            print(json_data.columns)
            # print(json_data[0])
            json_data = json_data.loc[json_data[value_label] > 0]
            print(json_data.columns)
            # print(json_data[0])

        else:
            print("请求数据失败!")
            exit(0)

        time_1 = time.time()
        print("# 1.用时%s ms " % ((time_1 - time_0) * 1000))
        print("# 1.%s数据规模%d " % (name, json_data.shape[0]))

        # value_frame = json_data[value_label]
        # mean = value_frame.mean()
        # std = value_frame.std()
        # rng = value_frame.max() - value_frame.min()
        #
        # quartiles = pd.cut(value_frame, 10)
        # grouped = value_frame.groupby(quartiles)
        # stat = grouped.apply(lambda x: x.count()) / value_frame.count()
        #
        # diserr = 0
        # if i == 2:
        #     stat_0 = stat
        # else:
        #     diserr = (abs(stat - stat_0)).sum() / 2
        #
        # stat_result.write("%s,%f,%f,%f,%f\n" % (name, mean, std, rng, diserr))
        #
        # if DEBUG:
        #     print(json_data.head(5))

        # 2.绘制JSON图像
        print("# 2.绘制第%d幅图像..." % (i + 1))
        line_chart = name + line_postfix
        area_chart = name + area_postfix

        alt.Chart(json_data).mark_line().encode(
            alt.X(scale=alt.Scale(domain=[start_time, end_time]), field=time_label, type="temporal",
                  axis=alt.Axis(title="", labelFontSize=20)),
            alt.Y(scale=alt.Scale(domain=[min_value, max_value]), field=value_label, type="quantitative",
                  axis=alt.Axis(title="", labelFontSize=20)),
        ).properties(
            width=width,
            height=height
        ).save(line_chart)
        os.system("mv *%s %s" % (line_postfix, line_dir))

        alt.Chart(json_data).mark_area().encode(
            alt.X(scale=alt.Scale(domain=[start_time, end_time]), field=time_label, type="temporal",
                  axis=alt.Axis(title="", labelFontSize=20)),
            alt.Y(scale=alt.Scale(domain=[min_value, max_value]), field=value_label, type="quantitative",
                  axis=alt.Axis(title="", labelFontSize=20)),
        ).properties(
            width=width,
            height=height
        ).save(area_chart)
        os.system("mv *%s %s" % (area_postfix, area_dir))

        time_2 = time.time()
        print("# 2.用时%s ms " % ((time_2 - time_1) * 1000))

        # 循环结束
    # stat_result.close()
    print("# 所有图片绘制完成...")

time_2 = time.time()

# 3.面积图计算MS-SSIM
if start_step <= 3:
    print("# 3.面积图计算MS-SSIM...")
    msssim_result = open("1.txt", "a")
    # msssim_result.write("%f,%f\n" % (percent, alpha))
    # msssim_result.write("sample,msssim\n")
    area_charts = os.listdir(area_dir)
    for area_chart in area_charts:
        msssim = calcMSSSIM(area_dir + "/" + base_area_png, area_dir + "/" + area_chart)
        ms = (area_chart + "," + str(msssim) + "\n").replace("tf.Tensor(", "").replace(", shape=(), dtype=float32)", "")
        msssim_result.write(ms)
    msssim_result.close()
    time_3 = time.time()
    print("# 3.用时%s ms " % ((time_3 - time_2) * 1000))

time_3 = time.time()

# 4.提取折线图像素计算PAE
if start_step <= 4:
    os.system("rm -R %s" % pixel_dir)
    os.system("mkdir -p %s" % pixel_dir)
    print("# 4.提取折线图像素计算PAE...")
    pae_result = open("6.txt", "a")
    # pae_result.write("%f,%f\n" % (percent, alpha))
    # pae_result.write("sample,pae\n")
    line_charts = os.listdir(line_dir)
    for line_chart in line_charts:
        pixel_data = line2pixel(line_dir + "/" + line_chart)
        alt.Chart(pd.DataFrame(pixel_data)).mark_line().encode(
            alt.X(field="x", type="quantitative", axis=alt.Axis(title="", labelFontSize=20)),
            alt.Y(field="y", type="quantitative", axis=alt.Axis(title="", labelFontSize=20)),
        ).properties(
            width=width,
            height=height
        ).save(line_chart + pixel_postfix)
        os.system("mv *%s %s" % (pixel_postfix, pixel_dir))
        ys = list(map(lambda x: float(x["y"]), pixel_data))
        pae_result.write(line_chart + "," + str(pae_meas.pae(ys)) + "\n")
    pae_result.close()

    time_4 = time.time()
    print("# 4.用时%s ms " % ((time_4 - time_3) * 1000))
time_4 = time.time()

# 5.面积图提取上沿计算均方误差
if start_step <= 5:
    print("# 5.面积图提取上沿计算均方误差...")
    mse_result = open("mse_result.txt", "a")
    # mse_result.write("%f,%f\n" % (percent, alpha))
    mse_result.write("sample,mse\n")
    pixel_data_0 = line2pixel(area_dir + "/" + base_area_png)
    ys_0 = np.array(list(map(lambda x: float(x["y"]), pixel_data_0)))

    area_charts = os.listdir(area_dir)
    for area_chart in area_charts:
        pixel_data = line2pixel(area_dir + "/" + area_chart)
        alt.Chart(pd.DataFrame(pixel_data)).mark_line().encode(
            alt.X(field="x", type="quantitative", axis=alt.Axis(title="", labelFontSize=20)),
            alt.Y(field="y", type="quantitative", axis=alt.Axis(title="", labelFontSize=20)),
        ).properties(
            width=width,
            height=height
        ).save(area_chart + pixel_postfix)
        os.system("mv *%s %s" % (pixel_postfix, pixel_dir))
        ys = np.array(list(map(lambda x: float(x["y"]), pixel_data)))
        mse_result.write(area_chart + "," + str(((ys - ys_0) ** 2).sum() / len(ys_0)) + "\n")
    mse_result.close()

    time_5 = time.time()
    print("# 5.用时%s ms " % ((time_5 - time_4) * 1000))

time_5 = time.time()

# 5.折线图提取上沿计算均方误差
if start_step <= 6:
    print("# 6.折线图提取上沿计算均方误差...")
    mse_result = open("11.txt", "a")
    # mse_result.write("%f,%f\n" % (percent, alpha))
    # mse_result.write("sample,pmse\n")
    pixel_data_0 = line2pixel(line_dir + "/" + base_line_png)
    ys_0 = np.array(list(map(lambda x: float(x["y"]), pixel_data_0)))

    line_charts = os.listdir(line_dir)
    for line_chart in line_charts:
        pixel_data = line2pixel(line_dir + "/" + line_chart)
        ys = np.array(list(map(lambda x: float(x["y"]), pixel_data)))
        mse_result.write(line_chart + "," + str(((ys - ys_0) ** 2).sum() / len(ys_0)) + "\n")
    mse_result.close()

    time_5 = time.time()
    print("# 5.用时%s ms " % ((time_5 - time_4) * 1000))

time_5 = time.time()

# 7.折线图计算MS-SSIM
if start_step <= 3:
    print("# 7.折线图计算MS-SSIM...")
    msssim_result = open("l_msssim_result.txt", "a")
    # msssim_result.write("%f,%f\n" % (percent, alpha))
    msssim_result.write("sample,msssim\n")
    line_charts = os.listdir(line_dir)
    for line_chart in line_charts:
        msssim = calcMSSSIM(line_dir + "/" + base_line_png, line_dir + "/" + line_chart)
        ms = (line_chart + "," + str(msssim) + "\n").replace("tf.Tensor(", "").replace(", shape=(), dtype=float32)", "")
        msssim_result.write(ms)
    msssim_result.close()
    time_6 = time.time()
    print("# 7.用时%s ms " % ((time_6 - time_5) * 1000))

time_6 = time.time()

# 6. 绘制统计指标结果
# stat_result = open("stat_result.txt")
# mse_result = open("mse_result.txt")
#
# msssim_result = open("msssim_result.txt")
# pae_result = open("pae_result.txt")


print("# 本次脚本测试完成！")
print("# 用时%s ms " % ((time_5 - time_start) * 1000))


