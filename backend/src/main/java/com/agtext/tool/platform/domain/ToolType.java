package com.agtext.tool.platform.domain;

/**
 * 工具操作类型枚举
 * 用于区分工具对系统数据的副作用（Side Effects），决定拦截与审计的强度
 */
public enum ToolType {
  /**
   * 只读工具：仅查询或检索数据（如获取任务列表、查询气温、读取文档）
   * 特点：无副作用，通常不需要二次确认（Confirmation），审计级别较低
   */
  READ,

  /**
   * 写入工具：涉及数据的创建、修改或删除（如更新任务状态、删除目标、发送邮件）
   * 特点：具有副作用，通常受 WriteGuardInterceptor 拦截，必须校验授权状态
   */
  WRITE
}