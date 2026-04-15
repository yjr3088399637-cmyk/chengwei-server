local voucherId = ARGV[1]
local userId = ARGV[2]
local id = ARGV[3]

local stockKey = 'seckill:stock:voucher:' .. voucherId
local orderKey = 'seckill:order:voucher:' .. voucherId

local stock = tonumber(redis.call('get', stockKey))
if(stock == nil or stock <= 0) then
    return 1
end

if(redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
redis.call('xadd', 'stream.orders', '*', 'voucherId', voucherId, 'userId', userId, 'id', id)
return 0
