package com.agtext.model.provider.mock;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.provider.ChatModelProvider;
import java.util.List;

/**
 * 模拟聊天模型提供者：
 * 不进行真实的联网 API 调用，而是根据输入通过简单逻辑立即返回预定义的响应。
 * 严肃性提示：该类仅用于测试环境，严禁在生产环境的业务流程中激活。
 */
public class MockChatModelProvider implements ChatModelProvider {

  /**
   * 供应商标识：
   * 返回 "mock"，用于 ModelRegistry 识别并挂载此实例。
   */
  @Override
  public String name() {
    return "mock";
  }

  /**
   * 模拟对话逻辑：
   * 行为：寻找对话历史中最后一条用户（user）消息，并将其内容前缀加上 "mock:" 后返回。
   * 价值：验证上层业务逻辑（如提示词构建、消息流转、响应解析）是否能正确处理 ModelResponse 对象。
   */
  @Override
  public ModelResponse chat(String model, List<ChatMessage> messages) {
    // 1. 从消息流中筛选出 role 为 "user" 的消息
    // 2. 获取最后一条（reduce 逻辑取最后元素）
    // 3. 提取内容，若无则默认为空字符串
    String lastUser =
            messages.stream()
                    .filter(m -> "user".equals(m.role()))
                    .reduce((a, b) -> b)
                    .map(ChatMessage::content)
                    .orElse("");

    // 4. 构造模拟响应，provider 标记为 "mock"
    return new ModelResponse("mock", model, "mock:" + lastUser);
  }
}