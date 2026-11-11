local Array = require("lockbox.util.array")
local Stream = require("lockbox.util.stream")
local Queue = require("lockbox.util.queue")
local Bit = require("lockbox.util.bit")
local AND = Bit.band
local CTR = {}
function CTR.Cipher()
  local public = {}
  local key, blockCipher, padding, inputQueue, outputQueue, iv
  function public.setKey(keyBytes)
    key = keyBytes
    return public
  end
  function public.setBlockCipher(cipher)
    blockCipher = cipher
    return public
  end
  function public.setPadding(paddingMode)
    padding = paddingMode
    return public
  end
  function public.init()
    inputQueue = Queue()
    outputQueue = Queue()
    iv = nil
    return public
  end
  local updateIV = function()
    iv[16] = iv[16] + 1
    if iv[16] <= 255 then
      return
    end
    iv[16] = AND(iv[16], 255)
    iv[15] = iv[15] + 1
    if 255 >= iv[15] then
      return
    end
    iv[15] = AND(iv[15], 255)
    iv[14] = iv[14] + 1
    if 255 >= iv[14] then
      return
    end
    iv[14] = AND(iv[14], 255)
    iv[13] = iv[13] + 1
    if 255 >= iv[13] then
      return
    end
    iv[13] = AND(iv[13], 255)
    iv[12] = iv[12] + 1
    if 255 >= iv[12] then
      return
    end
    iv[12] = AND(iv[12], 255)
    iv[11] = iv[11] + 1
    if 255 >= iv[11] then
      return
    end
    iv[11] = AND(iv[11], 255)
    iv[10] = iv[10] + 1
    if 255 >= iv[10] then
      return
    end
    iv[10] = AND(iv[10], 255)
    iv[9] = iv[9] + 1
    if 255 >= iv[9] then
      return
    end
    iv[9] = AND(iv[9], 255)
    return
  end
  function public.update(messageStream)
    local byte = messageStream()
    while byte ~= nil do
      inputQueue.push(byte)
      if inputQueue.size() >= blockCipher.blockSize then
        local block = Array.readFromQueue(inputQueue, blockCipher.blockSize)
        if iv == nil then
          iv = block
        else
          local out = iv
          out = blockCipher.encrypt(key, out)
          out = Array.XOR(out, block)
          Array.writeToQueue(outputQueue, out)
          updateIV()
        end
      end
      byte = messageStream()
    end
    return public
  end
  function public.finish()
    local paddingStream = padding(blockCipher.blockSize, inputQueue.getHead())
    public.update(paddingStream)
    return public
  end
  function public.getOutputQueue()
    return outputQueue
  end
  function public.asHex()
    return Stream.toHex(outputQueue.pop)
  end
  function public.asBytes()
    return Stream.toArray(outputQueue.pop)
  end
  return public
end
function CTR.Decipher()
  local public = {}
  local key, blockCipher, padding, inputQueue, outputQueue, iv
  function public.setKey(keyBytes)
    key = keyBytes
    return public
  end
  function public.setBlockCipher(cipher)
    blockCipher = cipher
    return public
  end
  function public.setPadding(paddingMode)
    padding = paddingMode
    return public
  end
  function public.init()
    inputQueue = Queue()
    outputQueue = Queue()
    iv = nil
    return public
  end
  local updateIV = function()
    iv[16] = iv[16] + 1
    if iv[16] <= 255 then
      return
    end
    iv[16] = AND(iv[16], 255)
    iv[15] = iv[15] + 1
    if 255 >= iv[15] then
      return
    end
    iv[15] = AND(iv[15], 255)
    iv[14] = iv[14] + 1
    if 255 >= iv[14] then
      return
    end
    iv[14] = AND(iv[14], 255)
    iv[13] = iv[13] + 1
    if 255 >= iv[13] then
      return
    end
    iv[13] = AND(iv[13], 255)
    iv[12] = iv[12] + 1
    if 255 >= iv[12] then
      return
    end
    iv[12] = AND(iv[12], 255)
    iv[11] = iv[11] + 1
    if 255 >= iv[11] then
      return
    end
    iv[11] = AND(iv[11], 255)
    iv[10] = iv[10] + 1
    if 255 >= iv[10] then
      return
    end
    iv[10] = AND(iv[10], 255)
    iv[9] = iv[9] + 1
    if 255 >= iv[9] then
      return
    end
    iv[9] = AND(iv[9], 255)
    return
  end
  function public.update(messageStream)
    local byte = messageStream()
    while byte ~= nil do
      inputQueue.push(byte)
      if inputQueue.size() >= blockCipher.blockSize then
        local block = Array.readFromQueue(inputQueue, blockCipher.blockSize)
        if iv == nil then
          iv = block
        else
          local out = iv
          out = blockCipher.encrypt(key, out)
          out = Array.XOR(out, block)
          Array.writeToQueue(outputQueue, out)
          updateIV()
        end
      end
      byte = messageStream()
    end
    return public
  end
  function public.finish()
    local paddingStream = padding(blockCipher.blockSize, inputQueue.getHead())
    public.update(paddingStream)
    return public
  end
  function public.getOutputQueue()
    return outputQueue
  end
  function public.asHex()
    return Stream.toHex(outputQueue.pop)
  end
  function public.asBytes()
    return Stream.toArray(outputQueue.pop)
  end
  return public
end
return CTR
