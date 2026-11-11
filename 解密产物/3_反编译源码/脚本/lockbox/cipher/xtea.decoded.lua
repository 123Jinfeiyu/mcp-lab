require("lockbox").insecure()
local Bit = require("lockbox.util.bit")
local AND = Bit.band
local OR = Bit.bor
local XOR = Bit.bxor
local LSHIFT = Bit.lshift
local RSHIFT = Bit.rshift
local bytes2word = function(b0, b1, b2, b3)
  local i = b0
  i = LSHIFT(i, 8)
  i = OR(i, b1)
  i = LSHIFT(i, 8)
  i = OR(i, b2)
  i = LSHIFT(i, 8)
  i = OR(i, b3)
  return i
end
local word2bytes = function(word)
  local b0, b1, b2, b3
  b3 = AND(word, 255)
  word = RSHIFT(word, 8)
  b2 = AND(word, 255)
  word = RSHIFT(word, 8)
  b1 = AND(word, 255)
  word = RSHIFT(word, 8)
  b0 = AND(word, 255)
  return b0, b1, b2, b3
end
local XTEA = {}
XTEA.blockSize = 8
function XTEA.encrypt(key, data)
  local y = bytes2word(data[1], data[2], data[3], data[4])
  local z = bytes2word(data[5], data[6], data[7], data[8])
  local delta = 2654435769
  local sum = 0
  local k0 = bytes2word(key[1], key[2], key[3], key[4])
  local k1 = bytes2word(key[5], key[6], key[7], key[8])
  local k2 = bytes2word(key[9], key[10], key[11], key[12])
  local k3 = bytes2word(key[13], key[14], key[15], key[16])
  local k = {
    [0] = k0,
    k1,
    k2,
    k3
  }
  for _ = 1, 32 do
    local temp
    temp = XOR(LSHIFT(z, 4), RSHIFT(z, 5)) + z
    temp = XOR(temp, sum + k[AND(sum, 3)])
    y = AND(y + temp, 4294967295)
    sum = AND(sum + delta, 4294967295)
    temp = XOR(LSHIFT(y, 4), RSHIFT(y, 5)) + y
    temp = XOR(temp, sum + k[AND(RSHIFT(sum, 11), 3)])
    z = AND(z + temp, 4294967295)
  end
  local out = {}
  out[1], out[2], out[3], out[4] = word2bytes(y)
  out[5], out[6], out[7], out[8] = word2bytes(z)
  return out
end
function XTEA.decrypt(key, data)
  local y = bytes2word(data[1], data[2], data[3], data[4])
  local z = bytes2word(data[5], data[6], data[7], data[8])
  local delta = 2654435769
  local sum = 3337565984
  local k0 = bytes2word(key[1], key[2], key[3], key[4])
  local k1 = bytes2word(key[5], key[6], key[7], key[8])
  local k2 = bytes2word(key[9], key[10], key[11], key[12])
  local k3 = bytes2word(key[13], key[14], key[15], key[16])
  local k = {
    [0] = k0,
    k1,
    k2,
    k3
  }
  for _ = 1, 32 do
    local temp
    temp = XOR(LSHIFT(y, 4), RSHIFT(y, 5)) + y
    temp = XOR(temp, sum + k[AND(RSHIFT(sum, 11), 3)])
    z = AND(z + 4294967296 - temp, 4294967295)
    sum = AND(sum + 4294967296 - delta, 4294967295)
    temp = XOR(LSHIFT(z, 4), RSHIFT(z, 5)) + z
    temp = XOR(temp, sum + k[AND(sum, 3)])
    y = AND(y + 4294967296 - temp, 4294967295)
  end
  local out = {}
  out[1], out[2], out[3], out[4] = word2bytes(y)
  out[5], out[6], out[7], out[8] = word2bytes(z)
  return out
end
return XTEA
