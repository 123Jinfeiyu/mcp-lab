require("lockbox").insecure()
local Bit = require("lockbox.util.bit")
local String = require("string")
local Queue = require("lockbox.util.queue")
local SUBST = {
  41,
  46,
  67,
  201,
  162,
  216,
  124,
  1,
  61,
  54,
  84,
  161,
  236,
  240,
  6,
  19,
  98,
  167,
  5,
  243,
  192,
  199,
  115,
  140,
  152,
  147,
  43,
  217,
  188,
  76,
  130,
  202,
  30,
  155,
  87,
  60,
  253,
  212,
  224,
  22,
  103,
  66,
  111,
  24,
  138,
  23,
  229,
  18,
  190,
  78,
  196,
  214,
  218,
  158,
  222,
  73,
  160,
  251,
  245,
  142,
  187,
  47,
  238,
  122,
  169,
  104,
  121,
  145,
  21,
  178,
  7,
  63,
  148,
  194,
  16,
  137,
  11,
  34,
  95,
  33,
  128,
  127,
  93,
  154,
  90,
  144,
  50,
  39,
  53,
  62,
  204,
  231,
  191,
  247,
  151,
  3,
  255,
  25,
  48,
  179,
  72,
  165,
  181,
  209,
  215,
  94,
  146,
  42,
  172,
  86,
  170,
  198,
  79,
  184,
  56,
  210,
  150,
  164,
  125,
  182,
  118,
  252,
  107,
  226,
  156,
  116,
  4,
  241,
  69,
  157,
  112,
  89,
  100,
  113,
  135,
  32,
  134,
  91,
  207,
  101,
  230,
  45,
  168,
  2,
  27,
  96,
  37,
  173,
  174,
  176,
  185,
  246,
  28,
  70,
  97,
  105,
  52,
  64,
  126,
  15,
  85,
  71,
  163,
  35,
  221,
  81,
  175,
  58,
  195,
  92,
  249,
  206,
  186,
  197,
  234,
  38,
  44,
  83,
  13,
  110,
  133,
  40,
  132,
  9,
  211,
  223,
  205,
  244,
  65,
  129,
  77,
  82,
  106,
  220,
  55,
  200,
  108,
  193,
  171,
  250,
  36,
  225,
  123,
  8,
  12,
  189,
  177,
  74,
  120,
  136,
  149,
  139,
  227,
  99,
  232,
  109,
  233,
  203,
  213,
  254,
  59,
  0,
  29,
  57,
  242,
  239,
  183,
  14,
  102,
  88,
  208,
  228,
  166,
  119,
  114,
  248,
  235,
  117,
  75,
  10,
  49,
  68,
  80,
  180,
  143,
  237,
  31,
  26,
  219,
  153,
  141,
  51,
  159,
  17,
  131,
  20
}
local XOR = Bit.bxor
local MD2 = function()
  local queue = Queue()
  local X = {}
  for i = 0, 47 do
    X[i] = 0
  end
  local C = {}
  for i = 0, 15 do
    C[i] = 0
  end
  local public = {}
  local processBlock = function()
    local block = {}
    for i = 0, 15 do
      block[i] = queue.pop()
    end
    for i = 0, 15 do
      X[i + 16] = block[i]
      X[i + 32] = XOR(X[i], block[i])
    end
    local t
    t = 0
    for i = 0, 17 do
      for j = 0, 47 do
        X[j] = XOR(X[j], SUBST[t + 1])
        t = X[j]
      end
      t = (t + i) % 256
    end
    t = C[15]
    for i = 0, 15 do
      C[i] = XOR(C[i], SUBST[XOR(block[i], t) + 1])
      t = C[i]
    end
  end
  function public.init()
    queue.reset()
    X = {}
    for i = 0, 47 do
      X[i] = 0
    end
    C = {}
    for i = 0, 15 do
      C[i] = 0
    end
    return public
  end
  function public.update(stream)
    for b in stream, nil, nil do
      queue.push(b)
      if queue.size() >= 16 then
        processBlock()
      end
    end
    return public
  end
  function public.finish()
    local i = 16 - queue.size()
    while queue.size() < 16 do
      queue.push(i)
    end
    processBlock()
    queue.push(C[0])
    queue.push(C[1])
    queue.push(C[2])
    queue.push(C[3])
    queue.push(C[4])
    queue.push(C[5])
    queue.push(C[6])
    queue.push(C[7])
    queue.push(C[8])
    queue.push(C[9])
    queue.push(C[10])
    queue.push(C[11])
    queue.push(C[12])
    queue.push(C[13])
    queue.push(C[14])
    queue.push(C[15])
    processBlock()
    return public
  end
  function public.asBytes()
    return {
      X[0],
      X[1],
      X[2],
      X[3],
      X[4],
      X[5],
      X[6],
      X[7],
      X[8],
      X[9],
      X[10],
      X[11],
      X[12],
      X[13],
      X[14],
      X[15]
    }
  end
  function public.asHex()
    return String.format("%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x", X[0], X[1], X[2], X[3], X[4], X[5], X[6], X[7], X[8], X[9], X[10], X[11], X[12], X[13], X[14], X[15])
  end
  return public
end
return MD2
