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
  print(string.format("\229\136\157\229\167\139\229\140\150OCR\230\143\146\228\187\182\229\164\177\232\180\165 \229\142\159\229\155\160: %s", Err))
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
  print(string.format("\229\138\160\232\189\189\230\168\161\229\158\139\233\148\153\232\175\175 \229\142\159\229\155\160: %s", Err))
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
