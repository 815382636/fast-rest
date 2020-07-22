import requests
import json
params = {"amount":500,"columns":"weight","database":"sampletest1","dbtype":"postgresql","password":"1111aaaa","timeseries":"sample10","url":"jdbc:postgresql://192.168.10.172:5432/","username":"postgres", "valueLimit":11, "sample":"simple,m4"}

param_file ="sampletest.txt"
param_lines = open(param_file).read().split("\n")
result = open("sampleresult.txt", "a")
for i in param_lines:
    jsondata =json.loads(i)
    print(i)
    print(jsondata.get("timeseries"))
    response = requests.get("http://127.0.0.1:9091/sample", jsondata)
    if response.status_code ==200:
        result.write(jsondata.get("timeseries")+"   "+jsondata.get("sample")+"   "+response.text+"\n")
    else:
        result.write(jsondata.get("timeseries") + "   " + jsondata.get("sample") + "   " +"sample defeat")
result.close()


