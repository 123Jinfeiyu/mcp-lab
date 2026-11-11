local Bit = require("lockbox.util.bit")
local Stream = require("lockbox.util.stream")
local Array = require("lockbox.util.array")
local XOR = Bit.bxor
local HMAC = function()
  local public = {}
  local blockSize = 64
  local Digest
  local outerPadding = {}
  local innerPadding = {}
  local digest
  function public.setBlockSize(bytes)
    blockSize = bytes
    return public
  end
  function public.setDigest(digestModule)
    Digest = digestModule
    digest = Digest()
    return public
  end
  function public.setKey(key)
    local keyStream
    if Array.size(key) > blockSize then
      keyStream = Stream.fromArray(Digest().update(Stream.fromArray(key)).finish().asBytes())
    else
      keyStream = Stream.fromArray(key)
    end
    outerPadding = {}
    innerPadding = {}
    for i = 1, blockSize do
      local byte = keyStream()
      if byte == nil then
        byte = 0
      end
      outerPadding[i] = XOR(92, byte)
      innerPadding[i] = XOR(54, byte)
    end
    return public
  end
  function public.init()
    digest.init().update(Stream.fromArray(innerPadding))
    return public
  end
  function public.update(messageStream)
    digest.update(messageStream)
    return public
  end
  function public.finish()
    local inner = digest.finish().asBytes()
    digest.init().update(Stream.fromArray(outerPadding)).update(Stream.fromArray(inner)).finish()
    return public
  end
  function public.asBytes()
    return digest.asBytes()
  end
  function public.asHex()
    return digest.asHex()
  end
  return public
end
return HMAC
