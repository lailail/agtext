package com.agtext.common.api;

import java.util.List;

/**
 * 分页响应包装类：用于标准化 API 的分页数据返回格式。
 * * @param <T> 分页数据项的泛型类型
 * @param items 当前页的数据内容列表
 * @param page 当前请求的页码
 * @param pageSize 每页允许的最大记录数
 * @param total 数据库中符合筛选条件的记录总数（用于前端计算总页数及展示分页组件）
 */
public record PageResponse<T>(List<T> items, int page, int pageSize, long total) {}