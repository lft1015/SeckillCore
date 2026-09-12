-- KEYS[1] = seckill:stock:{activityId}  库存 Key
-- ARGV[1] = 扣减数量（通常为 1）
-- 返回值：1=成功，0=库存不足，-1=Key 不存在

local stockKey = KEYS[1]
local deductNum = tonumber(ARGV[1])

if redis.call('EXISTS', stockKey) == 0 then
    return -1
end

local currentStock = tonumber(redis.call('GET', stockKey))
if currentStock == nil or currentStock < deductNum then
    return 0
end

redis.call('DECRBY', stockKey, deductNum)
return 1