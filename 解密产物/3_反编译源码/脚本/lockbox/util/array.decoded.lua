local String = require("string")
local Bit = require("lockbox.util.bit")
local Queue = require("lockbox.util.queue")
local XOR = Bit.bxor
local Array = {}
function Array.size(array)
  return #array
end
function Array.fromString(string)
  local bytes = {}
  local i = 1
  local byte = String.byte(string, i)
  while byte ~= nil do
    bytes[i] = byte
    i = i + 1
    byte = String.byte(string, i)
  end
  return bytes
end
function Array.toString(bytes)
  local chars = {}
  local i = 1
  local byte = bytes[i]
  while byte ~= nil do
    chars[i] = String.char(byte)
    i = i + 1
    byte = bytes[i]
  end
  return table.concat(chars, "")
end
function Array.fromStream(stream)
  local array = {}
  local i = 1
  local byte = stream()
  while byte ~= nil do
    array[i] = byte
    i = i + 1
    byte = stream()
  end
  return array
end
function Array.readFromQueue(queue, size)
  local array = {}
  for i = 1, size do
    array[i] = queue.pop()
  end
  return array
end
function Array.writeToQueue(queue, array)
  local size = Array.size(array)
  for i = 1, size do
    queue.push(array[i])
  end
end
function Array.toStream(array)
  local queue = Queue()
  local i = 1
  local byte = array[i]
  while byte ~= nil do
    queue.push(byte)
    i = i + 1
    byte = array[i]
  end
  return queue.pop
end
local fromHexTable = {}
for i = 0, 255 do
  fromHexTable[String.format("%02X", i)] = i
  fromHexTable[String.format("%02x", i)] = i
end
function Array.fromHex(hex)
  local array = {}
  for i = 1, String.len(hex) / 2 do
    local h = String.sub(hex, i * 2 - 1, i * 2)
    array[i] = fromHexTable[h]
  end
  return array
end
local toHexTable = {}
for i = 0, 255 do
  toHexTable[i] = String.format("%02X", i)
end
function Array.toHex(array)
  local hex = {}
  local i = 1
  local byte = array[i]
  while byte ~= nil do
    hex[i] = toHexTable[byte]
    i = i + 1
    byte = array[i]
  end
  return table.concat(hex, "")
end
function Array.concat(a, b)
  local concat = {}
  local out = 1
  local i = 1
  local byte = a[i]
  while byte ~= nil do
    concat[out] = byte
    i = i + 1
    out = out + 1
    byte = a[i]
  end
  i = 1
  byte = b[i]
  while byte ~= nil do
    concat[out] = byte
    i = i + 1
    out = out + 1
    byte = b[i]
  end
  return concat
end
function Array.truncate(a, newSize)
  local x = {}
  for i = 1, newSize do
    x[i] = a[i]
  end
  return x
end
function Array.XOR(a, b)
  local x = {}
  for k, v in pairs(a) do
    x[k] = XOR(v, b[k])
  end
  return x
end
function Array.substitute(input, sbox)
  local out = {}
  for k, v in pairs(input) do
    out[k] = sbox[v]
  end
  return out
end
function Array.permute(input, pbox)
  local out = {}
  for k, v in pairs(pbox) do
    out[k] = input[v]
  end
  return out
end
function Array.copy(input)
  local out = {}
  for k, v in pairs(input) do
    out[k] = v
  end
  return out
end
function Array.slice(input, start, stop)
  local out = {}
  for i = start, stop do
    out[i - start + 1] = input[i]
  end
  return out
end
return Array
