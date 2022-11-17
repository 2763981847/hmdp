package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {
    /**
     * 根据商户id拿到对应商户信息
     * @param id 商户id
     * @return 商户信息
     */
    Result getShopById(Long id);

    /**
     * 更新商户信息
     * @param shop 商户信息
     * @return 返回结果
     */
    Result updateShop(Shop shop);
}
