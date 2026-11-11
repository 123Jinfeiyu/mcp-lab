function getuuid()
  local seed = {
    "e",
    "1",
    "2",
    "3",
    "4",
    "5",
    "6",
    "7",
    "8",
    "9",
    "a",
    "b",
    "c",
    "d",
    "e",
    "f"
  }
  local tb = {}
  for i = 1, 32 do
    table.insert(tb, seed[math.random(1, 16)])
  end
  local sid = table.concat(tb)
  return string.format("%s-%s-%s-%s-%s", string.sub(sid, 1, 8), string.sub(sid, 9, 12), string.sub(sid, 13, 16), string.sub(sid, 17, 20), string.sub(sid, 21, 32))
end
_ENV["护卫盾_发送封包"] = function(tmp)
  sendTime = 0
  while true do
    uuid = getuuid()
    local t = tostring(os.time())
    token = MD5(_ENV["全局_软件信息"]["软件ID"] .. _ENV["全局_软件信息"]["通讯秘钥"] .. t)
    local sendData = tmp
    sendData.sid = _ENV["全局_软件信息"]["软件ID"]
    sendData.ket = _ENV["全局_软件信息"]["通讯秘钥"]
    sendData.uuid = uuid
    sendData.t = t
    sendData.m1 = token
    local jbpath = getWorkPath()
    jbpath = jbpath:gsub("/root", "/脚本/HWD.lua")
    sendData.m2 = fileMD5(jbpath)
    sendData.m3 = _ENV["全局_软件信息"]["模块MD5"]
    sendData.mcode = _ENV["护卫盾_取机器码"]()
    sendData.webkey = _ENV["全局_软件信息"]["通用秘钥"]
    sendData.clientid = _ENV["全局_软件信息"]["客户端ID"]
    local sendDataJson = jsonLib.encode(sendData)
    sendDataEncode = string.lower(Encrypt(sendDataJson, _ENV["全局_软件信息"]["通讯秘钥"], utf8.left(_ENV["全局_软件信息"]["通讯秘钥"], 16)))
    local sendDataString = _ENV["全局_软件信息"]["客户端sign算法"]:gsub("%[data%]", sendDataEncode)
    sendDataString = sendDataString:gsub("%[key%]", _ENV["全局_软件信息"]["通讯秘钥"])
    local sendDataEncodeSign = MD5(sendDataString)
    local url = "https://" .. _ENV["全局_软件信息"]["授权域名"] .. "/ac.php?s=" .. MD5(_ENV["全局_软件信息"]["软件ID"]) .. "&t=" .. tostring(os.time())
    local sendDataWpe = {
      sign = sendDataEncodeSign,
      data = sendDataEncode
    }
    local resultDataString = javaHttpPost(sendDataWpe, url)
    sendState = true
    if resultDataString == "" then
      sendState = false
      sendTime = sendTime + 1
    end
    if sendState == true then
      local resultDataTable = jsonLib.decode(resultDataString)
      local resultDataTable = jsonLib.decode(resultDataTable.body)
      resultSign = resultDataTable.sign
      resultData = resultDataTable.data
    end
    if sendState ~= true and not (sendTime > 3) then
    end
  end
  if sendState == false then
    _ENV["全局_错误信息"]["错误码"] = "407"
    _ENV["全局_错误信息"]["错误内容"] = "[407]网络连接失败."
    return "{}"
  end
  local sign = _ENV["全局_软件信息"]["服务端sign算法"]:gsub("%[data%]", resultData)
  sign = sign:gsub("%[key%]", _ENV["全局_软件信息"]["通讯秘钥"])
  sign = MD5(sign)
  if sign ~= resultSign then
    _ENV["全局_错误信息"]["错误码"] = "406"
    _ENV["全局_错误信息"]["错误内容"] = "[406]数据校验失败."
    return "{}"
  end
  resultData = Decrypt(resultData, _ENV["全局_软件信息"]["通讯秘钥"], utf8.left(_ENV["全局_软件信息"]["通讯秘钥"], 16))
  local resultTable = jsonLib.decode(resultData)
  local token1 = MD5(token .. resultTable.t)
  if resultTable.uuid ~= uuid or token1 ~= resultTable.token then
    _ENV["全局_错误信息"]["错误码"] = "406"
    _ENV["全局_错误信息"]["错误内容"] = "[406]数据校验失败."
    return "{}"
  else
    _ENV["全局_错误信息"]["错误码"] = resultTable.code
    _ENV["全局_错误信息"]["错误内容"] = resultTable.msg
    return resultData
  end
