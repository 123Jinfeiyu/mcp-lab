_ENV["本地数据目录"] = {
  "/sdcard/AI私信助手数据/",
  "/sdcard/MT2/mcp/AI本地数据/"
}

local DIRS = _ENV["本地数据目录"]

local function read_text(name)
  for i = 1, #DIRS do
    local ok, v = pcall(function()
      return sh.Read(DIRS[i] .. name .. ".txt")
    end)
    if ok and v ~= nil and v ~= "" then
      return v
    end
  end
  return nil
end

local function trim(v)
  if v == nil then
    return nil
  end
  return (v:gsub("^[%s\r\n]+", ""):gsub("[%s\r\n]+$", ""))
end

local function text(name, def)
  local v = read_text(name)
  if v == nil then
    return def
  end
  v = trim(v)
  if v == "" then
    return def
  end
  return v
end

local function list(name)
  local v = read_text(name)
  local t = {}
  if v ~= nil then
    for line in v:gmatch("[^\r\n]+") do
      line = trim(line)
      if line ~= "" then
        table.insert(t, line)
      end
    end
  end
  return t
end

local function encode(s)
  local e = _ENV["encodeUrl"]
  if e ~= nil then
    local ok, r = pcall(e, s)
    if ok and type(r) == "string" then
      return r
    end
  end
  return s
end

local function workdir()
  local ok, p = pcall(function()
    return getWorkPath()
  end)
  if ok and type(p) == "string" and p ~= "" then
    return p
  end
  return "/sdcard/"
end

local DB = {
  users = {},
  shops = {}
}

local function dbpath(kind)
  if kind == "users" then
    return workdir() .. "/dy_seen_users.txt"
  end
  return workdir() .. "/dy_seen_shops.txt"
end

local function dbload(kind)
  local ok, v = pcall(function()
    return sh.Read(dbpath(kind))
  end)
  if ok and v ~= nil then
    for line in v:gmatch("[^\r\n]+") do
      if line ~= "" then
        DB[kind][line] = true
      end
    end
  end
end

dbload("users")
dbload("shops")

local function dbsave(kind)
  local t = {}
  for k in pairs(DB[kind]) do
    table.insert(t, k)
  end
  pcall(function()
    sh.Write(dbpath(kind), table.concat(t, "\n"))
  end)
end

local function dbhas(kind, name)
  if name == nil or name == "" then
    return false
  end
  return DB[kind][name] == true
end

local function dbadd(kind, name)
  if name == nil or name == "" then
    return
  end
  if DB[kind][name] ~= true then
    DB[kind][name] = true
    dbsave(kind)
  end
end

local function qval(url, key)
  return string.match(url, "[?&]" .. key .. "=([^&]*)")
end

local function dq(s)
  if s == nil then
    return ""
  end
  local d = _ENV["decodeUrl"]
  if d ~= nil then
    local ok, r = pcall(d, s)
    if ok and type(r) == "string" then
      return r
    end
  end
  return s
end

local DEF_AI_D = "你是抖音私信助手。商家昵称:{商家昵称} 视频简介:{视频简介} 认证信息:{认证信息}。请写一条30字以内的自然打招呼私信,只输出话术本身。"
local DEF_AI_B = DEF_AI_D
local DEF_HF_D = "根据下面这段聊天记录,写一条自然、简短的回复,只输出内容本身:\n{聊天记录}"
local DEF_HF_B = DEF_HF_D

local function handle(url)
  local t = string.match(url, "[?&]type=([%w_]+)") or ""
  if t == "pingbi" then
    return text("屏蔽词", ""), 200
  end
  if t == "huifupingbi" then
    return text("回复屏蔽词", ""), 200
  end
  if t == "shijian" then
    return "0-0", 200
  end
  if t == "getpage" then
    return "OK", 200
  end
  if t == "huashu" then
    return encode(text("话术", "你好")), 200
  end
  if t == "get" then
    return encode(text("关键词", "美食")), 200
  end
  if t == "getuid" then
    return encode(text("UID", "")), 200
  end
  if t == "getfuhao" then
    return encode(text("符号", "")), 200
  end
  if t == "getzdy2" then
    return encode(text("自定义2", "")), 200
  end
  if t == "getzdy3" then
    return encode(text("自定义3", "")), 200
  end
  if t == "is2" then
    return text("开关2", ""), 200
  end
  if t == "is3" then
    return text("开关3", ""), 200
  end
  if t == "getmode" then
    return text("模式", "0"), 200
  end
  if t == "getmode2" then
    return text("私信模式", "0"), 200
  end
  if t == "getzhai" then
    return encode(text("AI策略D", DEF_AI_D)), 200
  end
  if t == "getzhai2" then
    return encode(text("AI策略B", DEF_AI_B)), 200
  end
  if t == "gethuifuai" then
    return encode(text("私信AI策略D", DEF_HF_D)), 200
  end
  if t == "gethuifuai2" then
    return encode(text("私信AI策略B", DEF_HF_B)), 200
  end
  if string.sub(t, 1, 6) == "xierut" then
    dbadd("users", dq(qval(url, "nicheng")))
    return "OK", 200
  end
  if string.sub(t, 1, 5) == "xieru" then
    dbadd("shops", dq(qval(url, "nicheng")))
    return "OK", 200
  end
  if string.sub(t, 1, 3) == "glt" then
    if dbhas("users", dq(qval(url, "nicheng"))) then
      return "NO", 200
    end
    return "OK", 200
  end
  if string.sub(t, 1, 2) == "gl" then
    if dbhas("shops", dq(qval(url, "nicheng"))) then
      return "NO", 200
    end
    return "OK", 200
  end
  return "OK", 200
end

local base = _ENV["httpGet"]

_ENV["本地读取列表"] = list
_ENV["本地读取文本"] = text

local cfg = read_text("配置")
if cfg ~= nil then
  for line in cfg:gmatch("[^\r\n]+") do
    local k, v = string.match(line, "^%s*([^=%s]+)%s*=%s*(.-)%s*$")
    if k ~= nil and v ~= nil and v ~= "" then
      _ENV["本地" .. k] = v
    end
  end
end

_ENV["httpGet"] = function(url, ...)
  if type(url) == "string" and string.find(url, "/cx?", 1, true) ~= nil then
    return handle(url)
  end
  if base ~= nil then
    return base(url, ...)
  end
  return "", 0
end
