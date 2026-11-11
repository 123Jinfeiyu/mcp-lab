require("File")
require("page")
require("EAD")
require("HWD")
require("http")
require("HWD_Login")
require("lib")
require("PaddLeOCR")
local action, arr = showUIEx("main.ui", 50, 50, 980, 1720)
_ENV["切换抖音"] = false
if arr["多选框_主配置"]:find("0") then
  _ENV["只回复"] = true
else
  _ENV["只回复"] = false
end
if arr["多选框_联系"]:find("0") then
  _ENV["分段"] = true
else
  _ENV["分段"] = false
end
if arr["多选框_联系"]:find("1") then
  _ENV["符号"] = true
else
  _ENV["符号"] = false
end
if arr["多选框_联系"]:find("2") then
  _ENV["单词"] = true
else
  _ENV["单词"] = false
end
_ENV["AI话术"] = true
_ENV["服务器ip"] = "81.68.192.85"
AImode = "deepseek-chat"
AIkey = "sk-f42e17f8d23f443fa653706352af94a6"
_ENV["运行间隔"] = tonumber(arr["运行间隔"])
_ENV["停留1"] = tonumber(arr["停留1"])
_ENV["停留2"] = tonumber(arr["停留2"])
if arr["单选框_模式"] == "0" then
  _ENV["私信控制"] = tonumber(arr["私信控制_搜索"])
else
  _ENV["私信控制"] = tonumber(arr["私信控制_UID"])
