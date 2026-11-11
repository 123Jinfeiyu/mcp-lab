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
  return XOR(x, XOR(y, z))
end
local G = function(x, y, z)
  return OR(AND(x, y), AND(NOT(x), z))
end
local H = function(x, y, z)
  return XOR(OR(x, NOT(y)), z)
end
local I = function(x, y, z)
  return OR(AND(x, z), AND(y, NOT(z)))
end
local J = function(x, y, z)
  return XOR(x, OR(y, NOT(z)))
end
local FF = function(a, b, c, d, e, x, s)
  a = a + F(b, c, d) + x
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local GG = function(a, b, c, d, e, x, s)
  a = a + G(b, c, d) + x + 1518500249
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local HH = function(a, b, c, d, e, x, s)
  a = a + H(b, c, d) + x + 1859775393
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local II = function(a, b, c, d, e, x, s)
  a = a + I(b, c, d) + x + 2400959708
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local JJ = function(a, b, c, d, e, x, s)
  a = a + J(b, c, d) + x + 2840853838
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local FFF = function(a, b, c, d, e, x, s)
  a = a + F(b, c, d) + x
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local GGG = function(a, b, c, d, e, x, s)
  a = a + G(b, c, d) + x + 2053994217
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local HHH = function(a, b, c, d, e, x, s)
  a = a + H(b, c, d) + x + 1836072691
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local III = function(a, b, c, d, e, x, s)
  a = a + I(b, c, d) + x + 1548603684
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local JJJ = function(a, b, c, d, e, x, s)
  a = a + J(b, c, d) + x + 1352829926
  a = LROT(a, s) + e
  a = AND(a, 4294967295)
  return a
