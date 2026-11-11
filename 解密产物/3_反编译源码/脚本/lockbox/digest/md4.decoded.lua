require("lockbox").insecure()
local Bit = require("lockbox.util.bit")
local String = require("string")
local Math = require("math")
local Queue = require("lockbox.util.queue")
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
  return OR(AND(x, y), OR(AND(x, z), AND(y, z)))
end
local H = function(x, y, z)
  return XOR(x, XOR(y, z))
end
local MD4 = function()
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
    for i = 0, 15 do
      X[i] = bytes2word(queue.pop(), queue.pop(), queue.pop(), queue.pop())
    end
    a = LROT(a + F(b, c, d) + X[0], 3)
    d = LROT(d + F(a, b, c) + X[1], 7)
    c = LROT(c + F(d, a, b) + X[2], 11)
    b = LROT(b + F(c, d, a) + X[3], 19)
    a = LROT(a + F(b, c, d) + X[4], 3)
    d = LROT(d + F(a, b, c) + X[5], 7)
    c = LROT(c + F(d, a, b) + X[6], 11)
    b = LROT(b + F(c, d, a) + X[7], 19)
    a = LROT(a + F(b, c, d) + X[8], 3)
    d = LROT(d + F(a, b, c) + X[9], 7)
    c = LROT(c + F(d, a, b) + X[10], 11)
    b = LROT(b + F(c, d, a) + X[11], 19)
    a = LROT(a + F(b, c, d) + X[12], 3)
    d = LROT(d + F(a, b, c) + X[13], 7)
    c = LROT(c + F(d, a, b) + X[14], 11)
    b = LROT(b + F(c, d, a) + X[15], 19)
    a = LROT(a + G(b, c, d) + X[0] + 1518500249, 3)
    d = LROT(d + G(a, b, c) + X[4] + 1518500249, 5)
    c = LROT(c + G(d, a, b) + X[8] + 1518500249, 9)
    b = LROT(b + G(c, d, a) + X[12] + 1518500249, 13)
    a = LROT(a + G(b, c, d) + X[1] + 1518500249, 3)
    d = LROT(d + G(a, b, c) + X[5] + 1518500249, 5)
    c = LROT(c + G(d, a, b) + X[9] + 1518500249, 9)
    b = LROT(b + G(c, d, a) + X[13] + 1518500249, 13)
    a = LROT(a + G(b, c, d) + X[2] + 1518500249, 3)
    d = LROT(d + G(a, b, c) + X[6] + 1518500249, 5)
    c = LROT(c + G(d, a, b) + X[10] + 1518500249, 9)
    b = LROT(b + G(c, d, a) + X[14] + 1518500249, 13)
    a = LROT(a + G(b, c, d) + X[3] + 1518500249, 3)
    d = LROT(d + G(a, b, c) + X[7] + 1518500249, 5)
    c = LROT(c + G(d, a, b) + X[11] + 1518500249, 9)
    b = LROT(b + G(c, d, a) + X[15] + 1518500249, 13)
    a = LROT(a + H(b, c, d) + X[0] + 1859775393, 3)
    d = LROT(d + H(a, b, c) + X[8] + 1859775393, 9)
    c = LROT(c + H(d, a, b) + X[4] + 1859775393, 11)
    b = LROT(b + H(c, d, a) + X[12] + 1859775393, 15)
    a = LROT(a + H(b, c, d) + X[2] + 1859775393, 3)
    d = LROT(d + H(a, b, c) + X[10] + 1859775393, 9)
    c = LROT(c + H(d, a, b) + X[6] + 1859775393, 11)
    b = LROT(b + H(c, d, a) + X[14] + 1859775393, 15)
    a = LROT(a + H(b, c, d) + X[1] + 1859775393, 3)
    d = LROT(d + H(a, b, c) + X[9] + 1859775393, 9)
    c = LROT(c + H(d, a, b) + X[5] + 1859775393, 11)
    b = LROT(b + H(c, d, a) + X[13] + 1859775393, 15)
    a = LROT(a + H(b, c, d) + X[3] + 1859775393, 3)
    d = LROT(d + H(a, b, c) + X[11] + 1859775393, 9)
    c = LROT(c + H(d, a, b) + X[7] + 1859775393, 11)
    b = LROT(b + H(c, d, a) + X[15] + 1859775393, 15)
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
return MD4
