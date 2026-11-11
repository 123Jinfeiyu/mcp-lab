local Array = require("lockbox.util.array")
local Stream = require("lockbox.util.stream")
local Queue = require("lockbox.util.queue")
local IGE = {}
function IGE.Cipher()
  local public = {}
  local key, blockCipher, padding, inputQueue, outputQueue, xPrev, yPrev
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
    xPrev = nil
    yPrev = nil
    return public
  end
  function public.update(messageStream)
    local byte = messageStream()
    while byte ~= nil do
      inputQueue.push(byte)
      if inputQueue.size() >= blockCipher.blockSize then
        local block = Array.readFromQueue(inputQueue, blockCipher.blockSize)
        if yPrev == nil then
          yPrev = block
        elseif xPrev == nil then
          xPrev = block
        else
          local out = Array.XOR(yPrev, block)
          out = blockCipher.encrypt(key, out)
          out = Array.XOR(out, xPrev)
          Array.writeToQueue(outputQueue, out)
          xPrev = block
          yPrev = out
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
function IGE.Decipher()
  local public = {}
  local key, blockCipher, padding, inputQueue, outputQueue, xPrev, yPrev
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
    xPrev = nil
    yPrev = nil
    return public
  end
  function public.update(messageStream)
    local byte = messageStream()
    while byte ~= nil do
      inputQueue.push(byte)
      if inputQueue.size() >= blockCipher.blockSize then
        local block = Array.readFromQueue(inputQueue, blockCipher.blockSize)
        if xPrev == nil then
          xPrev = block
        elseif yPrev == nil then
          yPrev = block
        else
          local out = Array.XOR(yPrev, block)
          out = blockCipher.decrypt(key, out)
          out = Array.XOR(out, xPrev)
          Array.writeToQueue(outputQueue, out)
          xPrev = block
          yPrev = out
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
return IGE
