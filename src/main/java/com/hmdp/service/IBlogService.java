package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
    /**
     * 查询热门探店博文
     * @param current 当前页码
     * @return 查询到的该页数据
     */
    Result listHotBlogs(Integer current);

    /**
     * 根据id获取博文详情
     * @param id 博文id
     * @return 博文详情
     */
    Result getBlogById(long id);

    /**
     * 点赞博文功能
     * @param id 博文id
     * @return 返回信息
     */
    Result likeBlog(Long id);

    /**
     * 查询博文的点赞用户
     * @param id 博文id
     * @return 点赞用户
     */
    Result listBlogLikes(Long id);
}
