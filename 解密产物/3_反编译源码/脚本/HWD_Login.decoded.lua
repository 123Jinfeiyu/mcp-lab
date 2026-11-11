function onLoadEvent(handle)
  local ??????????????? = _ENV["护卫盾_初始化"]("www.code2bot.cn", 443, "5e039e9a3f29d31be34590275ebe1a34", "76087826-78a3-463a-be8c-c3f386a85d5c", "5mYrgqRXA4tBmSxPeN5hfVBOTtOvmt3Q", "[data]AS9jL[key]GBTa7", "[data]fbHBV[key]iwwbE", "a9b093308c8c309d4273639342594a5c")
  if ??????????????? == false then
    toast("初始化失败", 0, 0, 12)
    sleep(1000)
    exitScript()
  end
  if _ENV["护卫盾_取软件数据"]("version") ~= _ENV["全局_本地版本号"] then
    if _ENV["护卫盾_取软件版本数据"](_ENV["全局_本地版本号"], "forceUpdate") == "yes" then
      toast("本软件版本已停用,请更新最新版本", 0, 0, 12)
      sleep(5000)
      exitScript()
    else
      toast("发现新版本,请联系客服获取最新版本", 0, 0, 12)
    end
  end
end
function onClickEvent(handle, id)
  print(handle, id)
  if id == "按钮_注册" then
    if getEditText(handle, "输入框_注册用户名") == "" then
      toast("请输入注册用户名", 0, 0, 12)
      return
    end
    if getEditText(handle, "输入框_注册密码") == "" or getEditText(handle, "输入框_注册重复密码") == "" then
      toast("请输入密码 (6-16位).", 0, 0, 12)
      return
    end
    if getEditText(handle, "输入框_注册密码") ~= getEditText(handle, "输入框_注册重复密码") then
      toast("两次密码输入不同,请重新输入.", 0, 0, 12)
      return
    end
    if not getEditText(handle, "输入框_注册邮箱"):find("@") or not getEditText(handle, "输入框_注册邮箱"):find(".") then
      toast("请输入正确的安全邮箱,密码找回唯一方式.", 0, 0, 12)
      return
    end
    local ???????????? = _ENV["护卫盾_注册通行证"](getEditText(handle, "输入框_注册用户名"), getEditText(handle, "输入框_注册密码"), getEditText(handle, "输入框_注册邮箱"), getEditText(handle, "输入框_推荐人"), "")
    if ???????????? == false then
      toast(_ENV["护卫盾_取错误信息"](), 0, 0, 12)
    else
      toast("注册成功,请充值或登录", 0, 0, 12)
    end
  end
  if id == "按钮_充值" then
    if getEditText(handle, "输入框_充值用户名") == "" then
      toast("请输入充值账号", 0, 0, 12)
      return
    end
    if getEditText(handle, "输入框_充值卡密") == "" then
      toast("请输入充值卡密", 0, 0, 12)
      return
    end
    local ???????????? = _ENV["护卫盾_充值"](getEditText(handle, "输入框_充值用户名"), getEditText(handle, "输入框_充值卡密"), "")
    if ???????????? == false then
      toast(_ENV["护卫盾_取错误信息"](), 0, 0, 12)
    elseif _ENV["护卫盾_取软件数据"]("type") == "0" then
      toast("充值成功,到期时间为:" .. _ENV["到期时间回调"], 0, 0, 12)
    else
      toast("充值成功,点数余额为:" .. tostring(_ENV["点数余额回调"]), 0, 0, 12)
    end
  end
  if id == "按钮_发送验证码" then
    if getEditText(handle, "输入框_改密用户名") == "" then
      toast("请输入用户名", 0, 0, 12)
      return
    end
    if not getEditText(handle, "输入框_改密邮箱"):find("@") or not getEditText(handle, "输入框_改密邮箱"):find(".") then
      toast("请输入正确的安全邮箱", 0, 0, 12)
      return
    end
    local ?????????????????? = _ENV["护卫盾_发送验证邮件"](getEditText(handle, "输入框_改密用户名"), getEditText(handle, "输入框_改密邮箱"), "")
    if ?????????????????? == false then
      toast(_ENV["护卫盾_取错误信息"](), 0, 0, 12)
    else
      toast("邮件发送成功,请前往邮箱内查看,如未收到,请查看是否在垃圾箱中.", 0, 0, 12)
      setViewVisible(handle, "按钮_发送验证码", 8)
    end
  end
  if id == "按钮_改密" then
    if getEditText(handle, "输入框_改密用户名") == "" then
      toast("请输入用户名", 0, 0, 12)
      return
    end
    if getEditText(handle, "输入框_改密密码") == "" or getEditText(handle, "输入框_改密重复密码") == "" then
      toast("请输入密码 (6-16位).", 0, 0, 12)
      return
    end
    if getEditText(handle, "输入框_改密密码") ~= getEditText(handle, "输入框_改密重复密码") then
      toast("两次密码输入不同,请重新输入.", 0, 0, 12)
      return
    end
    if getEditText(handle, "输入框_改密验证码") == "" then
      toast("请输入邮箱验证码", 0, 0, 12)
      return
    end
    local ???????????? = _ENV["护卫盾_修改密码"](getEditText(handle, "输入框_改密用户名"), getEditText(handle, "输入框_改密密码"), getEditText(handle, "输入框_改密验证码"), "")
    if ???????????? == false then
      toast(_ENV["护卫盾_取错误信息"](), 0, 0, 12)
    else
      toast("密码修改成功,请牢记新密码.", 0, 0, 12)
    end
  end
end
function onCloseEvent(handle)
end
function onSelectEvent(handle, id, idx, sel)
end
action, arr = showUI("护卫盾_SDK.ui", 800, 400, onLoadEvent, onClickEvent, onCloseEvent, onSelectEvent)
for i, v in pairs(arr) do
  print("key:==>" .. i .. " val:==>" .. v)
end
setStopCallBack(function(error)
  if error then
    print("脚本异常结束了!!!")
  else
    print("脚本正常结束了!!!")
  end
  if _ENV["护卫盾_取错误码"]() ~= 200 then
    console.show()
    console.clearLog()
    console.setPos(100, 100)
    console.println(5, "异常停止:" .. _ENV["护卫盾_取错误信息"]())
    sleep(200)
  end
  _ENV["护卫盾_登出"]()
end)
if action == 1 then
  local ???????????? = _ENV["护卫盾_登录"](arr["输入框_登录用户名"], arr["输入框_登录密码"], "", _ENV["全局_本地版本号"])
  local ?????????TOKEN = _ENV["护卫盾_取用户资料"]("token")
  local ?????????TOKEN = MD5(arr["输入框_登录用户名"] .. "5mYrgqRXA4tBmSxPeN5hfVBOTtOvmt3Q")
  if ?????????TOKEN ~= ?????????TOKEN then
    exitScript()
  end
  beginThread(_ENV["护卫盾_心跳"], tonumber(_ENV["护卫盾_取软件数据"]("heartbeattime")))
else
  toast("再见!", 0, 0, 12)
  sleep(3000)
  exitScript()
end
