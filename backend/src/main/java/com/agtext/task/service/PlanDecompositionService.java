package com.agtext.task.service;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.service.ModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlanDecompositionService {
  private final ModelService modelService;
  private final ObjectMapper objectMapper;

  public PlanDecompositionService(ModelService modelService, ObjectMapper objectMapper) {
    this.modelService = modelService;
    this.objectMapper = objectMapper;
  }

  public DecompositionResult decompose(
      String objective, String providerOverride, String modelOverride, int maxSteps) {
    if (objective == null || objective.isBlank()) {
      throw new IllegalArgumentException("objective is required");
    }
    int limit = Math.max(1, Math.min(20, maxSteps));

    List<ChatMessage> prompt = new ArrayList<>();
    prompt.add(
        ChatMessage.system(
            """
            你是“计划拆解器”。将目标拆成可执行的步骤（每步 1 句），不要写解释。
            约束：
            - 最多 %d 步
            - 只输出严格 JSON，不要输出其它文字

            输出格式：
            {"title":"计划标题","steps":[{"title":"步骤标题","description":"可选"}]}
            """
                .formatted(limit)));
    prompt.add(ChatMessage.user(objective.trim()));

    ModelResponse resp = modelService.chat(providerOverride, modelOverride, prompt);
    String text = resp.content() == null ? "" : resp.content().trim();
    DecompositionResult parsed = tryParseJson(text, limit);
    if (parsed != null) {
      return parsed;
    }
    return fallback(objective, limit);
  }

  private DecompositionResult tryParseJson(String text, int limit) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(text);
      String title = node.hasNonNull("title") ? node.get("title").asText() : null;
      JsonNode stepsNode = node.get("steps");
      if (stepsNode == null || !stepsNode.isArray()) {
        return null;
      }
      List<Step> steps = new ArrayList<>();
      for (JsonNode s : stepsNode) {
        if (steps.size() >= limit) {
          break;
        }
        String st = s.hasNonNull("title") ? s.get("title").asText() : null;
        if (st == null || st.isBlank()) {
          continue;
        }
        String desc = s.hasNonNull("description") ? s.get("description").asText() : null;
        steps.add(new Step(st, desc));
      }
      if (steps.isEmpty()) {
        return null;
      }
      return new DecompositionResult(title, steps);
    } catch (Exception e) {
      return null;
    }
  }

  private DecompositionResult fallback(String objective, int limit) {
    String[] lines = objective.trim().split("\\r?\\n");
    List<Step> steps = new ArrayList<>();
    for (String line : lines) {
      if (steps.size() >= limit) {
        break;
      }
      String t = line.trim();
      if (!t.isBlank()) {
        steps.add(new Step(t, null));
      }
    }
    if (steps.isEmpty()) {
      steps.add(new Step(objective.trim(), null));
    }
    return new DecompositionResult(null, steps);
  }

  public record Step(String title, String description) {}

  public record DecompositionResult(String title, List<Step> steps) {}
}
