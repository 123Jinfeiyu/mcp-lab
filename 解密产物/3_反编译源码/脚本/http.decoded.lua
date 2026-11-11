function onJavaCallWithString(str)
  print("onJavaCallWithString:", tickCount(), str)
  return "hello"
end
function onJavaCallWithInt(str)
  print("onJavaCallWithInt:", tickCount(), str)
  return 123
end
function onJavaCallWithBool(str)
  print("onJavaCallWithBool:", tickCount(), str)
  return true
end
function onJavaCallWithVoid(str)
  print("onJavaCallWithVoid:", tickCount(), str)
end
function onHttpGetResult(str)
  print("onHttpGetResult:", str)
end
print("插件初始化开始")
local apkname = "plugdemo.apk"
apkname = "app-release.apk"
libjavacaller = require("libjavacaller")
local apkpath = getWorkPath() .. "/" .. apkname
extractAssets("护卫盾_SDK.rc", getWorkPath(), apkname)
libjavacaller.init(getWorkPath())
print("插件初始化完成开始注册apk")
apkid = libjavacaller.registerApk(apkpath)
print("注册apk完成开始调用java函数")
function javaHttpGetHead(url)
  local rep_head = libjavacaller.invokeWithString(apkid, "com/example/plug/Plugin", "httpHeadRequest", url)
  return rep_head
end
function javaHttpGet(url)
  local rep_get = libjavacaller.invokeWithString(apkid, "com/example/plug/Plugin", "httpGetRequest", url)
  return rep_get
end
function javaHttpPost(postTable, url)
  local postData = jsonLib.encode(postTable)
  local rep_post = libjavacaller.invokeWithString(apkid, "com/example/plug/Plugin", "httpPostRequest", url, postData)
  return rep_post
end
