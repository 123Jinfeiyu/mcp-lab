local Queue = function()
  local queue = {}
  local tail = 0
  local head = 0
  local public = {}
  function public.push(obj)
    queue[head] = obj
    head = head + 1
    return
  end
  function public.pop()
    if tail < head then
      local obj = queue[tail]
      queue[tail] = nil
      tail = tail + 1
      return obj
    else
      return nil
    end
  end
  function public.size()
    return head - tail
  end
  function public.getHead()
    return head
  end
  function public.getTail()
    return tail
  end
  function public.reset()
    queue = {}
    head = 0
    tail = 0
  end
  return public
end
return Queue
