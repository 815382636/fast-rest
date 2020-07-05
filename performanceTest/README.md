# 自适应采样中间层性能测试

通过计算面积图MS-SSIM、折线图PAE、面积图均方误差、折线图均方误差、折线图MS-SSIM来比较采样结果与原数据的画图效果

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

-   运行的文件为py文件中的param_file变量
-   若接口有修改，请修改目录下的以sf开头的txt文件
-   py文件中的param_file变量代表要使用的txt，start_step代表从哪里开始，percent代表timeLimit,alpha代表valueLimit

### 运行

-   python autorun.py 或 python autorun-color.py