end
_ENV["护卫盾_初始化"] = function(????????????, ????????????, ????????????, ??????ID, ????????????, ?????????sign??????, ?????????sign??????, ??????MD5)
  _ENV["全局_软件信息"]["授权域名"] = ????????????
  _ENV["全局_软件信息"]["访问端口"] = ????????????
  _ENV["全局_软件信息"]["通用秘钥"] = ????????????
  _ENV["全局_软件信息"]["软件ID"] = ??????ID
  _ENV["全局_软件信息"]["通讯秘钥"] = ????????????
  _ENV["全局_软件信息"]["客户端sign算法"] = ?????????sign??????
  _ENV["全局_软件信息"]["服务端sign算法"] = ?????????sign??????
  _ENV["全局_软件信息"]["模块MD5"] = ??????MD5
  _ENV["全局_软件信息"]["客户端ID"] = getuuid()
  local data = {action = "init"}
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  end
  _ENV["全局_软件数据"]["软件名"] = _ENV["封包返回"].result.name
  _ENV["全局_软件数据"]["验证码特征"] = _ENV["封包返回"].result.captcha
  _ENV["全局_软件数据"]["软件版本信息"] = _ENV["封包返回"].result.version
  _ENV["全局_软件数据"]["最新版本号"] = _ENV["取最新版本号"]("<!!!>", "<!!!>", 1)
  _ENV["全局_软件数据"]["公告源码"] = _ENV["封包返回"].result.notice
  _ENV["全局_软件数据"].QQ = _ENV["封包返回"].result.qq
  _ENV["全局_软件数据"]["登录方式"] = _ENV["封包返回"].result.login
  _ENV["全局_软件数据"]["转绑扣除"] = tostring(_ENV["封包返回"].result.deduct)
  _ENV["全局_软件数据"]["计费模式"] = tostring(_ENV["封包返回"].result.type)
  _ENV["全局_软件数据"]["登录页图片"] = _ENV["封包返回"].result.loginimg
  _ENV["全局_软件数据"]["心跳周期"] = tostring(_ENV["封包返回"].result.heartbeatTime)
  _ENV["全局_软件数据"]["客户端IP"] = _ENV["封包返回"].result.clientip
  website = _ENV["封包返回"].result.website
  return true
