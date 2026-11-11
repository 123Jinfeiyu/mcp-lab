require("lockbox").insecure()
local Array = require("lockbox.util.array")
local Stream = require("lockbox.util.stream")
local Queue = require("lockbox.util.queue")
local ECB = {}
function ECB.Cipher()
  local public = {}
  local key, blockCipher, padding, inputQueue, outputQueue
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
    return public
  end
  function public.update(messageStream)
    local byte = messageStream()
    while byte ~= nil do
      inputQueue.push(byte)
      if inputQueue.size() >= blockCipher.blockSize then
        local block = Array.readFromQueue(inputQueue, blockCipher.blockSize)
        block = blockCipher.encrypt(key, block)
        Array.writeToQueue(outputQueue, block)
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
function ECB.Decipher()
  local public = {}
  local key, blockCipher, padding, inputQueue, outputQueue
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
    return public
  end
  function public.update(messageStream)
    local byte = messageStream()
    while byte ~= nil do
      inputQueue.push(byte)
      if inputQueue.size() >= blockCipher.blockSize then
        local block = Array.readFromQueue(inputQueue, blockCipher.blockSize)
        block = blockCipher.decrypt(key, block)
        Array.writeToQueue(outputQueue, block)
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
return ECB
