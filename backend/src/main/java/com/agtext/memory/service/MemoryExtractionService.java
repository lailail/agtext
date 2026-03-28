package com.agtext.memory.service;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.service.ModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemoryExtractionService {
  private final ModelService modelService;
  private final ObjectMapper objectMapper;
  private final MemorySettingsProperties props;

  public MemoryExtractionService(
      ModelService modelService, ObjectMapper objectMapper, MemorySettingsProperties props) {
    this.modelService = modelService;
    this.objectMapper = objectMapper;
    this.props = props;
  }

  public List<Candidate> extractCandidates(String userMessage, String assistantMessage) {
    if (!props.extractionEnabled()) {
      return List.of();
    }
    String provider = props.extractionProvider();
    String model = props.extractionModel();

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
    prompt.add(
        ChatMessage.user(
            "用户消息：\n"
                + (userMessage == null ? "" : userMessage)
                + "\n\n助手回复：\n"
                + (assistantMessage == null ? "" : assistantMessage)));

    ModelResponse resp = modelService.chat(provider, model, prompt);
    String text = resp.content() == null ? "" : resp.content().trim();
    if (text.isBlank()) {
      return List.of();
    }

    try {
      JsonNode node = objectMapper.readTree(text);
      JsonNode itemsNode = node.get("items");
      if (itemsNode == null || !itemsNode.isArray()) {
        return List.of();
      }
      List<Candidate> out = new ArrayList<>();
      for (JsonNode it : itemsNode) {
        if (out.size() >= props.maxCandidatesPerTurn()) {
          break;
        }
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
      return List.of();
    }
  }

  public record Candidate(String title, String content, String reason) {}
}
