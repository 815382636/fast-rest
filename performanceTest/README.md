# 自适应采样中间层性能测试

通过计算面积图MS-SSIM、折线图PAE、面积图均方误差、折线图均方误差、折线图MS-SSIM来比较采样结果与原数据的相似性程度

## 环境建立

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

## 运行设置

### 运行文件

-   interfaceTest中 是对比测试单独权重和综合权重单变量MSSSIM、PAE、PMSE指标的测试脚本
-   interfaceTest1中 是对比测试单独权重和综合权重两个变量MSSSIM、PAE、PMSE指标的测试脚本
-   运行apprun.py 即可运行测试脚本
-   脚本生成的折线图、面积图会存在在xxx_png中。MSSSIM、PAE、PMSE会存在目录的txt文件中

### 运行

-   cd interfaceTest 或 cd interfaceTest1
-   python apprun.py 