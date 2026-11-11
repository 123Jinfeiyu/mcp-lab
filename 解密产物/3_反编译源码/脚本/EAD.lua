require("lockbox.init")
local Array = require("lockbox.util.array")
local Stream = require("lockbox.util.stream")
local CBCMode = require("lockbox.cipher.mode.cbc")
local ZeroPadding = require("lockbox.padding.zero")
local AES256Cipher = require("lockbox.cipher.aes256")
function Encrypt(str, key, iv)
  local v = {
    cipher = CBCMode.Cipher,
    key = Array.fromString(key),
    iv = Array.fromString(iv),
    plaintext = Array.fromString(str),
    padding = ZeroPadding
  }
  local cipher = v.cipher().setKey(v.key).setBlockCipher(AES256Cipher).setPadding(v.padding)
  local cipherOutput = cipher.init().update(Stream.fromArray(v.iv)).update(Stream.fromArray(v.plaintext)).finish().asHex()
  return cipherOutput
end
function Decrypt(hex, key, iv)
  local v = {
    decipher = CBCMode.Decipher,
    key = Array.fromString(key),
    iv = Array.fromString(iv),
    ciphertext = Array.fromHex(hex),
    padding = ZeroPadding
  }
  local decipher = v.decipher().setKey(v.key).setBlockCipher(AES256Cipher).setPadding(v.padding)
  local plainOutput = decipher.init().update(Stream.fromArray(v.iv)).update(Stream.fromArray(v.ciphertext)).finish().asHex()
  return Array.toString(Array.fromHex(plainOutput))
end
