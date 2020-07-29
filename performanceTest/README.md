# 自适应采样中间层接口和性能测试

通过计算面积图MS-SSIM、折线图PAE、面积图均方误差、折线图均方误差、折线图MS-SSIM来比较采样结果与原数据的相似性程度

## 可视化评价指标概述

### 多尺度结构相似性指数（MS-SSIM）

    多尺度结构相似性指数（MS-SSIM）是一种用于评价压缩图像质量的方法。SSIM的全称为structural similarity index，即为结构相似性，是一种衡量两幅图像相似度的指标。该指标首先由德州大学奥斯丁分校的图像和视频工程实验室(Laboratory for Image and Video Engineering)提出。而如果两幅图像是压缩前和压缩后的图像，那么SSIM算法就可以用来评估压缩后的图像质量。MS-SSIM 指标越高则可视化效果越好。

### 像素近似熵（PAE）
    
    素近似熵（PAE）主要用于衡量折线图可视化结果的可读性，即用户从可视化结果中感知微小差异的容易程度。Ryan 等人从信息论中的原始近似熵提出了像素近似熵。其核心思想是，考虑到可视化效果而不是数据本身，通过将数据序列映射到虚拟可视化画布中并构造像素向量来计算像素数据的近似熵。PAE 独立于原始数据，并为给定的图表分辨率提供一致的熵值范围。PAE 指标越低则可视化效果越好。

### 像素均方误差（PMSE）

    像素均方误差（PMSE）是本次研究中提出的度量两幅图像之间的可视化差异的指标。MS-SSIM 度量图像之间的相似度，但相似度并不完全等价于采样精度。PAE 衡量的是可视化结果的可读性，而高可读性并不能带来高准确度。在本次实验过程中总结提出了 PMSE 指标，它确实更好地描述了本文的目标。如术语所示，PMSE 是像素级可视化度量和传统的均方误差指标的组合。PMSE 指标的计算过程如下：给定原始数据O和样本数据S，首先将数据点可视化成分辨率相同的折线图imgO和imgS。然后，使用提取脚本从图像中获取像素数据Oixel和Spixel。值得注意的是，尽管 O和 S的数据规模不同，但 Opixel和Spixel的数据规模是相同的，即图像的宽度。将样本数据Spixel作为原始数据Opixel的一个估计，并将其均方误差计算为 PMSE。PMSE 指标越低则可视化效果越好。

## 测试过程

-   本次实验主要是通过以上的几种评价指标来对比自适应分桶与平均分桶、桶内的5种采样算子（sample,M4,aggregation,outler,weight）、两种权重计算方式（计算联合权重和单独权重）所产生的采样结果与原数据的相似度。
-   本次实验使用5种数据（subway、kpi、bus、particle、intel）,使用控制变量法，变量为数据类型（5种数据）、采样数量（500、750、1000、1500、2000）、分桶方式（自适应分桶、平均分桶）、权重计算方式（联合权重、单独权重）、桶内采样算子（sample,M4,aggregation,outler,weight）


## 测试环境

### 虚拟环境

-   cd performanceTest
-   virtualenv venv
-   source ./bin/activate
-   cd ..
-   pip install -r requirement.txt

### chromedriver

-   目录中已存在mac版本chromedriver
-   其他版本下载地址：http://chromedriver.storage.googleapis.com/index.html
-   下载与本地chrome同版本的chromedriver覆盖在本目录中

## 采样实验

### 平均分桶与自适应分桶相似度对比

#### 实验内容

-   使用控制变量法，保持其余变量相同，比较平均分桶与自适应分桶采样相似度差异。

#### 实验过程

-   对比实验在bucketTest包中，startURL.txt是访问REST API的参数文件，可以对参数进行修改。
-   运行apprun.py可以生成MSSSIM,PAE,PMSE结果，在对应的文本文档里。
-   运行apprun_color.py可以画出有区分性的图表。

#### 运行

-   cd interfaceTest 
-   python apprun.py
-   python apprun_color.py

### 桶内算子采样相似度对比

#### 实验内容

-   使用控制变量法，保持其余变量相同，比较桶内算子M4、aggregation、outlier、sample、weight采样相似度差异。

#### 实验过程

-   对比实验在BucketSampleTest包中，subway.txt、intel.txt、kpi.txt、particle.txt、bus.txt是访问REST API的参数文件，可以对参数进行修改。
-   运行subwayrun.py、intelrun.py、kpirun.py、particlerun.py、busrun.py可以生成MSSSIM,PAE,PMSE结果，在1.txt~12.txt对应的文本文档里。
-   plt.py可以画出有MSSSIM,PAE,PMSE指标的综合性图表。

#### 运行

-   cd BucketSampleTest 
-   python subwayrun.py
-   python intelrun.py
-   python kpirun.py
-   python particlerun.py
-   python busrun.py
-   python plt.py

### 权重计算方式采样相似度对比

#### 实验内容

-   使用控制变量法，保持其余变量相同，比较权重计算方式（联合权重、单独权重）采样相似度差异。

#### 实验过程

-   对比实验在interfaceTest1包中，subway.txt、intel.txt、kpi.txt、particle.txt、bus.txt是访问REST API的参数文件，可以对参数进行修改。
-   运行m4run.py、aggregationrun.py、outlierrun.py、weightrun.py、samplerun.py可以生成MSSSIM,PAE,PMSE结果，在1.txt~12.txt对应的文本文档里。
-   plt.py可以画出有MSSSIM,PAE,PMSE指标的综合性图表。

#### 运行

-   cd interfaceTest1 
-   python m4run.py
-   python aggregationrun.py
-   python outlierrun.py
-   python weightrun.py
-   python samplerun.py
-   python plt.py


### 桶内算子性能测试

- 此次测试主要通过对比自适应分桶和平均分桶使用不同的桶内采样方法（M4,aggregation，random，outlier，weight）所花费的时间来测试桶内算子的性能

#### 数据写入
- 数据写入使用的java脚本，该文件在项目代码的util包下，即./complete/src/main/java/hello/fast/util/SamplePerformanceTest.java。
- 将SamplePerformanceTest.java文件中的innerURL、innerUserName、innerPassword修改为要写入的postgreSQL数据库地址，即可自动化写入。
- 数据写入会建4张表，表中分别存10W,100W,1000W，1亿条数据，数据内容为两列：1列时间数据，一列随机生成的20以内自然数，若有需求可另行添加。

#### 中间层运行

- sampleTest文件夹下gs-rest-service-0.1.0.jar包对中间层略作修改，返回的不是经过采样的数据，是自适应分桶或平均分桶过程所花费的时间
- 通过fast.config可以对中间层参数进行修改。
- 中间层运行：java -jar gs-rest-service-0.1.0.jar

#### 测试脚本运行

- 测试的环境可以使用以上接口测试所建立的虚拟环境
- restAPI数据可以通过sampletest.txt文件进行修改
- 测试脚本运行：python performance_test_sample.py
- 测试结果会在sampleresult.txt中生成