end
_ENV["刷视频"] = tonumber(arr["刷视频"])
_ENV["滑动次数"] = 1
_ENV["话术"] = {}
_ENV["联系方式"] = {}
_ENV["回复指针"] = {}
_ENV["消息按钮计数"] = 0
_ENV["搜索空次数"] = 0
_ENV["名字"] = arr["输入框_名字"]
local key = _ENV["护卫盾_取用户资料"]("username") .. "----" .. encodeUrl(_ENV["名字"])
table.insert(_ENV["联系方式"], arr["联系1"])
table.insert(_ENV["回复指针"], 0)
httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getpage&key=" .. key .. "&phone=" .. arr["联系1"])
print("http://" .. _ENV["服务器ip"] .. "/cx?type=getpage&key=" .. key .. "&phone=" .. arr["联系1"])
table.insert(_ENV["联系方式"], arr["联系2"])
table.insert(_ENV["回复指针"], 0)
httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getpage&key=" .. key .. "&phone=" .. arr["联系2"])
table.insert(_ENV["联系方式"], arr["联系3"])
table.insert(_ENV["回复指针"], 0)
httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getpage&key=" .. key .. "&phone=" .. arr["联系3"])
table.insert(_ENV["联系方式"], arr["联系4"])
table.insert(_ENV["回复指针"], 0)
httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getpage&key=" .. key .. "&phone=" .. arr["联系4"])
table.insert(_ENV["联系方式"], arr["联系5"])
table.insert(_ENV["回复指针"], 0)
httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getpage&key=" .. key .. "&phone=" .. arr["联系5"])
table.insert(_ENV["联系方式"], arr["联系6"])
table.insert(_ENV["回复指针"], 0)
httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getpage&key=" .. key .. "&phone=" .. arr["联系6"])
local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=pingbi")
_ENV["屏蔽词"] = splitStr(decodeUrl(ret), "\r\n")
_ENV["抖音坐标"] = {
  {166, 791},
  {428, 789}
}
_ENV["步骤"] = 0
_ENV["历史步骤"] = -2222
_ENV["过滤板"] = ""
_ENV["抖音序号"] = 1
_ENV["启动时间"] = os.time()
local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=shijian")
_ENV["停止时间"] = tonumber(ret)
w, h = getDisplaySize()
_ENV["首次回复"] = true
_ENV["历史标题"] = ""
_ENV["视频简介"] = ""
_ENV["店铺名"] = ""
_ENV["跳转次数"] = 0
_ENV["查找计数"] = 0
_ENV["跳转序号"] = 1
function main()
  nodeLib.updateNode()
  if _ENV["步骤"] == 0 then
    hudlog("返回桌面")
    keyPress("home")
    sleep(_ENV["运行间隔"] * 3)
    hudlog("再次按下Home键")
    keyPress("home")
    sleep(_ENV["运行间隔"] * 3)
    hudlog("打开" .. _ENV["抖音序号"] .. "号抖音")
    tap(_ENV["抖音坐标"][_ENV["抖音序号"]][1], _ENV["抖音坐标"][_ENV["抖音序号"]][2])
    sleep(5000)
    if _ENV["切换抖音"] == true then
      if _ENV["抖音序号"] == 1 then
        _ENV["跳转序号"] = 1
        _ENV["抖音序号"] = 2
      else
        _ENV["抖音序号"] = 1
        _ENV["跳转序号"] = 2
      end
    else
      _ENV["抖音序号"] = 1
      _ENV["跳转序号"] = 1
    end
    _ENV["步骤"] = 1
  elseif _ENV["步骤"] == 1 then
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme:id/v0a"
    }, false)
    if ????????? ~= nil then
      hudlog("退出聊天页")
      keyPress("back")
      sleep(_ENV["运行间隔"])
    else
      local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "com.bytedance.ies.dmt.ui.widget.DmtTextView",
        id = "com.ss.android.ugc.aweme:id/tv_title",
        text = "消息"
      }, false)
      if ???????????? ~= nil then
        hudlog("进入消息列表")
        _ENV["步骤"] = 2
      else
        local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.TextView",
          desc = "消息，按钮",
          text = "消息"
        }, false)
        if ???????????? ~= nil then
          hudlog("点击消息按钮")
          nodeLib.click(????????????)
          sleep(_ENV["运行间隔"])
        else
          hudlog("返回")
          keyPress("back")
          sleep(_ENV["运行间隔"] * 5)
        end
      end
    end
  elseif _ENV["步骤"] == 2 then
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme:id/red_tips_count_view"
    }, false)
    if ???????????? ~= nil then
      hudlog("点击关注红点")
      nodeLib.click(????????????)
      sleep(5000)
      hudlog("返回")
      keyPress("back")
      sleep(_ENV["运行间隔"])
    else
      _ENV["步骤"] = 3
    end
  elseif _ENV["步骤"] == 3 then
    nodeLib.updateNode()
    local ???????????? = nodeLib.findAll(0, 0, 0, 0, {
      class = "android.widget.Button",
      id = "com.ss.android.ugc.aweme:id/ycy"
    }, false)
    if ???????????? ~= nil then
      for i = 1, #???????????? do
        local ????????? = nodeLib.findInNode(????????????[i], {
          id = "com.ss.android.ugc.aweme:id/v-3"
        }, false, false)
        if ????????? ~= nil then
          local ??????????????? = nodeLib.findInNode(????????????[i], {
            id = "com.ss.android.ugc.aweme:id/tv_title"
          }, false, false)
          if ??????????????? ~= nil then
            _ENV["用户名"] = ???????????????.text
            nodeLib.click(???????????????)
            sleep(_ENV["运行间隔"])
            _ENV["步骤"] = 4
            _ENV["查找计数"] = 0
            _ENV["消息按钮计数"] = 0
            break
          end
        end
      end
      local ret = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme:id/0bl"
      }, false)
      if ret == nil and _ENV["步骤"] ~= 4 then
        if _ENV["首次回复"] == true then
          local ?????? = nodeLib.findOne(0, 0, 0, 0, {
            class = "android.widget.TextView",
            text = "首页"
          }, false)
          if ?????? ~= nil then
            hudlog("点击首页")
            nodeLib.click(??????)
            sleep(_ENV["运行间隔"])
            if _ENV["只回复"] == true then
              _ENV["步骤"] = 0
            else
              _ENV["步骤"] = 7
              _ENV["首次回复"] = false
            end
          end
        else
          local ?????? = nodeLib.findOne(0, 0, 0, 0, {
            class = "android.widget.TextView",
            text = "首页"
          }, false)
          if ?????? ~= nil then
            hudlog("点击首页")
            nodeLib.click(??????)
            sleep(_ENV["运行间隔"])
            _ENV["步骤"] = -999
            _ENV["首次回复"] = true
          end
        end
      elseif _ENV["步骤"] ~= 4 and _ENV["步骤"] ~= 7 and _ENV["步骤"] ~= 0 then
        local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.TextView",
          desc = "消息，按钮",
          text = "消息"
        }, false)
        if ???????????? ~= nil then
          hudlog("点击消息按钮")
          nodeLib.click(????????????)
          sleep(3000)
          _ENV["消息按钮计数"] = _ENV["消息按钮计数"] + 1
          if 5 < _ENV["消息按钮计数"] then
            _ENV["步骤"] = 7
            _ENV["首次回复"] = false
            _ENV["消息按钮计数"] = 0
          else
            _ENV["步骤"] = 1
          end
        end
      end
    end
  elseif _ENV["步骤"] == 4 then
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
    }, false)
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil or ????????? ~= nil then
      local ???V?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        id = "com.ss.android.ugc.aweme:id/szc"
      }, false)
      local ???V??????2 = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        id = "com.ss.android.ugc.aweme:id/wxw"
      }, false)
      if ???V?????? == nil and ???V??????2 == nil then
        hudlog("未发现蓝V标识")
        _ENV["步骤"] = 1
        return
      end
      hudlog("分析聊天记录")
      local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=glt&nicheng=" .. encodeUrl(_ENV["用户名"]))
      if ???????????? == "OK" then
        local ?????????????????? = nodeLib.findAll(0, 0, 0, 0, {
          class = "android.widget.TextView",
          id = "com.ss.android.ugc.aweme:id/content_layout"
        }, false)
        if ?????????????????? ~= nil then
          _ENV["聊天记录"] = ""
          for i = 1, #?????????????????? do
            print(??????????????????[i])
            _ENV["聊天记录"] = _ENV["聊天记录"] .. ??????????????????[i].text .. "\n"
          end
          if #_ENV["联系方式"] == 0 then
            hudlog("没有可发送的联系方式")
            while true do
              sleep(10000)
            end
          else
            _ENV["联系指针"] = 1
            if _ENV["分段"] == true and _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(1, 6)
            elseif _ENV["分段"] == true and _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(1, 4)
            elseif _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(3, 6)
            elseif _ENV["分段"] == true then
              _ENV["联系指针"] = rnd(1, 2)
            elseif _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(3, 4)
            elseif _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(5, 6)
            end
            _ENV["待回复联系方式"] = _ENV["联系方式"][_ENV["联系指针"]]
            local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gethuifuai")
            _ENV["ai策略"] = decodeUrl(ret)
            _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{聊天记录}", _ENV["聊天记录"])
            local aurl = "https://api.deepseek.com/chat/completions"
            local aAuthorization = "Bearer " .. AIkey
            local abody = {
              model = AImode,
              messages = {
                {
                  role = "user",
                  content = _ENV["ai策略"]
                }
              },
              stream = false
            }
            local adata = jsonLib.encode(abody)
            print(adata)
            local aheader = "Authorization:" .. aAuthorization .. "\n" .. "Content-Type: application/json"
            local fh, code = httpPost(aurl, adata, 30, aheader)
            local jsret = jsonLib.decode(fh)
            if code == 200 then
              _ENV["待发送"] = jsret.choices[1].message.content
              print(_ENV["待发送"])
              _ENV["步骤"] = 5
            end
          end
        else
          _ENV["查找计数"] = _ENV["查找计数"] + 1
          if 5 <= _ENV["查找计数"] then
            _ENV["联系指针"] = 1
            if _ENV["分段"] == true and _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(1, 6)
            elseif _ENV["分段"] == true and _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(1, 4)
            elseif _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(3, 6)
            elseif _ENV["分段"] == true then
              _ENV["联系指针"] = rnd(1, 2)
            elseif _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(3, 4)
            elseif _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(5, 6)
            end
            _ENV["待回复联系方式"] = _ENV["联系方式"][_ENV["联系指针"]]
            _ENV["待发送"] = "加我薇"
            _ENV["步骤"] = 5
          end
        end
      else
        hudlog("在数据库查询到该用户,返回")
        _ENV["步骤"] = 1
        keyPress("back")
        sleep(_ENV["运行间隔"])
      end
    end
  elseif _ENV["步骤"] == 5 then
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil then
      hudlog("输入回复话术")
      nodeLib.setText(?????????, _ENV["待发送"])
      sleep(_ENV["运行间隔"])
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
    }, false)
    if ????????? ~= nil then
      hudlog("输入回复话术")
      nodeLib.click(?????????)
      sleep(_ENV["运行间隔"])
      inputText(_ENV["待发送"])
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil and ?????????.text ~= "输入你的问题.." and ?????????.text ~= "?发送消息" and ?????????.text ~= "?输入你的问题.." then
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
    }, false)
    if ????????? ~= nil and ?????????.text ~= "输入咨询的内容..." then
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
    end
  elseif _ENV["步骤"] == 6 then
    local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=is3")
    if ret == "OK" then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getzdy3")
      _ENV["自定义"] = decodeUrl(ret)
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.setText(?????????, _ENV["自定义"])
        sleep(_ENV["运行间隔"])
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["自定义"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    _ENV["联系分割"] = splitStr(_ENV["待回复联系方式"], "|")
    if #_ENV["联系分割"] == 1 and (_ENV["联系指针"] == 3 or _ENV["联系指针"] == 4) then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getfuhao")
      _ENV["联系分割"][1] = decodeUrl(ret) .. "\r\n\r\n\r\n" .. _ENV["联系分割"][1]
    end
    for i = 1, #_ENV["联系分割"] do
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.setText(?????????, _ENV["联系分割"][i])
        sleep(_ENV["运行间隔"])
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["联系分割"][i])
        sleep(_ENV["运行间隔"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    sleep(_ENV["运行间隔"] * 5)
    keyPress("back")
    _ENV["步骤"] = 1
  elseif _ENV["步骤"] == 7 then
    toast("获取关键词", 0, 0, 12)
    local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=get")
    _ENV["关键词"] = decodeUrl(ret)
    if _ENV["关键词"] ~= "" then
      hudlog("成功获取:" .. _ENV["关键词"])
      _ENV["步骤"] = 8
    else
      sleep(_ENV["运行间隔"])
      hudlog("关键词获取失败")
    end
  elseif _ENV["步骤"] == 8 then
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      text = "搜索"
    }, false)
    if ???????????? ~= nil then
      _ENV["步骤"] = 9
    end
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      text = "首页"
    }, false)
    if ?????? ~= nil then
      sleep(_ENV["运行间隔"])
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {desc = ".*收藏.*"}, true)
      if ?????? == nil then
        hudlog("点击首页")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        desc = "搜索"
      }, false)
      if ?????? ~= nil then
        hudlog("点击搜索")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      id = "com.ss.android.ugc.aweme:id/som"
    }, false)
    if ???????????? ~= nil then
      hudlog("返回")
      keyPress("back")
      sleep(_ENV["运行间隔"])
    end
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.FrameLayout",
      desc = "关闭",
      id = "com.ss.android.ugc.aweme:id/cancel_btn"
    }, false)
    if ?????? ~= nil then
      nodeLib.click(??????)
      sleep(1000)
    end
  elseif _ENV["步骤"] == 9 then
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil then
      hudlog("输入关键词")
      nodeLib.setText(?????????, _ENV["关键词"])
      sleep(_ENV["运行间隔"])
      local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        text = "搜索"
      }, false)
      if ???????????? ~= nil then
        hudlog("点击搜索")
        nodeLib.click(????????????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 10
      end
    end
  elseif _ENV["步骤"] == 10 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.Button",
      text = "用户"
    }, false)
    if ?????? ~= nil then
      hudlog("点击用户")
      nodeLib.click(??????)
      sleep(_ENV["运行间隔"])
    end
    local ??????????????? = nodeLib.findAll(0, 0, w, h, {
      desc = "关注按钮",
      text = "关注按钮"
    }, false)
    if ??????????????? ~= nil then
      _ENV["步骤"] = 11
    end
    local ??? = nodeLib.findOne(0, 0, 0, 0, {
      text = "搜索结果为空"
    }, false)
    if ??? ~= nil then
      hudlog("搜索结果为空")
      _ENV["步骤"] = 7
      sleep(_ENV["运行间隔"])
      _ENV["搜索空次数"] = _ENV["搜索空次数"] + 1
      if 3 < _ENV["搜索空次数"] then
        hudlog("搜索结果为空超过3次,停止运行")
        while true do
          sleep(_ENV["运行间隔"])
        end
      end
    end
  elseif _ENV["步骤"] == 11 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.view.ViewGroup",
      desc = "筛选，按钮",
      text = ""
    }, false)
    if ?????? ~= nil then
      hudlog("点击筛选")
      nodeLib.click(??????)
      sleep(_ENV["运行间隔"])
      local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        text = "企业认证"
      }, false)
      if ???????????? ~= nil then
        sleep(_ENV["运行间隔"])
        hudlog("点击企业认证")
        nodeLib.click(????????????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 12
      end
    end
  elseif _ENV["步骤"] == 12 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.Button",
      text = "用户"
    }, false)
    if ?????? ~= nil then
      hudlog("点击用户")
      nodeLib.click(??????)
      sleep(_ENV["运行间隔"])
      local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        text = "企业认证"
      }, false)
      if ???????????? == nil then
        _ENV["步骤"] = 13
      end
    end
  elseif _ENV["步骤"] == 13 then
    local ??? = nodeLib.findOne(0, 0, 0, 0, {
      text = "搜索结果为空"
    }, false)
    if ??? ~= nil then
      _ENV["步骤"] = 7
      sleep(1000)
    end
    local ??????????????? = nodeLib.findAll(0, 0, w, h, {
      class = "com.lynx.tasm.behavior.ui.view.UIView",
      desc = "关注按钮",
      text = "关注按钮"
    }, false)
    if ??????????????? ~= nil then
      for i = 1, #??????????????? do
        local ???????????? = nodeLib.findPreNode(???????????????[i], false)
        if ???????????? ~= nil then
          _ENV["预处理"] = string.gsub(????????????.text, "，按钮", "")
          print(_ENV["预处理"])
          _ENV["信息分割"] = splitStr(_ENV["预处理"], "，")
          if 3 <= #_ENV["信息分割"] then
            _ENV["店铺名"] = _ENV["信息分割"][#_ENV["信息分割"] - 2]
            _ENV["粉丝"] = _ENV["信息分割"][#_ENV["信息分割"] - 1]
            _ENV["粉丝"] = string.match(_ENV["粉丝"], "%d+")
            if _ENV["粉丝"] == nil then
              _ENV["粉丝"] = "1"
            end
            _ENV["粉丝数"] = tonumber(_ENV["粉丝"])
          end
          if 1 <= _ENV["粉丝数"] and _ENV["粉丝数"] <= 50000 then
            local ????????? = nodeLib.findParentNode(???????????????[i], false)
            if ????????? ~= nil then
              intX, intY = findMultiColor(?????????.bounds.l, ?????????.bounds.t, ?????????.bounds.r, ?????????.bounds.b, "FFFFFF", "2|16|61C1FF|-5|8|61C1FF|5|9|61C1FF|8|3|61C1FF|-8|1|61C1FF|-9|-3|61C1FF|11|-3|61C1FF|16|6|FFFFFF|-14|7|FFFFFF", 0, 0.9)
              intX2, intY2 = findMultiColor(?????????.bounds.l, ?????????.bounds.t, ?????????.bounds.r, ?????????.bounds.b, "FCFDFD", "10|-8|F5F7F7|-9|-6|FEFEFE|-3|-9|1CAAF8|0|-13|1CAAF9|14|-5|1DAAF7|10|0|1DAAF6|6|5|1DAAF6|-12|2|1DAAF6", 0, 0.9)
              if intX > -1 and intY > -1 then
                if _ENV["本地过滤"](_ENV["店铺名"]) then
                  local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gl&nicheng=" .. encodeUrl(_ENV["店铺名"]))
                  if ???????????? == "OK" then
                    hudlog("进入用户页")
                    nodeLib.click(?????????)
                    sleep(_ENV["运行间隔"])
                    _ENV["步骤"] = 14
                    break
                  else
                    print(_ENV["店铺名"] .. "==>服务器重复验证失败")
                  end
                else
                  print(_ENV["店铺名"] .. "==>本地重复验证失败")
                end
              elseif intX2 > -1 and intY2 > -1 then
                if _ENV["本地过滤"](_ENV["店铺名"]) then
                  local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gl&nicheng=" .. encodeUrl(_ENV["店铺名"]))
                  if ???????????? == "OK" then
                    hudlog("进入用户页")
                    nodeLib.click(?????????)
                    sleep(_ENV["运行间隔"])
                    _ENV["步骤"] = 14
                    break
                  else
                    print(_ENV["店铺名"] .. "==>服务器重复验证失败")
                  end
                else
                  print(_ENV["店铺名"] .. "==>本地重复验证失败")
                end
              else
                print(_ENV["店铺名"] .. "==>未发现蓝V认证图标")
              end
            end
          else
            print(_ENV["店铺名"] .. "==>粉丝数不达标")
          end
          if i == #??????????????? and _ENV["步骤"] ~= 6 then
            sleep(500)
            _ENV["滑动"](w * 0.5, h * 0.8, w * 0.5, h * 0.2, 500)
            if _ENV["历史标题"] ~= _ENV["店铺名"] then
              _ENV["历史标题"] = _ENV["店铺名"]
              _ENV["滑动次数"] = 1
            else
              _ENV["滑动次数"] = _ENV["滑动次数"] + 1
              if 3 < _ENV["滑动次数"] then
                if os.time() - _ENV["启动时间"] >= _ENV["私信控制"] * 60 then
                  _ENV["步骤"] = 1
                else
                  _ENV["步骤"] = 7
                end
              end
            end
          end
        else
          local ???????????? = nodeLib.findParentNode(???????????????[i], false)
          local ???????????? = ????????????.bounds
          _ENV["粉丝数"] = 0
          local ???????????? = ocrEx(248, ????????????.t, ???????????????[i].bounds.l, ????????????.b, 50, 0, 0.6, 0.3, 2.0, true, true)
          if #???????????? >= 2 then
            _ENV["店铺名"] = ????????????[1].text
            _ENV["粉丝"] = ????????????[2].text
            _ENV["粉丝"] = string.match(_ENV["粉丝"], "%d*%.*%d+")
            _ENV["粉丝数"] = tonumber(_ENV["粉丝"])
            if string.find(????????????[2].text, "万", 1, true) ~= nil then
              _ENV["粉丝数"] = _ENV["粉丝数"] * 10000
            end
            print(_ENV["店铺名"])
            print(_ENV["粉丝数"])
          end
          if _ENV["粉丝数"] == nil then
            _ENV["粉丝数"] = 1
          end
          if 1 <= _ENV["粉丝数"] and _ENV["粉丝数"] <= 50000 then
            local ????????? = nodeLib.findParentNode(???????????????[i], false)
            if ????????? ~= nil then
              intX, intY = findMultiColor(?????????.bounds.l, ?????????.bounds.t, ?????????.bounds.r, ?????????.bounds.b, "FFFFFF", "2|16|61C1FF|-5|8|61C1FF|5|9|61C1FF|8|3|61C1FF|-8|1|61C1FF|-9|-3|61C1FF|11|-3|61C1FF|16|6|FFFFFF|-14|7|FFFFFF", 0, 0.9)
              intX2, intY2 = findMultiColor(?????????.bounds.l, ?????????.bounds.t, ?????????.bounds.r, ?????????.bounds.b, "FCFDFD", "10|-8|F5F7F7|-9|-6|FEFEFE|-3|-9|1CAAF8|0|-13|1CAAF9|14|-5|1DAAF7|10|0|1DAAF6|6|5|1DAAF6|-12|2|1DAAF6", 0, 0.9)
              if intX > -1 and intY > -1 then
                if _ENV["本地过滤"](_ENV["店铺名"]) then
                  local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gl&nicheng=" .. encodeUrl(_ENV["店铺名"]))
                  if ???????????? == "OK" then
                    hudlog("进入用户页")
                    nodeLib.click(?????????)
                    sleep(_ENV["运行间隔"])
                    _ENV["步骤"] = 14
                    break
                  else
                    print(_ENV["店铺名"] .. "==>服务器重复验证失败")
                  end
                else
                  print(_ENV["店铺名"] .. "==>本地重复验证失败")
                end
              elseif intX2 > -1 and intY2 > -1 then
                if _ENV["本地过滤"](_ENV["店铺名"]) then
                  local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gl&nicheng=" .. encodeUrl(_ENV["店铺名"]))
                  if ???????????? == "OK" then
                    hudlog("进入用户页")
                    nodeLib.click(?????????)
                    sleep(_ENV["运行间隔"])
                    _ENV["步骤"] = 14
                    break
                  else
                    print(_ENV["店铺名"] .. "==>服务器重复验证失败")
                  end
                else
                  print(_ENV["店铺名"] .. "==>本地重复验证失败")
                end
              else
                print(_ENV["店铺名"] .. "==>未发现蓝V认证图标")
              end
            end
          else
            print(_ENV["店铺名"] .. "==>粉丝数不达标")
          end
          if i == #??????????????? and _ENV["步骤"] ~= 6 then
            sleep(500)
            _ENV["滑动"](w * 0.5, h * 0.8, w * 0.5, h * 0.2, 500)
            if _ENV["历史标题"] ~= _ENV["店铺名"] then
              _ENV["历史标题"] = _ENV["店铺名"]
              _ENV["滑动次数"] = 1
            else
              _ENV["滑动次数"] = _ENV["滑动次数"] + 1
              if 3 < _ENV["滑动次数"] then
                if os.time() - _ENV["启动时间"] >= _ENV["私信控制"] * 60 then
                  _ENV["步骤"] = 7
                else
                  _ENV["步骤"] = 1
                end
              end
            end
          end
        end
      end
    end
  elseif _ENV["步骤"] == 14 then
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      text = "私密账号"
    }, true)
    if ???????????? ~= nil then
      hudlog("私密账号,返回")
      keyPress("back")
      sleep(_ENV["运行间隔"])
      _ENV["步骤"] = 999
    else
      local ??????????????? = nodeLib.findOne(0, 0, 0, 0, {
        id = "android:id/text1"
      }, false)
      local ??????????????? = nodeLib.findPreNode(0, 0, 0, 0, {
        class = "android.widget.TextView",
        package = "com.ss.android.ugc.aweme",
        text = "粉丝"
      }, false)
      local ??????????????? = nodeLib.findOne(0, 0, 0, 0, {
        desc = ".*复制名字和修改备注"
      }, true)
      local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
        desc = "抖音组织认证.*"
      }, true)
      if ??????????????? ~= nil and ??????????????? ~= nil and ??????????????? ~= nil then
        if ???????????? == nil then
          hudlog("未发现蓝V认证,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        _ENV["作品数"] = string.match(???????????????.text, "%d+")
        _ENV["店铺名"] = ???????????????.text
        _ENV["认证信息"] = ????????????.text
        local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gl&nicheng=" .. encodeUrl(_ENV["店铺名"]))
        if ???????????? ~= "OK" then
          hudlog("服务器重复验证失败,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        _ENV["粉丝"] = ???????????????.text
        _ENV["粉丝"] = string.match(_ENV["粉丝"], "%d*%.*%d+")
        _ENV["粉丝数"] = tonumber(_ENV["粉丝"])
        if string.find(???????????????.text, "万", 1, true) ~= nil then
          _ENV["粉丝数"] = _ENV["粉丝数"] * 10000
        end
        if 1 > _ENV["粉丝数"] or _ENV["粉丝数"] > 50000 then
          hudlog("粉丝数不符,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        if _ENV["作品数"] == nil then
          hudlog("无作品数,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        elseif 1 >= tonumber(_ENV["作品数"]) then
          hudlog("作品数为零,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        local ???????????? = false
        for n = 1, #_ENV["屏蔽词"] do
          if string.find(_ENV["店铺名"], _ENV["屏蔽词"][n], 1, true) ~= nil then
            ???????????? = true
            break
          end
          if string.find(_ENV["店铺名"], _ENV["屏蔽词"][n], 1, true) ~= nil then
            ???????????? = true
            break
          end
        end
        if ???????????? == true then
          hudlog("触发屏蔽,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        _ENV["步骤"] = 15
      end
    end
  elseif _ENV["步骤"] == 15 then
    local r = rnd(_ENV["停留1"], _ENV["停留2"])
    hudlog("主页停留" .. tostring(r) .. "秒")
    sleep(r * 1000)
    if _ENV["AI话术"] == true then
      _ENV["步骤"] = 16
    else
      _ENV["步骤"] = 18
    end
  elseif _ENV["步骤"] == 16 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.ImageView",
      id = "com.ss.android.ugc.aweme:id/cover"
    }, false)
    if ?????? ~= nil then
      hudlog("点击视频")
      nodeLib.click(??????)
      sleep(_ENV["运行间隔"])
      _ENV["步骤"] = 17
    else
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "android:id/text1"
      }, false)
      if ?????? ~= nil then
        hudlog("点击作品")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
  elseif _ENV["步骤"] == 17 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {desc = ".*收藏.*"}, true)
    if ?????? ~= nil then
      hudlog("获取视频简介")
      _ENV["视频简介"] = ""
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme:id/desc"
      }, false)
      if ?????? ~= nil then
        _ENV["视频简介"] = ??????.text
      end
      keyPress("back")
      sleep(_ENV["运行间隔"])
      _ENV["步骤"] = 18
    end
  elseif _ENV["步骤"] == 18 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {desc = "私信"}, false)
    if ?????? ~= nil then
      if ??????.text == "联系客服" then
        local ?????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.view.ViewGroup",
          desc = "更多"
        }, false)
        if ?????? ~= nil then
          hudlog("点击更多")
          nodeLib.click(??????)
          sleep(_ENV["运行间隔"])
        end
      else
        hudlog("点击私信")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      desc = "",
      text = "发私信"
    }, false)
    if ????????? ~= nil then
      hudlog("点击发私信")
      local ret = nodeLib.findParentNode(?????????, false)
      if ret ~= nil then
        nodeLib.click(ret)
        sleep(_ENV["运行间隔"])
      end
    end
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.ImageView",
      desc = "更多面板"
    }, false)
    if ???????????? ~= nil then
      _ENV["步骤"] = 19
      httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xieru&nicheng=" .. encodeUrl(_ENV["店铺名"]))
    else
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        _ENV["步骤"] = 19
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xieru&nicheng=" .. encodeUrl(_ENV["店铺名"]))
      else
        local ?????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.ImageView",
          desc = "表情"
        }, false)
        if ?????? ~= nil then
          _ENV["步骤"] = 19
          httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xieru&nicheng=" .. encodeUrl(_ENV["店铺名"]))
        end
      end
    end
  elseif _ENV["步骤"] == 19 then
    hudlog("获取话术")
    if _ENV["AI话术"] == false then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=huashu")
      _ENV["待发送"] = decodeUrl(ret)
      _ENV["步骤"] = 20
    else
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getzhai")
      _ENV["ai策略"] = decodeUrl(ret)
      _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{商家昵称}", _ENV["店铺名"])
      _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{视频简介}", _ENV["视频简介"])
      _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{认证信息}", _ENV["认证信息"])
      local aurl = "https://api.deepseek.com/chat/completions"
      local aAuthorization = "Bearer " .. AIkey
      local abody = {
        model = AImode,
        messages = {
          {
            role = "user",
            content = _ENV["ai策略"]
          }
        },
        stream = false
      }
      local adata = jsonLib.encode(abody)
      local aheader = "Authorization:" .. aAuthorization .. "\n" .. "Content-Type: application/json"
      local fh, code = httpPost(aurl, adata, 30, aheader)
      local jsret = jsonLib.decode(fh)
      if code == 200 then
        _ENV["待发送"] = jsret.choices[1].message.content
        print(_ENV["待发送"])
        _ENV["步骤"] = 20
      end
    end
  elseif _ENV["步骤"] == 20 then
    hudlog("发送")
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil then
      nodeLib.setText(?????????, _ENV["待发送"])
      sleep(_ENV["运行间隔"])
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil and ?????????.text ~= "输入你的问题.." and ?????????.text ~= "?发送消息" and ?????????.text ~= "?输入你的问题.." then
        local ?????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.ImageView",
          desc = "发送"
        }, false)
        if ?????? ~= nil then
          nodeLib.click(??????)
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 21
        end
      end
    else
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["待发送"])
        local ????????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.TextView",
          id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
        }, false)
        if ????????? ~= nil and ?????????.text ~= "输入咨询的内容..." then
          local ?????? = nodeLib.findOne(0, 0, 0, 0, {
            class = "android.widget.Button",
            text = "发送"
          }, false)
          if ?????? ~= nil then
            nodeLib.click(??????)
            sleep(_ENV["运行间隔"])
            _ENV["步骤"] = 21
          end
        end
      end
    end
  elseif _ENV["步骤"] == 21 then
    local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=is2")
    if ret == "OK" then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getzdy2")
      _ENV["自定义"] = decodeUrl(ret)
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.setText(?????????, _ENV["自定义"])
        sleep(_ENV["运行间隔"])
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["自定义"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    _ENV["步骤"] = 999
  elseif _ENV["步骤"] == 999 then
    local ??????????????? = nodeLib.findAll(0, 0, w, h, {
      desc = "关注按钮",
      text = "关注按钮"
    }, false)
    if ??????????????? ~= nil then
      _ENV["步骤"] = 13
      sleep(_ENV["运行间隔"])
    else
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        keyPress("back")
        sleep(_ENV["运行间隔"] * 2)
        hudlog("返回")
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        keyPress("back")
        sleep(_ENV["运行间隔"] * 2)
        hudlog("返回")
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {desc = "私信"}, false)
      if ?????? ~= nil then
        keyPress("back")
        sleep(_ENV["运行间隔"] * 2)
        hudlog("返回")
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.FrameLayout",
        desc = "关闭",
        id = "com.ss.android.ugc.aweme:id/cancel_btn"
      }, false)
      if ?????? ~= nil then
        hudlog("点击关闭")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"] * 2)
      end
    end
  elseif _ENV["步骤"] == -999 then
    local tm = systemTime()
    local ???????????? = tonumber(tm)
    if ???????????? > _ENV["停止时间"] then
      keyPress("home")
      exitScript()
    end
    for i = 1, _ENV["刷视频"] do
      sleep(60000)
      _ENV["滑动"](w * 0.5, h * 0.7, w * 0.5, h * 0.1, 500)
    end
    _ENV["步骤"] = 0
    _ENV["启动时间"] = os.time()
  end
