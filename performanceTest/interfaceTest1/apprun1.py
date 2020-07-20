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
param_file = "bivariateURL.txt"
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


name = param_lines[6 * 3]
url = param_lines[6 * 3 + 1]
parameter = json.loads(param_lines[6 * 3 + 2])

time_label = "time"
dbtype = parameter["dbtype"]
for l in range(0, len(parameter["columns"])):
    value_labels.append(parameter["columns"][l].lower())


print("# 1.请求第%d幅图片JSON数据..." % (6 + 1))
json_data = {}
response = requests.get(url, parameter)

if response.status_code == 200:
    print(response.text)
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
print("# 1.%s数据规模%d " % (name, json_data.shape[0]))

