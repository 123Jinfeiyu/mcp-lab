local HMAC = require("lockbox.mac.hmac")
local Stream = require("lockbox.util.stream")
local Array = require("lockbox.util.array")
local HKDF = function()
  local public = {}
  local Digest, inputKeyMaterial
  local salt = Array.fromHex("0000000000000000000000000000000000000000")
  local info, outputLen, hashLen, secret
  local extract = function()
    local res = HMAC().setDigest(Digest).setKey(salt).init().update(Stream.fromArray(inputKeyMaterial)).finish().asBytes()
    hashLen = #res
    return res
  end
  local expand = function(prk)
    local iterations = math.ceil(outputLen / hashLen)
    local mixin = {}
    local results = {}
    local remainingBytes = outputLen
    for i = 1, iterations do
      local mac = HMAC().setDigest(Digest).setKey(prk).init()
      mac.update(Stream.fromArray(mixin))
      if info then
        mac.update(Stream.fromArray(info))
      end
      mac.update(Stream.fromArray({i}))
      local stepResult = mac.finish().asBytes()
      local stepSize = math.min(remainingBytes, #stepResult)
      for j = 1, stepSize do
        results[#results + 1] = stepResult[j]
      end
      mixin = stepResult
      remainingBytes = remainingBytes - stepSize
    end
    return results
  end
  function public.setDigest(digestModule)
    Digest = digestModule
    return public
  end
  function public.setInputKeyMaterial(ikm)
    inputKeyMaterial = ikm
    return public
  end
  function public.setSalt(s)
    salt = s or salt
    return public
  end
  function public.setInfo(i)
    info = i
    return public
  end
  function public.setOutputLen(len)
    outputLen = len
    return public
  end
  function public.finish()
    local prk = extract(salt, inputKeyMaterial)
    secret = expand(prk, info)
    return public
  end
  function public.asBytes()
    return secret
  end
  function public.asHex()
    return Array.toHex(secret)
  end
  return public
end
return HKDF
