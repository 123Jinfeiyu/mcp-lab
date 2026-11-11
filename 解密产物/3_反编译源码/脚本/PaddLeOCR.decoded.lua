import("com.nx.assist.lua.LuaEngine")
PaddLeOCR = require("PaddLeOCRPluGin")
path = "/mnt/sdcard/PaddLeOCR"
if not fileExist(path) then
  mkdir(path)
end
extractAssets("PaddLeOCR.rc", "/mnt/sdcard/PaddLeOCR", "*.*")
Loader = LuaEngine.loadApk("/sdcard/PaddLeOCR/PaddLeOCR.apk")
print(Loader)
Err = PaddLeOCR.IniTiaLModeL(Loader)
if Err ~= nil then
  print(string.format("初始化OCR插件失败 原因: %s", Err))
end
DbModeL = {
  "/sdcard/PaddLeOCR/PaddLeOCR_Db.param",
  "/sdcard/PaddLeOCR/PaddLeOCR_Db.bin"
}
RecModeL = {
  "/sdcard/PaddLeOCR/PaddLeOCR_Rec.param",
  "/sdcard/PaddLeOCR/PaddLeOCR_Rec.bin"
}
LaBeL = "/sdcard/PaddLeOCR/Label.txt"
Err = PaddLeOCR.LoadModeL(LaBeL, DbModeL, RecModeL, 640, 2, false, nil, nil)
if Err ~= nil then
  print(string.format("加载模型错误 原因: %s", Err))
end
function pdocr(x1, y1, x2, y2)
  local BitMap = LuaEngine.snapShot(x1, y1, x2, y2)
  local VaLue = PaddLeOCR.Detect(BitMap, 0.45, 0.55)
  print(VaLue)
  if BitMap ~= nil and BitMap.isRecycled() == false then
    BitMap.recycle()
  end
  return VaLue
end