end
function mainuid()
  nodeLib.updateNode()
  if _ENV["步骤"] == 0 then
    keyPress("home")
    sleep(_ENV["运行间隔"] * 3)
    keyPress("home")
    sleep(_ENV["运行间隔"] * 3)
    tap(_ENV["抖音坐标"][_ENV["抖音序号"]][1], _ENV["抖音坐标"][_ENV["抖音序号"]][2])
    sleep(5000)
    if _ENV["切换抖音"] == true then
      if _ENV["抖音序号"] == 1 then
        _ENV["跳转序号"] = 1
        _ENV["抖音序号"] = 2
      else
        _ENV["抖音序号"] = 1
        _ENV["跳转序号"] = 2
      end
    else
      _ENV["抖音序号"] = 1
      _ENV["跳转序号"] = 1
    end
    _ENV["步骤"] = 1
  elseif _ENV["步骤"] == 1 then
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme:id/v0a"
    }, false)
    if ????????? ~= nil then
      hudlog("退出聊天页")
      keyPress("back")
      sleep(_ENV["运行间隔"])
    else
      local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "com.bytedance.ies.dmt.ui.widget.DmtTextView",
        id = "com.ss.android.ugc.aweme:id/tv_title",
        text = "消息"
      }, false)
      if ???????????? ~= nil then
        hudlog("进入消息列表")
        _ENV["步骤"] = 2
      else
        local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.TextView",
          desc = "消息，按钮",
          text = "消息"
        }, false)
        if ???????????? ~= nil then
          hudlog("点击消息按钮")
          nodeLib.click(????????????)
          sleep(_ENV["运行间隔"])
        else
          hudlog("返回")
          keyPress("back")
          sleep(_ENV["运行间隔"] * 5)
        end
      end
    end
  elseif _ENV["步骤"] == 2 then
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme:id/red_tips_count_view"
    }, false)
    if ???????????? ~= nil then
      hudlog("点击关注红点")
      nodeLib.click(????????????)
      sleep(5000)
      hudlog("返回")
      keyPress("back")
      sleep(_ENV["运行间隔"])
    else
      _ENV["步骤"] = 3
    end
  elseif _ENV["步骤"] == 3 then
    nodeLib.updateNode()
    local ???????????? = nodeLib.findAll(0, 0, 0, 0, {
      class = "android.widget.Button",
      id = "com.ss.android.ugc.aweme:id/ycy"
    }, false)
    if ???????????? ~= nil then
      for i = 1, #???????????? do
        local ????????? = nodeLib.findInNode(????????????[i], {
          id = "com.ss.android.ugc.aweme:id/v-3"
        }, false, false)
        if ????????? ~= nil then
          local ??????????????? = nodeLib.findInNode(????????????[i], {
            id = "com.ss.android.ugc.aweme:id/tv_title"
          }, false, false)
          if ??????????????? ~= nil then
            _ENV["用户名"] = ???????????????.text
            nodeLib.click(???????????????)
            sleep(_ENV["运行间隔"])
            _ENV["步骤"] = 4
            _ENV["查找计数"] = 0
            _ENV["消息按钮计数"] = 0
            break
          end
        end
      end
      local ret = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme:id/0bl"
      }, false)
      if ret == nil and _ENV["步骤"] ~= 4 then
        if _ENV["首次回复"] == true then
          local ?????? = nodeLib.findOne(0, 0, 0, 0, {
            class = "android.widget.TextView",
            text = "首页"
          }, false)
          if ?????? ~= nil then
            hudlog("点击首页")
            nodeLib.click(??????)
            sleep(_ENV["运行间隔"])
            if _ENV["只回复"] == true then
              _ENV["步骤"] = 0
            else
              _ENV["步骤"] = 7
              _ENV["首次回复"] = false
            end
          end
        else
          local ?????? = nodeLib.findOne(0, 0, 0, 0, {
            class = "android.widget.TextView",
            text = "首页"
          }, false)
          if ?????? ~= nil then
            hudlog("点击首页")
            nodeLib.click(??????)
            sleep(_ENV["运行间隔"])
            _ENV["步骤"] = -999
            _ENV["首次回复"] = true
          end
        end
      elseif _ENV["步骤"] ~= 4 and _ENV["步骤"] ~= 7 and _ENV["步骤"] ~= 0 then
        local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.TextView",
          desc = "消息，按钮",
          text = "消息"
        }, false)
        if ???????????? ~= nil then
          hudlog("点击消息按钮")
          nodeLib.click(????????????)
          sleep(3000)
          _ENV["消息按钮计数"] = _ENV["消息按钮计数"] + 1
          if 5 < _ENV["消息按钮计数"] then
            _ENV["步骤"] = 7
            _ENV["首次回复"] = false
            _ENV["消息按钮计数"] = 0
          else
            _ENV["步骤"] = 1
          end
        end
      end
    end
  elseif _ENV["步骤"] == 4 then
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
    }, false)
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil or ????????? ~= nil then
      local ???V?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        id = "com.ss.android.ugc.aweme:id/szc"
      }, false)
      local ???V??????2 = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        id = "com.ss.android.ugc.aweme:id/wxw"
      }, false)
      if ???V?????? == nil and ???V??????2 == nil then
        hudlog("未发现蓝V标识")
        _ENV["步骤"] = 1
        return
      end
      hudlog("分析聊天记录")
      local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=glt&nicheng=" .. encodeUrl(_ENV["用户名"]))
      if ???????????? == "OK" then
        local ?????????????????? = nodeLib.findAll(0, 0, 0, 0, {
          class = "android.widget.TextView",
          id = "com.ss.android.ugc.aweme:id/content_layout"
        }, false)
        if ?????????????????? ~= nil then
          _ENV["聊天记录"] = ""
          for i = 1, #?????????????????? do
            print(??????????????????[i])
            _ENV["聊天记录"] = _ENV["聊天记录"] .. ??????????????????[i].text .. "\n"
          end
          if #_ENV["联系方式"] == 0 then
            hudlog("没有可发送的联系方式")
            while true do
              sleep(10000)
            end
          else
            _ENV["联系指针"] = 1
            if _ENV["分段"] == true and _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(1, 6)
            elseif _ENV["分段"] == true and _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(1, 4)
            elseif _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(3, 6)
            elseif _ENV["分段"] == true then
              _ENV["联系指针"] = rnd(1, 2)
            elseif _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(3, 4)
            elseif _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(5, 6)
            end
            _ENV["待回复联系方式"] = _ENV["联系方式"][_ENV["联系指针"]]
            local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gethuifuai")
            _ENV["ai策略"] = decodeUrl(ret)
            _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{聊天记录}", _ENV["聊天记录"])
            local aurl = "https://api.deepseek.com/chat/completions"
            local aAuthorization = "Bearer " .. AIkey
            local abody = {
              model = AImode,
              messages = {
                {
                  role = "user",
                  content = _ENV["ai策略"]
                }
              },
              stream = false
            }
            local adata = jsonLib.encode(abody)
            print(adata)
            local aheader = "Authorization:" .. aAuthorization .. "\n" .. "Content-Type: application/json"
            local fh, code = httpPost(aurl, adata, 30, aheader)
            local jsret = jsonLib.decode(fh)
            if code == 200 then
              _ENV["待发送"] = jsret.choices[1].message.content
              print(_ENV["待发送"])
              _ENV["步骤"] = 5
            end
          end
        else
          _ENV["查找计数"] = _ENV["查找计数"] + 1
          if 5 <= _ENV["查找计数"] then
            _ENV["联系指针"] = 1
            if _ENV["分段"] == true and _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(1, 6)
            elseif _ENV["分段"] == true and _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(1, 4)
            elseif _ENV["符号"] == true and _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(3, 6)
            elseif _ENV["分段"] == true then
              _ENV["联系指针"] = rnd(1, 2)
            elseif _ENV["符号"] == true then
              _ENV["联系指针"] = rnd(3, 4)
            elseif _ENV["单词"] == true then
              _ENV["联系指针"] = rnd(5, 6)
            end
            _ENV["待回复联系方式"] = _ENV["联系方式"][_ENV["联系指针"]]
            _ENV["待发送"] = "加我薇"
            _ENV["步骤"] = 5
          end
        end
      else
        hudlog("在数据库查询到该用户,返回")
        _ENV["步骤"] = 1
        keyPress("back")
        sleep(_ENV["运行间隔"])
      end
    end
  elseif _ENV["步骤"] == 5 then
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil then
      hudlog("输入回复话术")
      nodeLib.setText(?????????, _ENV["待发送"])
      sleep(_ENV["运行间隔"])
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
    }, false)
    if ????????? ~= nil then
      hudlog("输入回复话术")
      nodeLib.click(?????????)
      sleep(_ENV["运行间隔"])
      inputText(_ENV["待发送"])
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil and ?????????.text ~= "输入你的问题.." and ?????????.text ~= "?发送消息" and ?????????.text ~= "?输入你的问题.." then
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
    }, false)
    if ????????? ~= nil and ?????????.text ~= "输入咨询的内容..." then
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xierut&nicheng=" .. encodeUrl(_ENV["用户名"]))
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
        _ENV["步骤"] = 6
      end
    end
  elseif _ENV["步骤"] == 6 then
    local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=is3")
    if ret == "OK" then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getzdy3")
      _ENV["自定义"] = decodeUrl(ret)
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.setText(?????????, _ENV["自定义"])
        sleep(_ENV["运行间隔"])
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["自定义"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    _ENV["联系分割"] = splitStr(_ENV["待回复联系方式"], "|")
    if #_ENV["联系分割"] == 1 and (_ENV["联系指针"] == 3 or _ENV["联系指针"] == 4) then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getfuhao")
      _ENV["联系分割"][1] = decodeUrl(ret) .. "\r\n\r\n\r\n" .. _ENV["联系分割"][1]
    end
    for i = 1, #_ENV["联系分割"] do
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.setText(?????????, _ENV["联系分割"][i])
        sleep(_ENV["运行间隔"])
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["联系分割"][i])
        sleep(_ENV["运行间隔"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    sleep(_ENV["运行间隔"] * 5)
    keyPress("back")
    _ENV["步骤"] = 1
  elseif _ENV["步骤"] == 7 then
    hudlog("获取uid")
    local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getuid")
    uid = decodeUrl(ret)
    if uid ~= "" then
      hudlog("成功获取:" .. uid)
      _ENV["步骤"] = 8
    else
      sleep(_ENV["运行间隔"])
      hudlog("uid获取失败")
    end
  elseif _ENV["步骤"] == 8 then
    hudlog("跳转uid")
    local inter = {}
    inter.action = "android.intent.action.VIEW"
    inter.uri = "snssdk1128://user/detail/" .. uid
    runIntent(inter)
    sleep(_ENV["运行间隔"] * 2)
    if _ENV["跳转序号"] == 1 then
      local ret = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "抖音",
        id = "android:id/icon"
      }, false)
      if ret ~= nil then
        hudlog("点击抖音一号")
        nodeLib.click(ret)
        _ENV["步骤"] = 9
      end
    else
      local ret = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.ImageView",
        desc = "双开抖音",
        id = "android:id/icon"
      }, false)
      if ret ~= nil then
        hudlog("点击抖音二号")
        nodeLib.click(ret)
        _ENV["步骤"] = 9
      end
    end
  elseif _ENV["步骤"] == 9 then
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      text = "私密账号"
    }, true)
    if ???????????? ~= nil then
      hudlog("私密账号,返回")
      keyPress("back")
      sleep(_ENV["运行间隔"])
      _ENV["步骤"] = 999
    else
      local ??????????????? = nodeLib.findOne(0, 0, 0, 0, {
        id = "android:id/text1"
      }, false)
      local ??????????????? = nodeLib.findPreNode(0, 0, 0, 0, {
        class = "android.widget.TextView",
        package = "com.ss.android.ugc.aweme",
        text = "粉丝"
      }, false)
      local ??????????????? = nodeLib.findOne(0, 0, 0, 0, {
        desc = ".*复制名字和修改备注"
      }, true)
      local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
        desc = "抖音组织认证.*"
      }, true)
      if ??????????????? ~= nil and ??????????????? ~= nil and ??????????????? ~= nil then
        if ???????????? == nil then
          hudlog("未发现蓝V认证,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        _ENV["作品数"] = string.match(???????????????.text, "%d+")
        _ENV["店铺名"] = ???????????????.text
        _ENV["认证信息"] = ????????????.text
        local ????????????, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=gl&nicheng=" .. encodeUrl(_ENV["店铺名"]))
        if ???????????? ~= "OK" then
          hudlog("服务器重复验证失败,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        _ENV["粉丝"] = ???????????????.text
        _ENV["粉丝"] = string.match(_ENV["粉丝"], "%d*%.*%d+")
        _ENV["粉丝数"] = tonumber(_ENV["粉丝"])
        if string.find(???????????????.text, "万", 1, true) ~= nil then
          _ENV["粉丝数"] = _ENV["粉丝数"] * 10000
        end
        if 1 > _ENV["粉丝数"] or _ENV["粉丝数"] > 50000 then
          hudlog("粉丝数不符,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        if _ENV["作品数"] == nil then
          hudlog("无作品数,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        elseif 1 >= tonumber(_ENV["作品数"]) then
          hudlog("作品数为零,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        local ???????????? = false
        for n = 1, #_ENV["屏蔽词"] do
          if string.find(_ENV["店铺名"], _ENV["屏蔽词"][n], 1, true) ~= nil then
            ???????????? = true
            break
          end
          if string.find(_ENV["店铺名"], _ENV["屏蔽词"][n], 1, true) ~= nil then
            ???????????? = true
            break
          end
        end
        if ???????????? == true then
          hudlog("触发屏蔽,返回")
          keyPress("back")
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 999
          return
        end
        _ENV["步骤"] = 10
      end
    end
  elseif _ENV["步骤"] == 10 then
    local r = rnd(_ENV["停留1"], _ENV["停留2"])
    hudlog("主页停留" .. tostring(r) .. "秒")
    sleep(r * 1000)
    if _ENV["AI话术"] == true then
      _ENV["步骤"] = 11
    else
      _ENV["步骤"] = 13
    end
  elseif _ENV["步骤"] == 11 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.ImageView",
      id = "com.ss.android.ugc.aweme:id/cover"
    }, false)
    if ?????? ~= nil then
      hudlog("点击视频")
      nodeLib.click(??????)
      sleep(_ENV["运行间隔"])
      _ENV["步骤"] = 12
    else
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "android:id/text1"
      }, false)
      if ?????? ~= nil then
        hudlog("点击作品")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
  elseif _ENV["步骤"] == 12 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {desc = ".*收藏.*"}, true)
    if ?????? ~= nil then
      hudlog("获取视频简介")
      _ENV["视频简介"] = ""
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme:id/desc"
      }, false)
      if ?????? ~= nil then
        _ENV["视频简介"] = ??????.text
      end
      keyPress("back")
      sleep(_ENV["运行间隔"])
      _ENV["步骤"] = 13
    end
  elseif _ENV["步骤"] == 13 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {desc = "私信"}, false)
    if ?????? ~= nil then
      if ??????.text == "联系客服" then
        local ?????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.view.ViewGroup",
          desc = "更多"
        }, false)
        if ?????? ~= nil then
          hudlog("点击更多")
          nodeLib.click(??????)
          sleep(_ENV["运行间隔"])
        end
      else
        hudlog("点击私信")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      desc = "",
      text = "发私信"
    }, false)
    if ????????? ~= nil then
      hudlog("点击发私信")
      local ret = nodeLib.findParentNode(?????????, false)
      if ret ~= nil then
        nodeLib.click(ret)
        sleep(_ENV["运行间隔"])
      end
    end
    local ???????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.ImageView",
      desc = "更多面板"
    }, false)
    if ???????????? ~= nil then
      _ENV["步骤"] = 14
      httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xieru&nicheng=" .. encodeUrl(_ENV["店铺名"]))
    else
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        _ENV["步骤"] = 14
        httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xieru&nicheng=" .. encodeUrl(_ENV["店铺名"]))
      else
        local ?????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.ImageView",
          desc = "表情"
        }, false)
        if ?????? ~= nil then
          _ENV["步骤"] = 14
          httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=xieru&nicheng=" .. encodeUrl(_ENV["店铺名"]))
        end
      end
    end
  elseif _ENV["步骤"] == 14 then
    hudlog("获取话术")
    if _ENV["AI话术"] == false then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=huashu")
      _ENV["待发送"] = decodeUrl(ret)
      _ENV["步骤"] = 15
    else
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getzhai")
      _ENV["ai策略"] = decodeUrl(ret)
      _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{商家昵称}", _ENV["店铺名"])
      _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{视频简介}", _ENV["视频简介"])
      _ENV["ai策略"] = string.gsub(_ENV["ai策略"], "{认证信息}", _ENV["认证信息"])
      local aurl = "https://api.deepseek.com/chat/completions"
      local aAuthorization = "Bearer " .. AIkey
      local abody = {
        model = AImode,
        messages = {
          {
            role = "user",
            content = _ENV["ai策略"]
          }
        },
        stream = false
      }
      local adata = jsonLib.encode(abody)
      local aheader = "Authorization:" .. aAuthorization .. "\n" .. "Content-Type: application/json"
      local fh, code = httpPost(aurl, adata, 30, aheader)
      local jsret = jsonLib.decode(fh)
      if code == 200 then
        _ENV["待发送"] = jsret.choices[1].message.content
        print(_ENV["待发送"])
        _ENV["步骤"] = 15
      end
    end
  elseif _ENV["步骤"] == 15 then
    hudlog("发送")
    local ????????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.EditText"
    }, false)
    if ????????? ~= nil then
      nodeLib.setText(?????????, _ENV["待发送"])
      sleep(_ENV["运行间隔"])
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil and ?????????.text ~= "输入你的问题.." and ?????????.text ~= "?发送消息" and ?????????.text ~= "?输入你的问题.." then
        local ?????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.ImageView",
          desc = "发送"
        }, false)
        if ?????? ~= nil then
          nodeLib.click(??????)
          sleep(_ENV["运行间隔"])
          _ENV["步骤"] = 16
        end
      end
    else
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["待发送"])
        local ????????? = nodeLib.findOne(0, 0, 0, 0, {
          class = "android.widget.TextView",
          id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
        }, false)
        if ????????? ~= nil and ?????????.text ~= "输入咨询的内容..." then
          local ?????? = nodeLib.findOne(0, 0, 0, 0, {
            class = "android.widget.Button",
            text = "发送"
          }, false)
          if ?????? ~= nil then
            nodeLib.click(??????)
            sleep(_ENV["运行间隔"])
            _ENV["步骤"] = 16
          end
        end
      end
    end
  elseif _ENV["步骤"] == 16 then
    local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=is2")
    if ret == "OK" then
      local ret, code = httpGet("http://" .. _ENV["服务器ip"] .. "/cx?type=getzdy2")
      _ENV["自定义"] = decodeUrl(ret)
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.setText(?????????, _ENV["自定义"])
        sleep(_ENV["运行间隔"])
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        hudlog("输入回复话术")
        nodeLib.click(?????????)
        sleep(_ENV["运行间隔"])
        inputText(_ENV["自定义"])
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.Button",
        text = "发送"
      }, false)
      if ?????? ~= nil then
        hudlog("点击发送")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"])
      end
    end
    _ENV["步骤"] = 999
  elseif _ENV["步骤"] == 999 then
    local ?????? = nodeLib.findOne(0, 0, 0, 0, {
      class = "android.widget.TextView",
      text = "首页"
    }, false)
    if ?????? ~= nil then
      _ENV["步骤"] = 7
      _ENV["跳转次数"] = _ENV["跳转次数"] + 1
      if _ENV["跳转次数"] >= _ENV["私信控制"] then
        _ENV["步骤"] = 1
        _ENV["跳转次数"] = 0
      end
    else
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.EditText"
      }, false)
      if ????????? ~= nil then
        keyPress("back")
        sleep(_ENV["运行间隔"] * 2)
        hudlog("返回")
      end
      local ????????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.TextView",
        id = "com.ss.android.ugc.aweme.ecom_cs_im_plugin:id/fv"
      }, false)
      if ????????? ~= nil then
        keyPress("back")
        sleep(_ENV["运行间隔"] * 2)
        hudlog("返回")
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {desc = "私信"}, false)
      if ?????? ~= nil then
        keyPress("back")
        sleep(_ENV["运行间隔"] * 2)
        hudlog("返回")
      end
      local ?????? = nodeLib.findOne(0, 0, 0, 0, {
        class = "android.widget.FrameLayout",
        desc = "关闭",
        id = "com.ss.android.ugc.aweme:id/cancel_btn"
      }, false)
      if ?????? ~= nil then
        hudlog("点击关闭")
        nodeLib.click(??????)
        sleep(_ENV["运行间隔"] * 2)
      end
    end
  elseif _ENV["步骤"] == -999 then
    local tm = systemTime()
    local ???????????? = tonumber(tm)
    if ???????????? > _ENV["停止时间"] then
      keyPress("home")
      exitScript()
    end
    for i = 1, _ENV["刷视频"] do
      sleep(60000)
      _ENV["滑动"](w * 0.5, h * 0.7, w * 0.5, h * 0.1, 500)
    end
    _ENV["步骤"] = 0
    _ENV["启动时间"] = os.time()
  end
