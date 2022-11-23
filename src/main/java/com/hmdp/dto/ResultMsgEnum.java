package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : Fu QiuJie
 * @date : 2022/11/16 18:10
 */
@AllArgsConstructor
@Getter
public enum ResultMsgEnum {
    //手机格式错误
    INVALID_PHONE_ERROR("手机号格式错误"),
    CODE_ERROR("验证码错误"),
    SHOP_NOT_EXIST("店铺不存在"),
    OUT_OF_TIME("不在秒杀时间内"),
    UNDERSTOCK("库存不足"),
    DOUBLE_ORDERING("请勿重复下单");

    private final String msg;

}
