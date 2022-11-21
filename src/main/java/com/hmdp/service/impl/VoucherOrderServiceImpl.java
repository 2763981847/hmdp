package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.ResultMsgEnum;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdGenerator;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.lock.ILock;
import com.hmdp.utils.lock.SimpleRedisLock;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result secKillVoucher(Long voucherId) {
        //拿到优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断是否在秒杀时间内
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime()) || now.isAfter(voucher.getEndTime())) {
            //不在时间内，返回错误。
            return Result.fail(ResultMsgEnum.OUT_OF_TIME);
        }
        //在秒杀时间内则判断库存是否足够
        int stock = voucher.getStock();
        if (stock <= 0) {
            //库存不足，返回错误
            return Result.fail(ResultMsgEnum.UNDERSTOCK);
        }
        //库存有剩余则尝试创建订单
        //对每个不同用户分别加锁;
        Long userId = UserHolder.getUser().getId();
        ILock lock = new SimpleRedisLock(stringRedisTemplate, "voucherOrder:" + userId);
        boolean isLocked = lock.tryLock(10);
        if (!isLocked) {
            //获取锁失败，返回错误
            return Result.fail("请勿重复抢购");
        }
        try {
            //拿到当前代理对象
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            //通过代理对象调用方法，确保事务生效
            return proxy.tryCreateOrder(voucherId);
        } finally {
            lock.unLock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result tryCreateOrder(Long voucherId) {
        //判断用户是否已抢过该券
        Long userId = UserHolder.getUser().getId();
        int count = this.lambdaQuery().eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getVoucherId, voucherId).count();
        if (count > 0) {
            //已下过单，返回错误
            return Result.fail("请勿重复抢购");
        }
        //满足下单条件则扣减库存并创建订单
        boolean success = seckillVoucherService.lambdaUpdate()
                .eq(SeckillVoucher::getVoucherId, voucherId)
                .gt(SeckillVoucher::getStock, 0)
                .setSql("stock=stock-1").update();
        if (!success) {
            // 库存大于零才能更新成功,更新失败则返回错误
            return Result.fail(ResultMsgEnum.UNDERSTOCK);
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //设置秒杀券id
        voucherOrder.setVoucherId(voucherId);
        //设置当前用户id
        voucherOrder.setUserId(userId);
        //设置订单id
        long id = redisIdGenerator.nextId("order:id");
        voucherOrder.setId(id);
        //保存订单信息
        this.save(voucherOrder);
        //返回订单id
        return Result.ok(id);
    }

}
