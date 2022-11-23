package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result listHotBlogs(Integer current) {
        // 根据用户查询
        Page<Blog> page = this.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(this::packageBlog);
        return Result.ok(records);
    }


    @Override
    public Result getBlogById(long id) {
        //查询到博文信息
        Blog blog = this.getById(id);
        //整合数据
        packageBlog(blog);
        //返回数据
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        //获取当前用户
        String userId = UserHolder.getUser().getId().toString();
        //判断用户是要点赞还是取消点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        boolean isLiked = stringRedisTemplate.opsForZSet().score(key, userId) != null;
        boolean isUpdated = false;
        if (!isLiked) {
            //如果还没点过赞了,数据库点赞数加一
            isUpdated = this.lambdaUpdate().setSql("liked=liked+1").eq(Blog::getId, id).update();
            //将用户id加入已点赞集合
            if (isUpdated) {
                stringRedisTemplate.opsForZSet().add(key, userId, System.currentTimeMillis());
            }
        } else {
            //如果已经点过赞了,数据库点赞数减一
            isUpdated = this.lambdaUpdate().setSql("liked=liked-1").eq(Blog::getId, id).update();
            //将用户id从已点赞集合中移除
            if (isUpdated) {
                stringRedisTemplate.opsForZSet().remove(key, userId);
            }
        }
        return isUpdated ? Result.ok() : Result.fail("服务异常");
    }

    @Override
    public Result listBlogLikes(Long id) {
        //查询已点赞的用户
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> ids = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (ids == null || ids.isEmpty()) {
            //没有点赞用户，返回空列表
            return Result.ok(Collections.emptyList());
        }
        //解析出用户id列表
        List<Long> idList = ids.stream().map(Long::valueOf).collect(Collectors.toList());
        //查询出用户信息
        List<User> userList = userService.listByIds(idList);
        //用户信息脱敏
        List<UserDTO> userDTOList = userList.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOList);
    }

    private void packageBlog(Blog blog) {
        //整合用户信息
        User user = userService.getById(blog.getUserId());
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        //判断当前用户是否已点赞
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            //当前用户为未登录用户，直接返回
            return;
        }
        Long userId = userDTO.getId();
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Boolean isLiked = stringRedisTemplate.opsForZSet().score(key, userId.toString()) != null;
        blog.setIsLike(BooleanUtil.isTrue(isLiked));
    }
}
