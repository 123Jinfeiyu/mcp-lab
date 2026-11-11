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
print("\230\143\146\228\187\182\229\136\157\229\167\139\229\140\150\229\188\128\229\167\139")
local apkname = "plugdemo.apk"
apkname = "app-release.apk"
libjavacaller = require("libjavacaller")
local apkpath = getWorkPath() .. "/" .. apkname
extractAssets("\230\138\164\229\141\171\231\155\190_SDK.rc", getWorkPath(), apkname)
libjavacaller.init(getWorkPath())
print("\230\143\146\228\187\182\229\136\157\229\167\139\229\140\150\229\174\140\230\136\144\229\188\128\229\167\139\230\179\168\229\134\140apk")
apkid = libjavacaller.registerApk(apkpath)
print("\230\179\168\229\134\140apk\229\174\140\230\136\144\229\188\128\229\167\139\232\176\131\231\148\168java\229\135\189\230\149\176")
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