end
if action == 1 then
  if arr["单选框_模式"] == "0" then
    while true do
      print(_ENV["步骤"])
      local ok, res = xpcall(main, myerrorhandler)
      sleep(_ENV["运行间隔"])
      if _ENV["历史步骤"] ~= _ENV["步骤"] then
        _ENV["异常时间"] = os.time()
        print(os.time() - _ENV["异常时间"])
        _ENV["历史步骤"] = _ENV["步骤"]
      elseif os.time() - _ENV["异常时间"] > 300 then
        print("超时")
        _ENV["步骤"] = 0
        _ENV["异常时间"] = os.time()
      end
    end
  else
    while true do
      print(_ENV["步骤"])
      local ok, res = xpcall(mainuid, myerrorhandler)
      sleep(_ENV["运行间隔"])
      if _ENV["历史步骤"] ~= _ENV["步骤"] then
        _ENV["异常时间"] = os.time()
        print(os.time() - _ENV["异常时间"])
        _ENV["历史步骤"] = _ENV["步骤"]
      elseif os.time() - _ENV["异常时间"] > 300 then
        print("超时")
        _ENV["步骤"] = 0
        _ENV["异常时间"] = os.time()
      end
    end
    else
      toast("再见!", 0, 0, 12)
      sleep(3000)
      exitScript()
    end
  end
