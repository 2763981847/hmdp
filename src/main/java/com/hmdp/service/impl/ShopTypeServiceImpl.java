package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result listShopType() {
        //先尝试从缓存中拿到信息
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        String listJson = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(listJson)) {
            //成功拿到信息则直接返回
            List<ShopType> shopTypeList = JSONUtil.toList(listJson, ShopType.class);
            //返回前刷新其TTL
            stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
            return Result.ok(shopTypeList);
        }
        //没有拿到信息则查询数据库
        List<ShopType> shopTypeList = this.list();
        //将其保存至缓存并返回
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypeList), RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        //返回
        return Result.ok(shopTypeList);
    }
}
