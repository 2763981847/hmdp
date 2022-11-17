package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import lombok.AllArgsConstructor;
import org.aopalliance.intercept.Interceptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @author : Fu QiuJie
 * @date : 2022/11/16 20:01
 */
@AllArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从请求头中拿到token
        String token = request.getHeader("authorization");
        if (StringUtils.isEmpty(token)) {
            //没有token返回401（用户未授权）
            response.setStatus(401);
            return false;
        }
        //有token，尝试根据token拿到redis中的用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        if (userMap.isEmpty()) {
            //没有用户信息返回401（用户未授权）并清除token
            request.removeAttribute("authorization");
            response.setStatus(401);
            return false;
        }
        //拿到了用户信息，将其转为userDTO对象
        UserDTO userDTO = new UserDTO();
        BeanUtil.fillBeanWithMap(userMap, userDTO, false);
        //保存用户信息到ThreadLocal中
        UserHolder.saveUser(userDTO);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户信息
        UserHolder.removeUser();
    }
}
