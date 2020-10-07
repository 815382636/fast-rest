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

line_dir = "line_png"
area_dir = "area_png"
line_dir_list = ["line_png"]
area_dir_list = []
pixel_dir_list = ["pixel_png"]

pixel_dir = "pixel_png"
line_postfix = "-line.png"
area_postfix = "-area.png"
pixel_postfix = "-pixel.png"

start_step = 0
param_file = "url500.txt"
start_time = "2019-08-16 00:00:00"
end_time = "2019-08-18 00:00:00"
min_value, max_value = 1750, 3000
param_lines = open(param_file).read().split("\n")
n = len(param_lines) // 3
width = 1000
height = 200
base_area_png = param_lines[0] + area_postfix
base_line_png = param_lines[0] + line_postfix
pae_meas = PAEMeasure(width, height)
stat_0 = []
value_labels = []


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
        for l in range(0, len(parameter["columns"])):
            if parameter["columns"][l].lower() not in value_labels:
                value_labels.append(parameter["columns"][l].lower())

        if i == 0:
            for value_label in value_labels:
                os.system("rm -R %s" % (value_label + "_" + line_dir))
                os.system("mkdir -p %s" % (value_label + "_" + line_dir))
                line_dir_list.append(value_label + "_" + line_dir)
                os.system("rm -R %s" % (value_label + "_" + area_dir))
                os.system("mkdir -p %s" % (value_label + "_" + area_dir))
                area_dir_list.append(value_label + "_" + area_dir)
            print("建包完成")

        if DEBUG:
            print(name, url, parameter)
            print(time_label, value_labels, dbtype)

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
            json_data = json_data.loc[json_data[value_labels[0]] > 0]
            json_data = json_data.loc[json_data[value_labels[1]] > 0]

            print(json_data.columns)
        else:
            print("请求数据失败!")
            exit(0)

        time_1 = time.time()
        print("# 1.用时%s ms " % ((time_1 - time_0) * 1000))
        print("# 1.%s数据规模%d " % (name, json_data.shape[0]))

        line_chart = name + line_postfix
        area_chart = name + area_postfix
        color = "#0870CB"
        if "univariate" in name:
            color = "#04B390"
        if "aggregation" in name:
            color = "#8C2BAD"
        if "outlier" in name:
            color = "#81F7D8"
        if "m4" in name:
            color = "#D20338"
        if "weight" in name:
            color = "#2E2EFE"
        if "random" in name:
            color = "#FFCE54"
        for value_label in value_labels:
            # 2.绘制JSON图像
            print("# 2.绘制第%d幅%s折线图像..." % (i + 1,value_label))

            alt.Chart(json_data).mark_line().encode(
                alt.X(scale=alt.Scale(domain=[start_time, end_time]), field=time_label, type="temporal",
                      axis=alt.Axis(title="", labelFontSize=20)),
                alt.Y(scale=alt.Scale(domain=[min_value, max_value]), field=value_label, type="quantitative",
                      axis=alt.Axis(title="", labelFontSize=20)),
                alt.Color(value=color)

            ).properties(
                width=width,
                height=height
            ).save(line_chart)
            os.system("mv *%s %s" % (line_postfix, value_label+"_" + line_dir))

            print("# 2.绘制第%d幅%s面积图像..." % (i + 1,value_label))

            alt.Chart(json_data).mark_area().encode(
                alt.X(scale=alt.Scale(domain=[start_time, end_time]), field=time_label, type="temporal",
                      axis=alt.Axis(title="", labelFontSize=20)),
                alt.Y(scale=alt.Scale(domain=[min_value, max_value]), field=value_label, type="quantitative",
                      axis=alt.Axis(title="", labelFontSize=20)),
                alt.Color(value=color)

            ).properties(
                width=width,
                height=height
            ).save(area_chart)
            os.system("mv *%s %s" % (area_postfix, value_label+"_" + area_dir))

