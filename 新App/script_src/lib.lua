local hudid
function hudinit()
  hudid = createHUD()
  showHUD(id, "HelloWorld!", 12, "0xffff0000", "0xffffffff", 0, 0, 0, 0, 0)
end
hudinit()
function myerrorhandler(err)
  print("ERROR:", err)
end
function hudlog(txt)
  showHUD(hudid, tostring(txt), 12, "0xffff0000", "0xffffffff", 0, 0, 0, 0, 0)
end
_ENV["\230\156\172\229\156\176\232\191\135\230\187\164"] = function(txt)
  if string.find(_ENV["\232\191\135\230\187\164\230\157\191"], txt, 1, true) ~= nil then
    return false
  else
    _ENV["\232\191\135\230\187\164\230\157\191"] = _ENV["\232\191\135\230\187\164\230\157\191"] .. txt .. " "
    return true
  end
end
_ENV["\230\187\145\229\138\168"] = function(x1, y1, x2, y2, t)
  local gesture = Gesture:new()
  local path = Path:new()
  path:setStartTime(100)
  path:setDurTime(t)
  path:addPoint(x1, y1)
  path:addPoint(x2, y2)
  gesture:addPath(path)
  function onFinishEvent(type)
    print("event:" .. type)
  end
  local r = gesture:dispatch(onFinishEvent)
  local d = t + 200
  sleep(d)
  if r then
    print("\230\137\167\232\161\140\230\136\144\229\138\159")
  else
    print("\230\137\167\232\161\140\229\164\177\232\180\165")
  end
end
_ENV["\232\177\134\229\140\133"] = function(v1)
  local aurl = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
  local aAuthorization = "Bearer " .. AIkey
  local abody = {
    model = AImode,
    reasoning_effort = "medium",
    messages = {
      {
        role = "user",
        content = {
          {type = "text", text = v1}
        }
      }
    }
  }
  local adata = jsonLib.encode(abody)
  print(adata)
  local aheader = "Authorization:" .. aAuthorization .. "\n" .. "Content-Type: application/json"
  local fh, code = httpPost(aurl, adata, 30, aheader)
  local jsret = jsonLib.decode(fh)
  print(fh)
  if code == 200 then
    return jsret.choices[1].message.content
  end
end
function deepseek(v2)
  local aurl = "https://api.deepseek.com/chat/completions"
  local aAuthorization = "Bearer " .. DAIkey
  local abody = {
    model = DAImode,
    messages = {
      {role = "user", content = v2}
    },
    stream = false
  }
  local adata = jsonLib.encode(abody)
  local aheader = "Authorization:" .. aAuthorization .. "\n" .. "Content-Type: application/json"
  local fh, code = httpPost(aurl, adata, 30, aheader)
  print(fh)
  local jsret = jsonLib.decode(fh)
  if code == 200 then
    return jsret.choices[1].message.content
  end
end
