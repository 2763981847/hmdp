package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Api(tags = "优惠券抢购类接口")
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    @ApiOperation("秒杀券抢购接口")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.secKillVoucher(voucherId);
    }
}