end
local RIPEMD160 = function()
  local queue = Queue()
  local A = 1732584193
  local B = 4023233417
  local C = 2562383102
  local D = 271733878
  local E = 3285377520
  local public = {}
  local processBlock = function()
    local aa, bb, cc, dd, ee = A, B, C, D, E
    local aaa, bbb, ccc, ddd, eee = A, B, C, D, E
    local X = {}
    for i = 0, 15 do
      X[i] = bytes2word(queue.pop(), queue.pop(), queue.pop(), queue.pop())
    end
    aa, cc = FF(aa, bb, cc, dd, ee, X[0], 11), LROT(cc, 10)
    ee, bb = FF(ee, aa, bb, cc, dd, X[1], 14), LROT(bb, 10)
    dd, aa = FF(dd, ee, aa, bb, cc, X[2], 15), LROT(aa, 10)
    cc, ee = FF(cc, dd, ee, aa, bb, X[3], 12), LROT(ee, 10)
    bb, dd = FF(bb, cc, dd, ee, aa, X[4], 5), LROT(dd, 10)
    aa, cc = FF(aa, bb, cc, dd, ee, X[5], 8), LROT(cc, 10)
    ee, bb = FF(ee, aa, bb, cc, dd, X[6], 7), LROT(bb, 10)
    dd, aa = FF(dd, ee, aa, bb, cc, X[7], 9), LROT(aa, 10)
    cc, ee = FF(cc, dd, ee, aa, bb, X[8], 11), LROT(ee, 10)
    bb, dd = FF(bb, cc, dd, ee, aa, X[9], 13), LROT(dd, 10)
    aa, cc = FF(aa, bb, cc, dd, ee, X[10], 14), LROT(cc, 10)
    ee, bb = FF(ee, aa, bb, cc, dd, X[11], 15), LROT(bb, 10)
    dd, aa = FF(dd, ee, aa, bb, cc, X[12], 6), LROT(aa, 10)
    cc, ee = FF(cc, dd, ee, aa, bb, X[13], 7), LROT(ee, 10)
    bb, dd = FF(bb, cc, dd, ee, aa, X[14], 9), LROT(dd, 10)
    aa, cc = FF(aa, bb, cc, dd, ee, X[15], 8), LROT(cc, 10)
    ee, bb = GG(ee, aa, bb, cc, dd, X[7], 7), LROT(bb, 10)
    dd, aa = GG(dd, ee, aa, bb, cc, X[4], 6), LROT(aa, 10)
    cc, ee = GG(cc, dd, ee, aa, bb, X[13], 8), LROT(ee, 10)
    bb, dd = GG(bb, cc, dd, ee, aa, X[1], 13), LROT(dd, 10)
    aa, cc = GG(aa, bb, cc, dd, ee, X[10], 11), LROT(cc, 10)
    ee, bb = GG(ee, aa, bb, cc, dd, X[6], 9), LROT(bb, 10)
    dd, aa = GG(dd, ee, aa, bb, cc, X[15], 7), LROT(aa, 10)
    cc, ee = GG(cc, dd, ee, aa, bb, X[3], 15), LROT(ee, 10)
    bb, dd = GG(bb, cc, dd, ee, aa, X[12], 7), LROT(dd, 10)
    aa, cc = GG(aa, bb, cc, dd, ee, X[0], 12), LROT(cc, 10)
    ee, bb = GG(ee, aa, bb, cc, dd, X[9], 15), LROT(bb, 10)
    dd, aa = GG(dd, ee, aa, bb, cc, X[5], 9), LROT(aa, 10)
    cc, ee = GG(cc, dd, ee, aa, bb, X[2], 11), LROT(ee, 10)
    bb, dd = GG(bb, cc, dd, ee, aa, X[14], 7), LROT(dd, 10)
    aa, cc = GG(aa, bb, cc, dd, ee, X[11], 13), LROT(cc, 10)
    ee, bb = GG(ee, aa, bb, cc, dd, X[8], 12), LROT(bb, 10)
    dd, aa = HH(dd, ee, aa, bb, cc, X[3], 11), LROT(aa, 10)
    cc, ee = HH(cc, dd, ee, aa, bb, X[10], 13), LROT(ee, 10)
    bb, dd = HH(bb, cc, dd, ee, aa, X[14], 6), LROT(dd, 10)
    aa, cc = HH(aa, bb, cc, dd, ee, X[4], 7), LROT(cc, 10)
    ee, bb = HH(ee, aa, bb, cc, dd, X[9], 14), LROT(bb, 10)
    dd, aa = HH(dd, ee, aa, bb, cc, X[15], 9), LROT(aa, 10)
    cc, ee = HH(cc, dd, ee, aa, bb, X[8], 13), LROT(ee, 10)
    bb, dd = HH(bb, cc, dd, ee, aa, X[1], 15), LROT(dd, 10)
    aa, cc = HH(aa, bb, cc, dd, ee, X[2], 14), LROT(cc, 10)
    ee, bb = HH(ee, aa, bb, cc, dd, X[7], 8), LROT(bb, 10)
    dd, aa = HH(dd, ee, aa, bb, cc, X[0], 13), LROT(aa, 10)
    cc, ee = HH(cc, dd, ee, aa, bb, X[6], 6), LROT(ee, 10)
    bb, dd = HH(bb, cc, dd, ee, aa, X[13], 5), LROT(dd, 10)
    aa, cc = HH(aa, bb, cc, dd, ee, X[11], 12), LROT(cc, 10)
    ee, bb = HH(ee, aa, bb, cc, dd, X[5], 7), LROT(bb, 10)
    dd, aa = HH(dd, ee, aa, bb, cc, X[12], 5), LROT(aa, 10)
    cc, ee = II(cc, dd, ee, aa, bb, X[1], 11), LROT(ee, 10)
    bb, dd = II(bb, cc, dd, ee, aa, X[9], 12), LROT(dd, 10)
    aa, cc = II(aa, bb, cc, dd, ee, X[11], 14), LROT(cc, 10)
    ee, bb = II(ee, aa, bb, cc, dd, X[10], 15), LROT(bb, 10)
    dd, aa = II(dd, ee, aa, bb, cc, X[0], 14), LROT(aa, 10)
    cc, ee = II(cc, dd, ee, aa, bb, X[8], 15), LROT(ee, 10)
    bb, dd = II(bb, cc, dd, ee, aa, X[12], 9), LROT(dd, 10)
    aa, cc = II(aa, bb, cc, dd, ee, X[4], 8), LROT(cc, 10)
    ee, bb = II(ee, aa, bb, cc, dd, X[13], 9), LROT(bb, 10)
    dd, aa = II(dd, ee, aa, bb, cc, X[3], 14), LROT(aa, 10)
    cc, ee = II(cc, dd, ee, aa, bb, X[7], 5), LROT(ee, 10)
    bb, dd = II(bb, cc, dd, ee, aa, X[15], 6), LROT(dd, 10)
    aa, cc = II(aa, bb, cc, dd, ee, X[14], 8), LROT(cc, 10)
    ee, bb = II(ee, aa, bb, cc, dd, X[5], 6), LROT(bb, 10)
    dd, aa = II(dd, ee, aa, bb, cc, X[6], 5), LROT(aa, 10)
    cc, ee = II(cc, dd, ee, aa, bb, X[2], 12), LROT(ee, 10)
    bb, dd = JJ(bb, cc, dd, ee, aa, X[4], 9), LROT(dd, 10)
    aa, cc = JJ(aa, bb, cc, dd, ee, X[0], 15), LROT(cc, 10)
    ee, bb = JJ(ee, aa, bb, cc, dd, X[5], 5), LROT(bb, 10)
    dd, aa = JJ(dd, ee, aa, bb, cc, X[9], 11), LROT(aa, 10)
    cc, ee = JJ(cc, dd, ee, aa, bb, X[7], 6), LROT(ee, 10)
    bb, dd = JJ(bb, cc, dd, ee, aa, X[12], 8), LROT(dd, 10)
    aa, cc = JJ(aa, bb, cc, dd, ee, X[2], 13), LROT(cc, 10)
    ee, bb = JJ(ee, aa, bb, cc, dd, X[10], 12), LROT(bb, 10)
    dd, aa = JJ(dd, ee, aa, bb, cc, X[14], 5), LROT(aa, 10)
    cc, ee = JJ(cc, dd, ee, aa, bb, X[1], 12), LROT(ee, 10)
    bb, dd = JJ(bb, cc, dd, ee, aa, X[3], 13), LROT(dd, 10)
    aa, cc = JJ(aa, bb, cc, dd, ee, X[8], 14), LROT(cc, 10)
    ee, bb = JJ(ee, aa, bb, cc, dd, X[11], 11), LROT(bb, 10)
    dd, aa = JJ(dd, ee, aa, bb, cc, X[6], 8), LROT(aa, 10)
    cc, ee = JJ(cc, dd, ee, aa, bb, X[15], 5), LROT(ee, 10)
    bb, dd = JJ(bb, cc, dd, ee, aa, X[13], 6), LROT(dd, 10)
    aaa, ccc = JJJ(aaa, bbb, ccc, ddd, eee, X[5], 8), LROT(ccc, 10)
    eee, bbb = JJJ(eee, aaa, bbb, ccc, ddd, X[14], 9), LROT(bbb, 10)
    ddd, aaa = JJJ(ddd, eee, aaa, bbb, ccc, X[7], 9), LROT(aaa, 10)
    ccc, eee = JJJ(ccc, ddd, eee, aaa, bbb, X[0], 11), LROT(eee, 10)
    bbb, ddd = JJJ(bbb, ccc, ddd, eee, aaa, X[9], 13), LROT(ddd, 10)
    aaa, ccc = JJJ(aaa, bbb, ccc, ddd, eee, X[2], 15), LROT(ccc, 10)
    eee, bbb = JJJ(eee, aaa, bbb, ccc, ddd, X[11], 15), LROT(bbb, 10)
    ddd, aaa = JJJ(ddd, eee, aaa, bbb, ccc, X[4], 5), LROT(aaa, 10)
    ccc, eee = JJJ(ccc, ddd, eee, aaa, bbb, X[13], 7), LROT(eee, 10)
    bbb, ddd = JJJ(bbb, ccc, ddd, eee, aaa, X[6], 7), LROT(ddd, 10)
    aaa, ccc = JJJ(aaa, bbb, ccc, ddd, eee, X[15], 8), LROT(ccc, 10)
    eee, bbb = JJJ(eee, aaa, bbb, ccc, ddd, X[8], 11), LROT(bbb, 10)
    ddd, aaa = JJJ(ddd, eee, aaa, bbb, ccc, X[1], 14), LROT(aaa, 10)
    ccc, eee = JJJ(ccc, ddd, eee, aaa, bbb, X[10], 14), LROT(eee, 10)
    bbb, ddd = JJJ(bbb, ccc, ddd, eee, aaa, X[3], 12), LROT(ddd, 10)
    aaa, ccc = JJJ(aaa, bbb, ccc, ddd, eee, X[12], 6), LROT(ccc, 10)
    eee, bbb = III(eee, aaa, bbb, ccc, ddd, X[6], 9), LROT(bbb, 10)
    ddd, aaa = III(ddd, eee, aaa, bbb, ccc, X[11], 13), LROT(aaa, 10)
    ccc, eee = III(ccc, ddd, eee, aaa, bbb, X[3], 15), LROT(eee, 10)
    bbb, ddd = III(bbb, ccc, ddd, eee, aaa, X[7], 7), LROT(ddd, 10)
    aaa, ccc = III(aaa, bbb, ccc, ddd, eee, X[0], 12), LROT(ccc, 10)
    eee, bbb = III(eee, aaa, bbb, ccc, ddd, X[13], 8), LROT(bbb, 10)
    ddd, aaa = III(ddd, eee, aaa, bbb, ccc, X[5], 9), LROT(aaa, 10)
    ccc, eee = III(ccc, ddd, eee, aaa, bbb, X[10], 11), LROT(eee, 10)
    bbb, ddd = III(bbb, ccc, ddd, eee, aaa, X[14], 7), LROT(ddd, 10)
    aaa, ccc = III(aaa, bbb, ccc, ddd, eee, X[15], 7), LROT(ccc, 10)
    eee, bbb = III(eee, aaa, bbb, ccc, ddd, X[8], 12), LROT(bbb, 10)
    ddd, aaa = III(ddd, eee, aaa, bbb, ccc, X[12], 7), LROT(aaa, 10)
    ccc, eee = III(ccc, ddd, eee, aaa, bbb, X[4], 6), LROT(eee, 10)
    bbb, ddd = III(bbb, ccc, ddd, eee, aaa, X[9], 15), LROT(ddd, 10)
    aaa, ccc = III(aaa, bbb, ccc, ddd, eee, X[1], 13), LROT(ccc, 10)
    eee, bbb = III(eee, aaa, bbb, ccc, ddd, X[2], 11), LROT(bbb, 10)
    ddd, aaa = HHH(ddd, eee, aaa, bbb, ccc, X[15], 9), LROT(aaa, 10)
    ccc, eee = HHH(ccc, ddd, eee, aaa, bbb, X[5], 7), LROT(eee, 10)
    bbb, ddd = HHH(bbb, ccc, ddd, eee, aaa, X[1], 15), LROT(ddd, 10)
    aaa, ccc = HHH(aaa, bbb, ccc, ddd, eee, X[3], 11), LROT(ccc, 10)
    eee, bbb = HHH(eee, aaa, bbb, ccc, ddd, X[7], 8), LROT(bbb, 10)
    ddd, aaa = HHH(ddd, eee, aaa, bbb, ccc, X[14], 6), LROT(aaa, 10)
    ccc, eee = HHH(ccc, ddd, eee, aaa, bbb, X[6], 6), LROT(eee, 10)
    bbb, ddd = HHH(bbb, ccc, ddd, eee, aaa, X[9], 14), LROT(ddd, 10)
    aaa, ccc = HHH(aaa, bbb, ccc, ddd, eee, X[11], 12), LROT(ccc, 10)
    eee, bbb = HHH(eee, aaa, bbb, ccc, ddd, X[8], 13), LROT(bbb, 10)
    ddd, aaa = HHH(ddd, eee, aaa, bbb, ccc, X[12], 5), LROT(aaa, 10)
    ccc, eee = HHH(ccc, ddd, eee, aaa, bbb, X[2], 14), LROT(eee, 10)
    bbb, ddd = HHH(bbb, ccc, ddd, eee, aaa, X[10], 13), LROT(ddd, 10)
    aaa, ccc = HHH(aaa, bbb, ccc, ddd, eee, X[0], 13), LROT(ccc, 10)
    eee, bbb = HHH(eee, aaa, bbb, ccc, ddd, X[4], 7), LROT(bbb, 10)
    ddd, aaa = HHH(ddd, eee, aaa, bbb, ccc, X[13], 5), LROT(aaa, 10)
    ccc, eee = GGG(ccc, ddd, eee, aaa, bbb, X[8], 15), LROT(eee, 10)
    bbb, ddd = GGG(bbb, ccc, ddd, eee, aaa, X[6], 5), LROT(ddd, 10)
    aaa, ccc = GGG(aaa, bbb, ccc, ddd, eee, X[4], 8), LROT(ccc, 10)
    eee, bbb = GGG(eee, aaa, bbb, ccc, ddd, X[1], 11), LROT(bbb, 10)
    ddd, aaa = GGG(ddd, eee, aaa, bbb, ccc, X[3], 14), LROT(aaa, 10)
    ccc, eee = GGG(ccc, ddd, eee, aaa, bbb, X[11], 14), LROT(eee, 10)
    bbb, ddd = GGG(bbb, ccc, ddd, eee, aaa, X[15], 6), LROT(ddd, 10)
    aaa, ccc = GGG(aaa, bbb, ccc, ddd, eee, X[0], 14), LROT(ccc, 10)
    eee, bbb = GGG(eee, aaa, bbb, ccc, ddd, X[5], 6), LROT(bbb, 10)
    ddd, aaa = GGG(ddd, eee, aaa, bbb, ccc, X[12], 9), LROT(aaa, 10)
    ccc, eee = GGG(ccc, ddd, eee, aaa, bbb, X[2], 12), LROT(eee, 10)
    bbb, ddd = GGG(bbb, ccc, ddd, eee, aaa, X[13], 9), LROT(ddd, 10)
    aaa, ccc = GGG(aaa, bbb, ccc, ddd, eee, X[9], 12), LROT(ccc, 10)
    eee, bbb = GGG(eee, aaa, bbb, ccc, ddd, X[7], 5), LROT(bbb, 10)
    ddd, aaa = GGG(ddd, eee, aaa, bbb, ccc, X[10], 15), LROT(aaa, 10)
    ccc, eee = GGG(ccc, ddd, eee, aaa, bbb, X[14], 8), LROT(eee, 10)
    bbb, ddd = FFF(bbb, ccc, ddd, eee, aaa, X[12], 8), LROT(ddd, 10)
    aaa, ccc = FFF(aaa, bbb, ccc, ddd, eee, X[15], 5), LROT(ccc, 10)
    eee, bbb = FFF(eee, aaa, bbb, ccc, ddd, X[10], 12), LROT(bbb, 10)
    ddd, aaa = FFF(ddd, eee, aaa, bbb, ccc, X[4], 9), LROT(aaa, 10)
    ccc, eee = FFF(ccc, ddd, eee, aaa, bbb, X[1], 12), LROT(eee, 10)
    bbb, ddd = FFF(bbb, ccc, ddd, eee, aaa, X[5], 5), LROT(ddd, 10)
    aaa, ccc = FFF(aaa, bbb, ccc, ddd, eee, X[8], 14), LROT(ccc, 10)
    eee, bbb = FFF(eee, aaa, bbb, ccc, ddd, X[7], 6), LROT(bbb, 10)
    ddd, aaa = FFF(ddd, eee, aaa, bbb, ccc, X[6], 8), LROT(aaa, 10)
    ccc, eee = FFF(ccc, ddd, eee, aaa, bbb, X[2], 13), LROT(eee, 10)
    bbb, ddd = FFF(bbb, ccc, ddd, eee, aaa, X[13], 6), LROT(ddd, 10)
    aaa, ccc = FFF(aaa, bbb, ccc, ddd, eee, X[14], 5), LROT(ccc, 10)
    eee, bbb = FFF(eee, aaa, bbb, ccc, ddd, X[0], 15), LROT(bbb, 10)
    ddd, aaa = FFF(ddd, eee, aaa, bbb, ccc, X[3], 13), LROT(aaa, 10)
    ccc, eee = FFF(ccc, ddd, eee, aaa, bbb, X[9], 11), LROT(eee, 10)
    bbb, ddd = FFF(bbb, ccc, ddd, eee, aaa, X[11], 11), LROT(ddd, 10)
    A, B, C, D, E = AND(B + cc + ddd, 4294967295), AND(C + dd + eee, 4294967295), AND(D + ee + aaa, 4294967295), AND(E + aa + bbb, 4294967295), AND(A + bb + ccc, 4294967295)
  end
  function public.init()
    queue.reset()
    A = 1732584193
    B = 4023233417
    C = 2562383102
    D = 271733878
    E = 3285377520
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
    local b16, b17, b18, b19 = word2bytes(E)
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
      b15,
      b16,
      b17,
      b18,
      b19
    }
  end
  function public.asHex()
    local b0, b1, b2, b3 = word2bytes(A)
    local b4, b5, b6, b7 = word2bytes(B)
    local b8, b9, b10, b11 = word2bytes(C)
    local b12, b13, b14, b15 = word2bytes(D)
    local b16, b17, b18, b19 = word2bytes(E)
    local fmt = "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x"
    return String.format(fmt, b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15, b16, b17, b18, b19)
  end
  return public
end
return RIPEMD160
