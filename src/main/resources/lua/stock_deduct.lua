-- 库存扣减Lua脚本
-- KEYS[1]: 库存key
-- ARGV[1]: 扣减数量

-- 获取当前库存
local stock = tonumber(redis.call('get', KEYS[1]))

-- 如果库存不存在，返回-1
if stock == nil then
    return -1
end

-- 如果库存不足，返回-2
if stock < tonumber(ARGV[1]) then
    return -2
end

-- 扣减库存
redis.call('decrby', KEYS[1], ARGV[1])

-- 返回扣减后的库存
return stock - tonumber(ARGV[1]) 