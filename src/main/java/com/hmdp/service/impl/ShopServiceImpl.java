package com.hmdp.service.impl;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.ResultMsgEnum;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getShopById(Long id) {
        //先尝试从redis缓存中拿到商户信息
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(shopJson)) {
            //成功拿到则将其转为shop对象并返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //返回前刷新TTL
            stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        }
        //缓存中没有则查询数据库
        Shop shop = this.getById(id);
        if (shop == null) {
            //不存在则返回错误
            return Result.fail(ResultMsgEnum.SHOP_NOT_EXIST);
        }
        //存在则将其保存至缓存并返回
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //返回结果
        return Result.ok(shop);
    }
}
