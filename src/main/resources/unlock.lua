--比较当前线程与锁持有线程
if (redis.call("get", KEYS[1]) == ARGV[1]) then
    --锁持有线程为当前线程则释放锁
    return redis.call("del", KEYS[1])
end
return 0;
