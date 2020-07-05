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

if len(sys.argv) < 1:
    print("请输入参数文件(必选)以及开始步骤编号(可选)！")
    exit(0)

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

RED = 76
GREEN = 120
BLUE = 168
ALPHA = 255

line_dir = "line_png"
area_dir = "area_png"
pixel_dir = "pixel_png"
line_postfix = "-line.png"
area_postfix = "-area.png"
pixel_postfix = "-pixel.png"

# 0.准备请求参数
# TODO: 参数生成器
param_file = sys.argv[1]
start_step = int(sys.argv[2]) if len(sys.argv) > 1 else 0
percent = float(sys.argv[3]) if len(sys.argv) > 1 else 1.0
alpha = float(sys.argv[4]) if len(sys.argv) > 1 else 1.0
param_lines = open(param_file).read().split("\n")
n = len(param_lines) // 3
base_area_png = param_lines[6] + area_postfix

start_time = param_lines[0]
end_time = param_lines[1]
min_value = int(param_lines[2])
max_value = int(param_lines[3])
width = int(param_lines[4])
height = int(param_lines[5])

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
    columns = a_img.shape[1]
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

    stat_result = open("stat_result.txt", "a")
    stat_result.write("sample,mean,std,range,diserr\n")
    # 每三行参数组合为一组实验测试
    for i in range(2, n):
        name = param_lines[i * 3]
        url = param_lines[i * 3 + 1]
        parameter = json.loads(param_lines[i * 3 + 2])
        parameter["percent"] = percent
        parameter["alpha"] = alpha

        time_label = "time"
        dbtype = parameter["dbtype"]
        value_label = parameter["columns"]

        if DEBUG:
            print(name, url, parameter)
            print(time_label, value_label, dbtype)

        time_0 = time.time()

        # 1.向采样中间层请求JSON数据
        print("# 1.请求第%d幅图片JSON数据..." % (i - 1))
        json_data = {}
        response = requests.get(url, parameter)

        if response.status_code == 200:
            json_data = pd.read_json(response.text)
            if dbtype == "iotdb":
                json_data.columns = list(map(lambda x: x.split(".")[-1], list(json_data.columns)))
        else:
            print("请求数据失败!")
            exit(0)

        time_1 = time.time()
        print("# 1.用时%s ms " % ((time_1 - time_0) * 1000))
        print("# 1.数据规模%d " % json_data.shape[0])

        value_frame = json_data[value_label]
        mean = value_frame.mean()
        std = value_frame.std()
        rng = value_frame.max() - value_frame.min()

        quartiles = pd.cut(value_frame, 10)
        grouped = value_frame.groupby(quartiles)
        stat = grouped.apply(lambda x: x.count()) / value_frame.count()

        diserr = 0
        if i == 2:
            stat_0 = stat
        else:
            diserr = (abs(stat - stat_0)).sum() / 2

        stat_result.write("%s,%f,%f,%f,%f\n" % (name, mean, std, rng, diserr))

        if DEBUG:
            print(json_data.head(5))

        # 2.绘制JSON图像
        print("# 2.绘制第%d幅图像..." % (i - 1))
        line_chart = name + line_postfix
        area_chart = name + area_postfix

        # color = "#4FC1E8"
        # if "sample" in name:
        #     color = "#A0D568"
        # if "grad" in name:
        #     color = "#AC92EB"
        # if "agg" in name:
        #     color = "#FFCE54"
        # if "m4" in name:
        #     color = "#ED5564"

        color = "#0870CB"
        if "sample" in name:
            color = "#04B390"
        if "grad" in name:
            color = "#8C2BAD"
        if "agg" in name:
            color = "#FE9B0A"
        if "m4" in name:
            color = "#D20338"

        print(name)
        print(color)

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
    stat_result.close()
    print("# 所有图片绘制完成...")

time_2 = time.time()

