require("lockbox").insecure()
local Bit = require("lockbox.util.bit")
local String = require("string")
local Math = require("math")
local Queue = require("lockbox.util.queue")
local SHIFT = {
  7,
  12,
  17,
  22,
  7,
  12,
  17,
  22,
  7,
  12,
  17,
  22,
  7,
  12,
  17,
  22,
  5,
  9,
  14,
  20,
  5,
  9,
  14,
  20,
  5,
  9,
  14,
  20,
  5,
  9,
  14,
  20,
  4,
  11,
  16,
  23,
  4,
  11,
  16,
  23,
  4,
  11,
  16,
  23,
  4,
  11,
  16,
  23,
  6,
  10,
  15,
  21,
  6,
  10,
  15,
  21,
  6,
  10,
  15,
  21,
  6,
  10,
  15,
  21
}
local CONSTANTS = {
  3614090360,
  3905402710,
  606105819,
  3250441966,
  4118548399,
  1200080426,
  2821735955,
  4249261313,
  1770035416,
  2336552879,
  4294925233,
  2304563134,
  1804603682,
  4254626195,
  2792965006,
  1236535329,
  4129170786,
  3225465664,
  643717713,
  3921069994,
  3593408605,
  38016083,
  3634488961,
  3889429448,
  568446438,
  3275163606,
  4107603335,
  1163531501,
  2850285829,
  4243563512,
  1735328473,
  2368359562,
  4294588738,
  2272392833,
  1839030562,
  4259657740,
  2763975236,
  1272893353,
  4139469664,
  3200236656,
  681279174,
  3936430074,
  3572445317,
  76029189,
  3654602809,
  3873151461,
  530742520,
  3299628645,
  4096336452,
  1126891415,
  2878612391,
  4237533241,
  1700485571,
  2399980690,
  4293915773,
  2240044497,
  1873313359,
  4264355552,
  2734768916,
  1309151649,
  4149444226,
  3174756917,
  718787259,
  3951481745
}
local AND = Bit.band
local OR = Bit.bor
local NOT = Bit.bnot
local XOR = Bit.bxor
local LROT = Bit.lrotate
local LSHIFT = Bit.lshift
local RSHIFT = Bit.rshift
local bytes2word = function(b0, b1, b2, b3)
  local i = b3
  i = LSHIFT(i, 8)
  i = OR(i, b2)
  i = LSHIFT(i, 8)
  i = OR(i, b1)
  i = LSHIFT(i, 8)
  i = OR(i, b0)
  return i
end
local word2bytes = function(word)
  local b0, b1, b2, b3
  b0 = AND(word, 255)
  word = RSHIFT(word, 8)
  b1 = AND(word, 255)
  word = RSHIFT(word, 8)
  b2 = AND(word, 255)
  word = RSHIFT(word, 8)
  b3 = AND(word, 255)
  return b0, b1, b2, b3
end
local dword2bytes = function(i)
  local b4, b5, b6, b7 = word2bytes(Math.floor(i / 4294967296))
  local b0, b1, b2, b3 = word2bytes(i)
  return b0, b1, b2, b3, b4, b5, b6, b7
end
local F = function(x, y, z)
  return OR(AND(x, y), AND(NOT(x), z))
end
local G = function(x, y, z)
  return OR(AND(x, z), AND(y, NOT(z)))
end
local H = function(x, y, z)
  return XOR(x, XOR(y, z))
end
local I = function(x, y, z)
  return XOR(y, OR(x, NOT(z)))
end
local MD5 = function()
  local queue = Queue()
  local A = 1732584193
  local B = 4023233417
  local C = 2562383102
  local D = 271733878
  local public = {}
  local processBlock = function()
    local a = A
    local b = B
    local c = C
    local d = D
    local X = {}
    for i = 1, 16 do
      X[i] = bytes2word(queue.pop(), queue.pop(), queue.pop(), queue.pop())
    end
    for i = 0, 63 do
      local f, g, temp
      if i >= 0 and i <= 15 then
        f = F(b, c, d)
        g = i
      elseif i >= 16 and i <= 31 then
        f = G(b, c, d)
        g = (5 * i + 1) % 16
      elseif i >= 32 and i <= 47 then
        f = H(b, c, d)
        g = (3 * i + 5) % 16
      elseif i >= 48 and i <= 63 then
        f = I(b, c, d)
        g = 7 * i % 16
      end
      temp = d
      d = c
      c = b
      b = b + LROT(a + f + CONSTANTS[i + 1] + X[g + 1], SHIFT[i + 1])
      a = temp
    end
    A = AND(A + a, 4294967295)
    B = AND(B + b, 4294967295)
    C = AND(C + c, 4294967295)
    D = AND(D + d, 4294967295)
  end
  function public.init()
    queue.reset()
    A = 1732584193
    B = 4023233417
    C = 2562383102
    D = 271733878
    return public
  end
  function public.update(bytes)
    for b in bytes, nil, nil do
      queue.push(b)
      if queue.size() >= 64 then
        processBlock()
      end
    end
    return public
  end
  function public.finish()
    local bits = queue.getHead() * 8
    queue.push(128)
    while (queue.size() + 7) % 64 < 63 do
      queue.push(0)
    end
    local b0, b1, b2, b3, b4, b5, b6, b7 = dword2bytes(bits)
    queue.push(b0)
    queue.push(b1)
    queue.push(b2)
    queue.push(b3)
    queue.push(b4)
    queue.push(b5)
    queue.push(b6)
    queue.push(b7)
    while queue.size() > 0 do
      processBlock()
    end
    return public
  end
  function public.asBytes()
    local b0, b1, b2, b3 = word2bytes(A)
    local b4, b5, b6, b7 = word2bytes(B)
    local b8, b9, b10, b11 = word2bytes(C)
    local b12, b13, b14, b15 = word2bytes(D)
    return {
      b0,
      b1,
      b2,
      b3,
      b4,
      b5,
      b6,
      b7,
      b8,
      b9,
      b10,
      b11,
      b12,
      b13,
      b14,
      b15
    }
  end
  function public.asHex()
    local b0, b1, b2, b3 = word2bytes(A)
    local b4, b5, b6, b7 = word2bytes(B)
    local b8, b9, b10, b11 = word2bytes(C)
    local b12, b13, b14, b15 = word2bytes(D)
    return String.format("%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x", b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15)
  end
  return public
end
return MD5
