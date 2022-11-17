package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.ResultMsgEnum;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {

            //如果不是有效手机号，则返回错误
            return Result.fail(ResultMsgEnum.INVALID_PHONE_ERROR);
        }

        //手机号有效，则生成二维码
        String code = RandomUtil.randomNumbers(6);

        //将验证码存入redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //todo 调用短信发送api发送验证码
        log.info("已发送验证码：{}", code);

        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //如果不是有效手机号，则返回错误
            return Result.fail(ResultMsgEnum.INVALID_PHONE_ERROR);
        }
        //检查验证码是否一致
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (StringUtils.isEmpty(code) || !code.equals(loginForm.getCode())) {
            //不一致，则返回错误
            return Result.fail(ResultMsgEnum.CODE_ERROR);
        }
        //一致，则查询用户是否是第一次登录
        User user = getUserByPhone(phone);
        if (user == null) {
            //用户第一次登录则先进行注册
            user = register(phone);
        }
        //将用户信息保存到redis中
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user, userDTO);
        //生成一个随机UUID拼接成key
        String token = UUID.fastUUID().toString(true);
        String key = LOGIN_USER_KEY + token;
        //将userDTO对象转为map
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().ignoreNullValue()
                        .setFieldValueEditor((name, value) -> value.toString()));
        stringRedisTemplate.opsForHash().putAll(key, map);
        //设置过期时间
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        //令验证码失效
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);
        //返回ok
        return Result.ok(token);
    }

    private User getUserByPhone(String phone) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        return this.getOne(queryWrapper);
    }

    private User register(String phone) {
        //初始化用户信息
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        //保存用户
        this.save(user);
        //返回用户
        return user;
    }
}
