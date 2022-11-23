package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    /**
     * 抢购秒杀券功能
     *
     * @param voucherId 秒杀券id
     * @return 生成的订单id
     */
    Result secKillVoucher(Long voucherId);

    /**
     * 尝试创建订单
     *
     * @param voucherOrder 订单信息
     */
    void tryCreateOrder(VoucherOrder voucherOrder);
}
