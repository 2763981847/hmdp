package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.ResultMsgEnum;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdGenerator;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.hash.ObjectHashMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdGenerator redisIdGenerator;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR =
            new ThreadPoolExecutor(1, 1, 0, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), runnable -> new Thread(runnable, "生成订单线程"));

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        private static final String QUEUE_NAME = "stream.orders";

        @Override
        public void run() {
            while (true) {
                try {
                    //尝试获取消息队列中的订单信息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(QUEUE_NAME, ReadOffset.lastConsumed())
                    );
                    //判断是否获取到消息
                    if (list == null || list.isEmpty()) {
                        //没有获取到消息，进入下一次循环
                        continue;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //创建订单
                    tryCreateOrder(voucherOrder);
                    //ACK确认
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
                } catch (Exception e) {
                    log.error("订单处理异常", e);
                    //处理异常消息
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    //尝试获取消息队列中的异常订单信息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(QUEUE_NAME, ReadOffset.from("0"))
                    );
                    //判断是否获取到异常消息
                    if (list == null || list.isEmpty()) {
                        //如果为空，则说明没有异常信息需要处理，结束循环
                        break;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //创建订单
                    tryCreateOrder(voucherOrder);
                    //ACK确认
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pendding订单异常", e);
                    try {
                        Thread.sleep(20);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public Result secKillVoucher(Long voucherId) {
        //执行lua脚本
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdGenerator.nextId("order:id");
        Long result = stringRedisTemplate
                .execute(SECKILL_SCRIPT
                        , Collections.emptyList()
                        , voucherId.toString(), userId.toString(), String.valueOf(orderId));
        int r = result.intValue();
        //判断结果是否成功下单
        if (r != 0) {
            //未成功下单，返回错误
            return Result.fail(r == 1 ? ResultMsgEnum.UNDERSTOCK : ResultMsgEnum.DOUBLE_ORDERING);
        }
        //成功则返回订单id
        return Result.ok(orderId);
    }

//    @Override
//    public Result secKillVoucher(Long voucherId) {
//        //拿到优惠券信息
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        //判断是否在秒杀时间内
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isBefore(voucher.getBeginTime()) || now.isAfter(voucher.getEndTime())) {
//            //不在时间内，返回错误。
//            return Result.fail(ResultMsgEnum.OUT_OF_TIME);
//        }
//        //在秒杀时间内则判断库存是否足够
//        int stock = voucher.getStock();
//        if (stock <= 0) {
//            //库存不足，返回错误
//            return Result.fail(ResultMsgEnum.UNDERSTOCK);
//        }
//        //库存有剩余则尝试创建订单
//        //对每个不同用户分别加锁
//        Long userId = UserHolder.getUser().getId();
//        //   ILock lock = new SimpleRedisLock(stringRedisTemplate, "voucherOrder:" + userId);
//        RLock lock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
//        boolean isLocked = lock.tryLock();
//        if (!isLocked) {
//            //获取锁失败，返回错误
//            return Result.fail("请勿重复抢购");
//        }
//        try {
//            //拿到当前代理对象
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            //通过代理对象调用方法，确保事务生效
//            return proxy.tryCreateOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//    }

    @Transactional(rollbackFor = Exception.class)
    public void tryCreateOrder(VoucherOrder voucherOrder) {
        //判断用户是否已抢过该券
        Long voucherId = voucherOrder.getVoucherId();
        Long userId = voucherOrder.getUserId();
        // 创建锁对象
        RLock redisLock = redissonClient.getLock(RedisConstants.LOCK_ORDER_KEY + userId);
        // 尝试获取锁
        boolean isLock = redisLock.tryLock();
        // 判断
        if (!isLock) {
            // 获取锁失败，直接返回失败或者重试
            log.error("用户重复下单！");
            return;
        }
        try {
            int count = this.lambdaQuery().eq(VoucherOrder::getUserId, userId)
                    .eq(VoucherOrder::getVoucherId, voucherId).count();
            if (count > 0) {
                //已下过单，返回
                log.error("用户重复下单");
                return;
            }
            //满足下单条件则扣减库存并创建订单
            boolean success = seckillVoucherService.lambdaUpdate()
                    .eq(SeckillVoucher::getVoucherId, voucherId)
                    .gt(SeckillVoucher::getStock, 0)
                    .setSql("stock=stock-1").update();
            if (!success) {
                // 库存大于零才能更新成功,更新失败则返回
                log.error("库存不足");
                return;
            }
            //保存订单信息
            this.save(voucherOrder);
        } finally {
            redisLock.unlock();
        }
    }

}
