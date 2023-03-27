**类大众点评 APP**

- 项目简介：基于 Spring Boot + Redis 的店铺点评 APP，实现了找店铺 => 写点评 => 看热评 => 点赞关注 => 关注 Feed 流的完整业务流程。
- 项目细节：
  1. 短信登录及登录验证:使用Redis实现分布式Session，解决了共享session问题，并且使用Hash结构代替String来保存登录用户信息，可以针对单个字段做CRUD且内存占用更小。
  2. 为方便其他业务后续使用缓存，使用泛型＋函数式编程实现了通用缓存访问静态方法，并解决了缓存穿透、缓存雪崩、缓存击穿等问题，工具类已上传至maven中央仓库：https://central.sonatype.com/artifact/cn.autumnclouds.redis/redis-client/0.0.3。
  3. 利用工具类对高频访问店铺等热点数据进行缓存，降低DB压力同时提升90%的数据查询性能。
  4. 优惠券秒杀:使用redission+ Lua脚本实现库存预检，并通过Stream队列实现订单的异步创建，解决了超卖问题、实现一人一单。经jmeter模拟测试，相比传统数据库，秒杀性能提高了60%。
  5. 在订单服务中使用redis实现了分布式的全局唯一ID生成器，以此来代替数据库自增生成ID，保证了订单数据的安全性，同时也满足了订单ID的递增性，便于数据库建立索引。
  6. 使用Redis的Geo + Hash数据结构分类存储附近商户，并使用Geo Search命令实现高性能商户查询及按距离排序。
  7. 使用Redis Set数据结构实现用户关注、共同关注功能(交集)，实测相对于DB查询性能提升约70%。
  8. 使用Redis BitMap 将签到情况与日期进行一对一映射，实现用户连续签到统计功能，相对于传统关系库的多对多关系存储，提升查询性能的同时节约90%+的内存。
  9. 在系统用户量不大的前提下，基于推模式实现关注Feed流，保证了新点评消息的及时可达，并减少用户访问的等待时间。
  10. 使用HyperLogLog实现了UV统计，节省大量内存（单个HLL始终小于16kb）的同时，只引入了小于0.81%的误差。
