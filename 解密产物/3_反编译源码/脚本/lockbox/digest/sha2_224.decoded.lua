local Bit = require("lockbox.util.bit")
local String = require("string")
local Math = require("math")
local Queue = require("lockbox.util.queue")
local CONSTANTS = {
  1116352408,
  1899447441,
  3049323471,
  3921009573,
  961987163,
  1508970993,
  2453635748,
  2870763221,
  3624381080,
  310598401,
  607225278,
  1426881987,
  1925078388,
  2162078206,
  2614888103,
  3248222580,
  3835390401,
  4022224774,
  264347078,
  604807628,
  770255983,
  1249150122,
  1555081692,
  1996064986,
  2554220882,
  2821834349,
  2952996808,
  3210313671,
  3336571891,
  3584528711,
  113926993,
  338241895,
  666307205,
  773529912,
  1294757372,
  1396182291,
  1695183700,
  1986661051,
  2177026350,
  2456956037,
  2730485921,
  2820302411,
  3259730800,
  3345764771,
  3516065817,
  3600352804,
  4094571909,
  275423344,
  430227734,
  506948616,
  659060556,
  883997877,
  958139571,
  1322822218,
  1537002063,
  1747873779,
  1955562222,
  2024104815,
  2227730452,
  2361852424,
  2428436474,
  2756734187,
  3204031479,
  3329325298
}
local fmt = "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x" .. "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x"
local AND = Bit.band
local OR = Bit.bor
local NOT = Bit.bnot
local XOR = Bit.bxor
local RROT = Bit.rrotate
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
local dword2bytes = function(i)
  local b4, b5, b6, b7 = word2bytes(i)
  local b0, b1, b2, b3 = word2bytes(Math.floor(i / 4294967296))
  return b0, b1, b2, b3, b4, b5, b6, b7
end
local SHA2_224 = function()
  local queue = Queue()
  local h0 = 3238371032
  local h1 = 914150663
  local h2 = 812702999
  local h3 = 4144912697
  local h4 = 4290775857
  local h5 = 1750603025
  local h6 = 1694076839
  local h7 = 3204075428
  local public = {}
  local processBlock = function()
    local a = h0
    local b = h1
    local c = h2
    local d = h3
    local e = h4
    local f = h5
    local g = h6
    local h = h7
    local w = {}
    for i = 0, 15 do
      w[i] = bytes2word(queue.pop(), queue.pop(), queue.pop(), queue.pop())
    end
    for i = 16, 63 do
      local s0 = XOR(RROT(w[i - 15], 7), XOR(RROT(w[i - 15], 18), RSHIFT(w[i - 15], 3)))
      local s1 = XOR(RROT(w[i - 2], 17), XOR(RROT(w[i - 2], 19), RSHIFT(w[i - 2], 10)))
      w[i] = AND(w[i - 16] + s0 + w[i - 7] + s1, 4294967295)
    end
    for i = 0, 63 do
      local s1 = XOR(RROT(e, 6), XOR(RROT(e, 11), RROT(e, 25)))
      local ch = XOR(AND(e, f), AND(NOT(e), g))
      local temp1 = h + s1 + ch + CONSTANTS[i + 1] + w[i]
      local s0 = XOR(RROT(a, 2), XOR(RROT(a, 13), RROT(a, 22)))
      local maj = XOR(AND(a, b), XOR(AND(a, c), AND(b, c)))
      local temp2 = s0 + maj
      h = g
      g = f
      f = e
      e = d + temp1
      d = c
      c = b
      b = a
      a = temp1 + temp2
    end
    h0 = AND(h0 + a, 4294967295)
    h1 = AND(h1 + b, 4294967295)
    h2 = AND(h2 + c, 4294967295)
    h3 = AND(h3 + d, 4294967295)
    h4 = AND(h4 + e, 4294967295)
    h5 = AND(h5 + f, 4294967295)
    h6 = AND(h6 + g, 4294967295)
    h7 = AND(h7 + h, 4294967295)
  end
  function public.init()
    queue.reset()
    h0 = 3238371032
    h1 = 914150663
    h2 = 812702999
    h3 = 4144912697
    h4 = 4290775857
    h5 = 1750603025
    h6 = 1694076839
    h7 = 3204075428
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
    local b0, b1, b2, b3 = word2bytes(h0)
    local b4, b5, b6, b7 = word2bytes(h1)
    local b8, b9, b10, b11 = word2bytes(h2)
    local b12, b13, b14, b15 = word2bytes(h3)
    local b16, b17, b18, b19 = word2bytes(h4)
    local b20, b21, b22, b23 = word2bytes(h5)
    local b24, b25, b26, b27 = word2bytes(h6)
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
      b19,
      b20,
      b21,
      b22,
      b23,
      b24,
      b25,
      b26,
      b27
    }
  end
  function public.asHex()
    local b0, b1, b2, b3 = word2bytes(h0)
    local b4, b5, b6, b7 = word2bytes(h1)
    local b8, b9, b10, b11 = word2bytes(h2)
    local b12, b13, b14, b15 = word2bytes(h3)
    local b16, b17, b18, b19 = word2bytes(h4)
    local b20, b21, b22, b23 = word2bytes(h5)
    local b24, b25, b26, b27 = word2bytes(h6)
    return String.format(fmt, b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15, b16, b17, b18, b19, b20, b21, b22, b23, b24, b25, b26, b27)
  end
  return public
end
return SHA2_224
