import("com.nx.assist.lua.LuaEngine")
print("\230\143\146\228\187\182\228\189\156\232\128\133: \230\149\153\228\184\187\t\tQQ: 1619433933")
print("\229\159\185\232\174\173\230\149\153\229\173\166: \232\156\130\231\190\164\t\tQQ: 1771955063\t\tQQ\231\190\164: 562451816")
print("\230\149\153\229\173\166\228\187\139\231\187\141: https://www.yuque.com/fengqunketang/wsmiu0/wh3d8p0ag55405ge")
local PaddLeOCRPLuGin = {}
local PluGinObject
function PaddLeOCRPLuGin.IniTiaLModeL(Object)
  if type(Object) ~= "userdata" or Object == nil then
    return "\229\143\152\233\135\143\231\177\187\229\158\139\233\148\153\232\175\175"
  end
  PluGinObject = Object.loadClass("com.LR.JZ.PaddLeOCR.Main")
  if PluGinObject == nil then
    return "\229\138\160\232\189\189Java\231\177\187\229\164\177\232\180\165"
  end
  return PluGinObject.IniTiaLModeL(LuaEngine.getContext()) == false and "\229\136\157\229\167\139\229\140\150\230\143\146\228\187\182\229\164\177\232\180\165, \232\175\183\231\161\174\228\191\157\229\189\147\229\137\141\232\174\190\229\164\135\231\179\187\231\187\159\231\137\136\230\156\172\228\184\141\228\189\142\228\186\1427.0" or nil
end
function PaddLeOCRPLuGin.GetCPUCoreNumBer()
  if PluGinObject == nil then
    return "\232\175\183\229\133\136\229\136\157\229\167\139\229\140\150\230\143\146\228\187\182"
  end
  return PluGinObject.ObtainCoreNumber()
end
function PaddLeOCRPLuGin.IFGPUIsItAvaiLabLe()
  if PluGinObject == nil then
    return "\232\175\183\229\133\136\229\136\157\229\167\139\229\140\150\230\143\146\228\187\182"
  end
  return PluGinObject.IFGPUIsItAvaiLabLe()
end
function PaddLeOCRPLuGin.LoadModeL(LaBeL, DbModeL, RecModeL, Input, Core, GPU, DbPassWord, RecPassWord)
  if PluGinObject == nil then
    return "\232\175\183\229\133\136\229\136\157\229\167\139\229\140\150\230\143\146\228\187\182"
  end
  if type(LaBeL) ~= "string" or LaBeL == "" then
    return "\230\160\135\231\173\190\229\143\130\230\149\176\233\148\153\232\175\175"
  end
  if type(DbModeL) ~= "table" or #DbModeL ~= 2 or type(DbModeL[1]) ~= "string" or type(DbModeL[2]) ~= "string" then
    return "\230\150\135\230\156\172\230\163\128\230\181\139\230\168\161\229\158\139\229\143\130\230\149\176\233\148\153\232\175\175"
  end
  if type(RecModeL) ~= "table" or #RecModeL ~= 2 or type(RecModeL[1]) ~= "string" or type(RecModeL[2]) ~= "string" then
    return "\230\150\135\230\156\172\232\175\134\229\136\171\230\168\161\229\158\139\229\143\130\230\149\176\233\148\153\232\175\175"
  end
  if type(Input) ~= "number" or not (Input > 0) then
    return "\232\190\147\229\133\165\229\164\167\229\176\143\229\143\130\230\149\176\233\148\153\232\175\175 \229\185\182\228\184\148\233\156\128\232\166\129\229\164\167\228\186\1420"
  end
  if type(Core) ~= "number" or not (Core > 0) then
    return "\230\160\184\229\191\131\230\149\176\229\143\130\230\149\176\233\148\153\232\175\175 \229\185\182\228\184\148\233\156\128\232\166\129\229\164\167\228\186\1420"
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
    return "\232\175\183\229\133\136\229\136\157\229\167\139\229\140\150\230\143\146\228\187\182"
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
