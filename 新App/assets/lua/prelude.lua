-- 自研宿主 prelude: 把引擎 API 映射到 Java 宿主
-- Lua 5.3, 所有函数与宿主通过 __host.call(method, jsonArgs) 交互

local json = {}

local esc_map = {
  ['"'] = '\\"', ['\\'] = '\\\\', ['\b'] = '\\b', ['\f'] = '\\f',
  ['\n'] = '\\n', ['\r'] = '\\r', ['\t'] = '\\t'
}

local function esc(s)
  s = s:gsub('[%z\1-\31\\"]', function(c)
    return esc_map[c] or string.format('\\u%04x', string.byte(c))
  end)
  return '"' .. s .. '"'
end

local function is_array(t)
  local n = 0
  for k in pairs(t) do
    if type(k) ~= 'number' then return false end
    n = n + 1
  end
  return n == #t
end

function json.encode(v)
  local tp = type(v)
  if v == nil then return 'null' end
  if tp == 'number' then
    if v ~= v or v == math.huge or v == -math.huge then return 'null' end
    if math.type and math.type(v) == 'integer' then return tostring(v) end
    return string.format('%.14g', v)
  end
  if tp == 'boolean' then return v and 'true' or 'false' end
  if tp == 'string' then return esc(v) end
  if tp == 'table' then
    local out = {}
    if is_array(v) then
      for i = 1, #v do out[i] = json.encode(v[i]) end
      return '[' .. table.concat(out, ',') .. ']'
    end
    for k, x in pairs(v) do
      local kt = type(k)
      if kt == 'number' or kt == 'string' then
        out[#out + 1] = esc(tostring(k)) .. ':' .. json.encode(x)
      end
    end
    return '{' .. table.concat(out, ',') .. '}'
  end
  return 'null'
end

local function utf8_char(cp)
  if cp < 0x80 then return string.char(cp) end
  if cp < 0x800 then
    return string.char(0xC0 + math.floor(cp / 0x40), 0x80 + cp % 0x40)
  end
  return string.char(0xE0 + math.floor(cp / 0x1000), 0x80 + math.floor(cp / 0x40) % 0x40, 0x80 + cp % 0x40)
end

local function skip(s, i)
  while i <= #s and s:find('^[ \t\r\n]', i) do i = i + 1 end
  return i
end

local parse_value

local function parse_string(s, i)
  local out = {}
  i = i + 1
  while true do
    local c = s:sub(i, i)
    if c == '' then error('bad string') end
    if c == '"' then return table.concat(out), i + 1 end
    if c == '\\' then
      local e = s:sub(i + 1, i + 1)
      if e == 'u' then
        local hex = s:sub(i + 2, i + 5)
        out[#out + 1] = utf8_char(tonumber(hex, 16) or 63)
        i = i + 6
      elseif e == 'n' then out[#out + 1] = '\n'; i = i + 2
      elseif e == 'r' then out[#out + 1] = '\r'; i = i + 2
      elseif e == 't' then out[#out + 1] = '\t'; i = i + 2
      elseif e == 'b' then out[#out + 1] = '\b'; i = i + 2
      elseif e == 'f' then out[#out + 1] = '\f'; i = i + 2
      else out[#out + 1] = e; i = i + 2 end
    else
      out[#out + 1] = c
      i = i + 1
    end
  end
end

parse_value = function(s, i)
  i = skip(s, i)
  local c = s:sub(i, i)
  if c == '{' then
    local o = {}
    i = skip(s, i + 1)
    if s:sub(i, i) == '}' then return o, i + 1 end
    while true do
      local k
      i = skip(s, i)
      if s:sub(i, i) == '"' then k, i = parse_string(s, i) else error('bad key') end
      i = skip(s, i)
      if s:sub(i, i) ~= ':' then error('bad colon') end
      local v
      v, i = parse_value(s, i + 1)
      o[k] = v
      i = skip(s, i)
      local d = s:sub(i, i)
      if d == ',' then i = i + 1
      elseif d == '}' then return o, i + 1
      else error('bad object') end
    end
  elseif c == '[' then
    local a = {}
    i = skip(s, i + 1)
    if s:sub(i, i) == ']' then return a, i + 1 end
    while true do
      local v
      v, i = parse_value(s, i)
      a[#a + 1] = v
      i = skip(s, i)
      local d = s:sub(i, i)
      if d == ',' then i = i + 1
      elseif d == ']' then return a, i + 1
      else error('bad array') end
    end
  elseif c == '"' then
    return parse_string(s, i)
  elseif c == 't' then return true, i + 4
  elseif c == 'f' then return false, i + 5
  elseif c == 'n' then return nil, i + 4
  else
    local num = s:match('^%-?%d+%.?%d*[eE]?[%+%-]?%d*', i)
    if num == nil or num == '' then error('bad value') end
    return tonumber(num), i + #num
  end
end

function json.decode(s)
  if type(s) ~= 'string' or s == '' then return nil end
  local ok, res = pcall(parse_value, s, 1)
  if ok then return res end
  return nil
end

local function host(method, ...)
  local n = select('#', ...)
  local parts = {}
  for i = 1, n do parts[i] = json.encode((select(i, ...))) end
  local res = __host.call(method, '[' .. table.concat(parts, ',') .. ']')
  if res == nil or res == '' or res == 'null' then return nil end
  local ok, v = pcall(json.decode, res)
  if ok and v ~= nil then return v end
  return res  -- 不是合法 JSON(如路径/普通字符串)就原样返回
end

function sleep(ms) host('sleep', tonumber(ms) or 0) end
function toast(s) host('toast', tostring(s)) end
function hudlog(s) host('hud', tostring(s)) end
function print(...)
  local t = {}
  for i = 1, select('#', ...) do t[i] = tostring((select(i, ...))) end
  host('log', table.concat(t, '\t'))
end
function tickCount() return tonumber(host('tickCount')) or 0 end
function rnd(a, b) return math.random(a, b) end
function getDisplaySize()
  local r = host('getDisplaySize')
  if type(r) == 'table' then return r[1], r[2] end
  return 0, 0
end
function keyPress(k) host('keyPress', tostring(k)) end
function tap(x, y) host('tap', x, y) end
function swipe(x1, y1, x2, y2, t) host('swipe', x1, y1, x2, y2, t) end
function inputText(s) host('inputText', tostring(s)) end
function runIntent(t)
  if type(t) == 'table' then host('runIntent', t.action or '', t.uri or '') end
end
function launchApp(p) host('launchApp', tostring(p)) end
function httpGet(url)
  local r = host('httpGet', url)
  if type(r) == 'table' then return r[1], r[2] end
  return '', 0
end
function httpPost(url, body, timeout, header)
  local r = host('httpPost', url, body or '', timeout or 20, header or '')
  if type(r) == 'table' then return r[1], r[2] end
  return '', 0
end
function MD5(s) return tostring(host('md5', tostring(s)) or '') end
function encodeUrl(s) return tostring(host('encodeUrl', tostring(s)) or '') end
function decodeUrl(s) return tostring(host('decodeUrl', tostring(s)) or '') end
function fileMD5(p) return tostring(host('fileMD5', tostring(p)) or '') end
function getWorkPath() return tostring(host('getWorkPath') or '') end
function aiAsk(prompt) return tostring(host('aiAsk', tostring(prompt)) or '') end
function fileExist(p) return host('fileExist', tostring(p)) == true end
function mkdir(p) host('mkdir', tostring(p)) end
function extractAssets(n, d, p) return host('extractAssets', tostring(n), tostring(d)) == true end

function splitStr(s, sep)
  local t = {}
  if type(s) ~= 'string' then return t end
  if sep == nil or sep == '' then t[1] = s return t end
  local start = 1
  while true do
    local i = string.find(s, sep, start, true)
    if i == nil then
      t[#t + 1] = string.sub(s, start)
      break
    end
    t[#t + 1] = string.sub(s, start, i - 1)
    start = i + #sep
  end
  return t
end

sh = {}
function sh.Read(p)
  local v = host('shRead', tostring(p))
  if v == nil then return nil end
  return v
end
function sh.Write(p, c) host('shWrite', tostring(p), tostring(c)) return true end

jsonLib = { encode = json.encode, decode = json.decode }
_G.json = json

nodeLib = {}
function nodeLib.updateNode() host('nodeUpdate') end
function nodeLib.findOne(l, t, r, b, sel, click)
  local v = host('nodeFindOne', l or 0, t or 0, r or 0, b or 0, sel or {}, click and true or false)
  if type(v) == 'table' then return v end
  return nil
end
function nodeLib.findAll(l, t, r, b, sel, flag)
  local v = host('nodeFindAll', l or 0, t or 0, r or 0, b or 0, sel or {}, flag and true or false)
  local out = {}
  if type(v) == 'table' then
    for i = 1, #v do out[i] = v[i] end
  end
  return out
end
function nodeLib.click(n) if type(n) == 'table' then return host('nodeClick', n.nid or 0) end return false end
function nodeLib.setText(n, s) if type(n) == 'table' then return host('nodeSetText', n.nid or 0, tostring(s)) end return false end
function nodeLib.findParentNode(n) if type(n) == 'table' then local v = host('nodeParent', n.nid or 0) if type(v) == 'table' then return v end end return nil end
function nodeLib.findPreNode(n) if type(n) == 'table' then local v = host('nodePreNode', n.nid or 0) if type(v) == 'table' then return v end end return nil end
function nodeLib.findInNode(n, sel) if type(n) == 'table' then local v = host('nodeFindIn', n.nid or 0, sel or {}) if type(v) == 'table' then return v end end return nil end

local hudid = 1
function createHUD() return 1 end
function showHUD(id, txt, ...) host('hud', tostring(txt)) end
function hideHUD(id) end

console = {
  show = function() end,
  hide = function() end,
  clearLog = function() end,
  setPos = function() end,
  println = function(a, b) print(a, b) end,
  log = function(a, b) print(a, b) end
}

local Gesture = {}
Gesture.__index = Gesture
function Gesture:new() return setmetatable({ paths = {} }, Gesture) end
function Gesture:addPath(p) table.insert(self.paths, p) end
function Gesture:dispatch(cb)
  local p = self.paths[1]
  if p and p.pts and #p.pts >= 2 then
    local a, b = p.pts[1], p.pts[#p.pts]
    swipe(a[1], a[2], b[1], b[2], p.dur or 300)
  end
  if cb then cb(1) end
  return true
end

local Path = {}
Path.__index = Path
function Path:new() return setmetatable({ pts = {}, dur = 300, st = 100 }, Path) end
function Path:setStartTime(t) self.st = t end
function Path:setDurTime(t) self.dur = t end
function Path:addPoint(x, y) table.insert(self.pts, { x, y }) end

_G.Gesture = Gesture
_G.Path = Path

function beginThread(f, ...) end
function setStopCallBack(f) _G.__stopcb = f end
function exitScript()
  host('stop')
  error('__stop__')
end
function import(...) return nil end
function ocrEx(w, t, l, b, ...)
  local r = host('ocrEx', w, t, l, b)
  if type(r) == 'table' then return r end
  return {}
end
function pdocr(x1, y1, x2, y2)
  local r = host('pdocr', x1, y1, x2, y2)
  if type(r) == 'table' then return r end
  return {}
end
function findMultiColor(l, t, r, b, c1, offs, dir, sim)
  local v = host('findMultiColor', l or 0, t or 0, r or 0, b or 0,
                 tostring(c1 or ''), tostring(offs or ''), dir or 0, sim or 0.9)
  if type(v) == 'table' then return v[1] or -1, v[2] or -1 end
  return -1, -1
end

do
  local wp = getWorkPath()
  if wp ~= "" then
    package.path = wp .. "/lua/?.lua;" .. package.path
  end
  local cfg = host('getConfig')
  if type(cfg) == 'table' then
    _ENV["本机配置"] = cfg
    if cfg["AI话术"] ~= nil then _ENV["本地AI话术"] = cfg["AI话术"] end
  else
    _ENV["本机配置"] = {}
  end
end