end
_ENV["取最新版本号"] = function(oldVer, name, type)
  local vstable = jsonLib.decode(_ENV["全局_软件数据"]["软件版本信息"])
  for i = 1, #vstable do
    local newVer = vstable[i].newVer
    if oldVer == vstable[i].oldVer and type == 0 then
      return vstable[#vstable][name]
    end
  end
  return vstable[#vstable].newVer
end
_ENV["护卫盾_取错误信息"] = function()
  return _ENV["全局_错误信息"]["错误内容"]
end
_ENV["护卫盾_取错误码"] = function()
  return tonumber(_ENV["全局_错误信息"]["错误码"])
end
_ENV["护卫盾_取软件数据"] = function(?????????)
  if ????????? == "name" then
    return _ENV["全局_软件数据"]["软件名"]
  elseif ????????? == "captcha" then
    return _ENV["全局_软件数据"]["验证码特征"]
  elseif ????????? == "versioninfo" then
    return _ENV["全局_软件数据"]["软件版本信息"]
  elseif ????????? == "version" then
    return _ENV["全局_软件数据"]["最新版本号"]
  elseif ????????? == "notice" then
    return _ENV["全局_软件数据"]["公告源码"]
  elseif ????????? == "qq" then
    return _ENV["全局_软件数据"].QQ
  elseif ????????? == "para" then
    return _ENV["全局_软件数据"]["软件自定义常量"]
  elseif ????????? == "loginimg" then
    return _ENV["全局_软件数据"]["登录页图片"]
  elseif ????????? == "website" then
    return _ENV["全局_软件数据"]["官网地址"]
  elseif ????????? == "heartbeattime" then
    return _ENV["全局_软件数据"]["心跳周期"]
  elseif ????????? == "clientip" then
    return _ENV["全局_软件数据"]["客户端IP"]
  elseif ????????? == "type" then
    return _ENV["全局_软件数据"]["计费模式"]
  elseif ????????? == "deduct" then
    return _ENV["全局_软件数据"]["转绑扣除"]
  elseif ????????? == "login" then
    return _ENV["全局_软件数据"]["登录方式"]
  else
    return ""
  end
end
_ENV["护卫盾_取云端独立自定义常量"] = function(??????, ?????????)
  local data = {
    action = "getparam",
    fun = ??????,
    name = ?????????,
    user = _ENV["全局_用户数据"]["用户名"]
  }
  local ???????????? = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return ""
  else
    return ????????????.result.body
  end
end
_ENV["护卫盾_取软件版本数据"] = function(??????????????????, ?????????)
  return _ENV["取最新版本号"](??????????????????, ?????????, 0)
end
_ENV["护卫盾_取软件自定义常量节点值"] = function(?????????)
  local cltable = jsonLib.decode(_ENV["全局_软件数据"]["软件自定义常量"])
  return cltable[?????????]
end
_ENV["护卫盾_取机器码"] = function()
  local GID = sh.Read("/sdcard/Gid.txt")
  if not GID then
    sh.Write("/sdcard/Gid.txt", tostring(os.time()))
    GID = sh.Read("/sdcard/Gid.txt")
  end
  _ENV["机器码"] = MD5(GID)
  _ENV["机器码分段"] = utf8.left(_ENV["机器码"], 8) .. "-" .. utf8.mid(_ENV["机器码"], 9, 4) .. "-" .. utf8.mid(_ENV["机器码"], 13, 4) .. "-" .. utf8.mid(_ENV["机器码"], 17, 4) .. "-" .. utf8.right(_ENV["机器码"], 12)
  return _ENV["机器码分段"]
end
_ENV["护卫盾_注册通行证"] = function(?????????, ??????, ??????, ?????????, ?????????)
  local data = {
    action = "user",
    fun = "reg",
    username = ?????????,
    password = ??????,
    mail = ??????,
    referrer = ?????????,
    code = ?????????
  }
  _ENV["护卫盾_发送封包"](data)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    return true
  end
end
_ENV["护卫盾_发送验证邮件"] = function(?????????, ??????, ?????????)
  local data = {
    action = "user",
    fun = "sendMail",
    username = ?????????,
    mail = ??????,
    code = ?????????
  }
  _ENV["护卫盾_发送封包"](data)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    return true
  end
end
_ENV["护卫盾_修改密码"] = function(?????????, ?????????, ???????????????, ?????????)
  print(???????????????)
  local data = {
    action = "user",
    fun = "resetPwd",
    username = ?????????,
    password = ?????????,
    mailcode = ???????????????,
    code = ?????????
  }
  print(data)
  _ENV["护卫盾_发送封包"](data)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    return true
  end
end
_ENV["护卫盾_充值"] = function(?????????, ????????????, ?????????)
  local data = {
    action = "recharge",
    user = ?????????,
    card = ????????????,
    code = ?????????
  }
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    _ENV["到期时间回调"] = _ENV["封包返回"].result.endtime
    _ENV["点数余额回调"] = tonumber(_ENV["封包返回"].result.point)
    _ENV["账户余额回调"] = tonumber(_ENV["封包返回"].result.balance)
    return true
  end
end
_ENV["护卫盾_加入黑名单"] = function(???????????????, ????????????)
  local data = {
    action = "addBlacklist",
    code = ???????????????,
    remark = ????????????
  }
  _ENV["护卫盾_发送封包"](data)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    return true
  end
end
_ENV["护卫盾_登录"] = function(?????????, ??????, ?????????, ???????????????)
  local data = {
    action = "login",
    user = ?????????,
    pwd = ??????,
    code = ?????????,
    client_version = ???????????????
  }
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  _ENV["全局_用户数据"]["用户名"] = ?????????
  _ENV["全局_用户数据"]["密码"] = ??????
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    _ENV["全局_软件数据"]["软件自定义常量"] = _ENV["封包返回"].result.softpara
    _ENV["全局_用户数据"]["用户自定义常量"] = _ENV["封包返回"].result.para
    _ENV["全局_用户数据"]["到期时间"] = _ENV["封包返回"].result.endtime
    _ENV["全局_用户数据"]["点数余额"] = tonumber(_ENV["封包返回"].result.point)
    _ENV["全局_用户数据"]["绑定数据"] = _ENV["封包返回"].result.bind
    _ENV["全局_用户数据"]["账户余额"] = tonumber(_ENV["封包返回"].result.balance)
    _ENV["全局_用户数据"]["登录token"] = _ENV["封包返回"].result.loginToken
    _ENV["全局_用户数据"]["登录auth"] = _ENV["封包返回"].result.loginAuth
    return true
  end
end
_ENV["护卫盾_取用户资料"] = function(?????????)
  if ????????? == "username" then
    return _ENV["全局_用户数据"]["用户名"]
  elseif ????????? == "password" then
    return _ENV["全局_用户数据"]["密码"]
  elseif ????????? == "endtime" then
    return _ENV["全局_用户数据"]["到期时间"]
  elseif ????????? == "point" then
    return _ENV["全局_用户数据"]["点数余额"]
  elseif ????????? == "para" then
    return _ENV["全局_用户数据"]["用户自定义常量"]
  elseif ????????? == "bind" then
    return _ENV["全局_用户数据"]["绑定数据"]
  elseif ????????? == "balance" then
    return _ENV["全局_用户数据"]["账户余额"]
  elseif ????????? == "token" then
    return _ENV["全局_用户数据"]["登录token"]
  elseif ????????? == "auth" then
    return _ENV["全局_用户数据"]["登录auth"]
  else
    return ""
  end
end
_ENV["护卫盾_取用户自定义常量节点值"] = function(?????????)
  local cltable = jsonLib.decode(_ENV["全局_用户数据"]["用户自定义常量"])
  return cltable[?????????]
end
_ENV["护卫盾_扣点"] = function(????????????, ????????????, ????????????)
  local data = {
    action = "deductpoint",
    user = _ENV["全局_用户数据"]["用户名"],
    num = tostring(????????????),
    msg = ????????????,
    interval = tostring(????????????)
  }
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  _ENV["全局_用户数据"]["点数余额"] = tonumber(_ENV["封包返回"].result.point)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    return true
  end
end
_ENV["护卫盾_扣时"] = function(????????????, ????????????, ????????????)
  local data = {
    action = "deducttime",
    user = _ENV["全局_用户数据"]["用户名"],
    num = tostring(????????????),
    msg = ????????????,
    interval = tostring(????????????)
  }
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  _ENV["全局_用户数据"]["到期时间"] = _ENV["封包返回"].result.endtime
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    return true
  end
end
_ENV["护卫盾_扣除余额"] = function(????????????, ????????????, ????????????)
  local data = {
    action = "deductbalance",
    user = _ENV["全局_用户数据"]["用户名"],
    num = tostring(????????????),
    msg = ????????????,
    interval = tostring(????????????)
  }
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  _ENV["全局_用户数据"]["账户余额"] = tonumber(_ENV["封包返回"].result.balance)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    return true
  end
end
_ENV["护卫盾_绑定用户数据"] = function(????????????)
  local data = {
    action = "bindstr",
    user = _ENV["全局_用户数据"]["用户名"],
    pwd = _ENV["全局_用户数据"]["密码"],
    str = ????????????
  }
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  _ENV["全局_用户数据"]["账户余额"] = tonumber(_ENV["封包返回"].result.balance)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    _ENV["全局_用户数据"]["绑定数据"] = ????????????
    return true
  end
end
_ENV["护卫盾_绑定本机"] = function(?????????)
  local data = {action = "bind", user = ?????????}
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  _ENV["全局_用户数据"]["账户余额"] = tonumber(_ENV["封包返回"].result.balance)
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return false
  else
    _ENV["全局_用户数据"]["绑定数据"] = _ENV["绑定内容"]
    return true
  end
end
_ENV["护卫盾_调用PHP函数"] = function(?????????, ?????????)
  local paras = splitStr(encodeUrl(?????????), encodeUrl(","))
  local data = {
    action = "callPHP",
    fun = ?????????,
    para = jsonLib.encode(paras),
    user = _ENV["全局_用户数据"]["用户名"]
  }
  _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return ""
  else
    return _ENV["封包返回"].result.result
  end
end
_ENV["护卫盾_心跳"] = function(????????????, ????????????)
  while true do
    local data = {
      action = "heartbeat",
      user = _ENV["全局_用户数据"]["用户名"],
      auth = ????????????
    }
    _ENV["封包返回"] = jsonLib.decode(_ENV["护卫盾_发送封包"](data))
    if not ???????????? then
      _ENV["全局_用户数据"]["用户名"] = _ENV["封包返回"].result.user
      _ENV["全局_用户数据"]["密码"] = _ENV["封包返回"].result.password
      _ENV["全局_用户数据"]["登录token"] = _ENV["封包返回"].result.loginToken
      _ENV["全局_软件信息"]["客户端ID"] = _ENV["封包返回"].result.clientId
    end
    if _ENV["全局_错误信息"]["错误码"] ~= "200" then
      exitScript()
    end
    _ENV["全局_软件数据"]["软件自定义常量"] = _ENV["封包返回"].result.softpara
    _ENV["全局_用户数据"]["用户自定义常量"] = _ENV["封包返回"].result.para
    _ENV["全局_用户数据"]["到期时间"] = _ENV["封包返回"].result.endtime
    _ENV["全局_用户数据"]["点数余额"] = tonumber(_ENV["封包返回"].result.point)
    _ENV["全局_用户数据"]["绑定数据"] = _ENV["封包返回"].result.bind
    _ENV["全局_用户数据"]["账户余额"] = tonumber(_ENV["封包返回"].result.balance)
    if ???????????? < 120 and ???????????? > 0 then
      sleep(120000)
    else
      sleep(???????????? * 1000)
    end
  end
end
_ENV["护卫盾_过滤HTML标签"] = function(html)
  html = string.gsub(html, "<script[%a%A]->[%a%A]-</script>", "")
  html = string.gsub(html, "<style[%a%A]->[%a%A]-</style>", "")
  html = string.gsub(html, "<[%a%A]->", "")
  return html
end
_ENV["护卫盾_登出"] = function()
  local data = {
    action = "logout",
    user = _ENV["全局_用户数据"]["用户名"]
  }
  jsonLib.decode(_ENV["护卫盾_发送封包"](data))
  if _ENV["全局_错误信息"]["错误码"] ~= "200" then
    return true
  else
    return false
  end
end
