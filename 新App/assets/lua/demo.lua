toast("Lua宿主已启动")
hudlog("测试:1秒后检查无障碍")
sleep(1000)
local w, h = getDisplaySize()
hudlog("屏幕:" .. w .. "x" .. h)
nodeLib.updateNode()
local n = nodeLib.findOne(0, 0, 0, 0, { text = "设置" }, false)
if n ~= nil then
  hudlog("找到[设置] y=" .. tostring(n.bounds.t))
else
  hudlog("未找到[设置](无障碍未开则正常)")
end
sleep(1500)
hudlog("测试完成")
