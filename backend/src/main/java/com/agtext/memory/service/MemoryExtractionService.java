package com.agtext.memory.service;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.service.ModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 记忆提取服务：
 * 利用 LLM 的语义理解能力，自动捕获对话中的关键事实、偏好或长期目标。
 */
@Service
public class MemoryExtractionService {
  private final ModelService modelService;
  private final ObjectMapper objectMapper;
  private final MemorySettingsProperties props; // 包含开关、模型名称、最大条数等配置

  public MemoryExtractionService(
          ModelService modelService, ObjectMapper objectMapper, MemorySettingsProperties props) {
    this.modelService = modelService;
    this.objectMapper = objectMapper;
    this.props = props;
  }

  /**
   * 从单轮对话（用户消息 + 助手回复）中提取记忆候选。
   * @return 提取出的候选记忆列表，若未开启功能或提取失败则返回空列表。
   */
  public List<Candidate> extractCandidates(String userMessage, String assistantMessage) {
    // 1. 检查全局配置开关
    if (!props.extractionEnabled()) {
      return List.of();
    }

    String provider = props.extractionProvider(); // 供应商，如 openai, anthropic
    String model = props.extractionModel();       // 模型，如 gpt-4o

    // 2. 构建提示词（Prompt）
    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(
            ChatMessage.system(
                    """
                    你是“长期记忆候选提取器”。请从对话中提取适合长期保存、对未来有用且相对稳定的信息（偏好、身份、重要约束、长期目标）。
        
                    规则：
                    1) 不要提取临时信息（例如：今天的天气、一次性验证码、当前这次问题的中间产物）。
                    2) 不要提取敏感信息（例如：密码、身份证号、银行卡、API Key）。
                    3) 最多输出 %d 条。
                    4) 只输出严格 JSON，不要输出其它文字。
        
                    输出格式：
                    {"items":[{"title":"可选","content":"必填","reason":"可选"}]}
                    """
                            .formatted(props.maxCandidatesPerTurn())));

    // 将当前对话上下文输入给模型
    prompt.add(
            ChatMessage.user(
                    "用户消息：\n"
                            + (userMessage == null ? "" : userMessage)
                            + "\n\n助手回复：\n"
                            + (assistantMessage == null ? "" : assistantMessage)));

    // 3. 调用 LLM 进行推理
    ModelResponse resp = modelService.chat(provider, model, prompt);
    String text = resp.content() == null ? "" : resp.content().trim();
    if (text.isBlank()) {
      return List.of();
    }

    // 4. 解析模型返回的 JSON 结果
    try {
      JsonNode node = objectMapper.readTree(text);
      JsonNode itemsNode = node.get("items");
      if (itemsNode == null || !itemsNode.isArray()) {
        return List.of();
      }

      List<Candidate> out = new ArrayList<>();
      for (JsonNode it : itemsNode) {
        // 二次防御：确保不超过配置的最大条数
        if (out.size() >= props.maxCandidatesPerTurn()) {
          break;
        }

        // 提取并校验必填项 content
        String content = it.hasNonNull("content") ? it.get("content").asText() : null;
        if (content == null || content.isBlank()) {
          continue;
        }

        String title = it.hasNonNull("title") ? it.get("title").asText() : null;
        String reason = it.hasNonNull("reason") ? it.get("reason").asText() : null;

        out.add(new Candidate(title, content, reason));
      }
      return out;
    } catch (Exception e) {
      // 捕获 JSON 解析异常或字段缺失异常，确保服务鲁棒性
      // 实事求是地说：LLM 的输出不总是完美的 JSON，此处必须进行健壮性处理
      return List.of();
    }
  }

  /**
   * 内部中间对象：提取出的原始候选
   */
  public record Candidate(String title, String content, String reason) {}
}