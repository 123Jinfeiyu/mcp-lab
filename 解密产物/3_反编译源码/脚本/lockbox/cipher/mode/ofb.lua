local Array = require("lockbox.util.array")
local Stream = require("lockbox.util.stream")
local Queue = require("lockbox.util.queue")
local OFB = {}
function OFB.Cipher()
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
          iv = out
          out = Array.XOR(out, block)
          Array.writeToQueue(outputQueue, out)
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
function OFB.Decipher()
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
          iv = out
          out = Array.XOR(out, block)
          Array.writeToQueue(outputQueue, out)
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
return OFB
