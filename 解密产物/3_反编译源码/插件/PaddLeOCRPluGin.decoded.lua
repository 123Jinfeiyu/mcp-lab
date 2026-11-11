import("com.nx.assist.lua.LuaEngine")
print("插件作者: 教主\t\tQQ: 1619433933")
print("培训教学: 蜂群\t\tQQ: 1771955063\t\tQQ群: 562451816")
print("教学介绍: https://www.yuque.com/fengqunketang/wsmiu0/wh3d8p0ag55405ge")
local PaddLeOCRPLuGin = {}
local PluGinObject
function PaddLeOCRPLuGin.IniTiaLModeL(Object)
  if type(Object) ~= "userdata" or Object == nil then
    return "变量类型错误"
  end
  PluGinObject = Object.loadClass("com.LR.JZ.PaddLeOCR.Main")
  if PluGinObject == nil then
    return "加载Java类失败"
  end
  return PluGinObject.IniTiaLModeL(LuaEngine.getContext()) == false and "初始化插件失败, 请确保当前设备系统版本不低于7.0" or nil
end
function PaddLeOCRPLuGin.GetCPUCoreNumBer()
  if PluGinObject == nil then
    return "请先初始化插件"
  end
  return PluGinObject.ObtainCoreNumber()
end
function PaddLeOCRPLuGin.IFGPUIsItAvaiLabLe()
  if PluGinObject == nil then
    return "请先初始化插件"
  end
  return PluGinObject.IFGPUIsItAvaiLabLe()
end
function PaddLeOCRPLuGin.LoadModeL(LaBeL, DbModeL, RecModeL, Input, Core, GPU, DbPassWord, RecPassWord)
  if PluGinObject == nil then
    return "请先初始化插件"
  end
  if type(LaBeL) ~= "string" or LaBeL == "" then
    return "标签参数错误"
  end
  if type(DbModeL) ~= "table" or #DbModeL ~= 2 or type(DbModeL[1]) ~= "string" or type(DbModeL[2]) ~= "string" then
    return "文本检测模型参数错误"
  end
  if type(RecModeL) ~= "table" or #RecModeL ~= 2 or type(RecModeL[1]) ~= "string" or type(RecModeL[2]) ~= "string" then
    return "文本识别模型参数错误"
  end
  if type(Input) ~= "number" or not (Input > 0) then
    return "输入大小参数错误 并且需要大于0"
  end
  if type(Core) ~= "number" or not (Core > 0) then
    return "核心数参数错误 并且需要大于0"
  end
  local ParameterGPU = false
  local ParameterDbPassWord = ""
  local ParameterRecPassWord = ""
  if type(GPU) == "boolean" then
    ParameterGPU = GPU
  end
  if type(DbPassWord) == "string" then
    ParameterDbPassWord = DbPassWord
  end
  if type(RecPassWord) == "string" then
    ParameterRecPassWord = RecPassWord
  end
  local VaLue = PluGinObject.LoadModeL(LaBeL, DbModeL[1], DbModeL[2], RecModeL[1], RecModeL[2], Input, Core, ParameterGPU, ParameterDbPassWord, ParameterRecPassWord)
  if VaLue == nil then
  end
  return VaLue
end
function PaddLeOCRPLuGin.Detect(BitMap, Confidence, ProbabiLity)
  if PluGinObject == nil then
    return "请先初始化插件"
  end
  if type(BitMap) ~= "userdata" then
    return nil
  end
  if math.type(Confidence) ~= "float" or Confidence < 0 then
    return nil
  end
  if math.type(ProbabiLity) ~= "float" or ProbabiLity < 0 then
    return nil
  end
  local Data = PluGinObject.Detect(BitMap, Confidence, ProbabiLity)
  if Data == "[]" then
    return {}
  end
  return jsonLib.decode(Data)
end
return PaddLeOCRPLuGin
