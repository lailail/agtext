package com.agtext.task.service;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.model.service.ModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 计划拆解服务
 * 职责：
 * 1. 构造 Prompt 调用 AI 模型将目标拆解为具体步骤。
 * 2. 解析 AI 返回的 JSON 数据。
 * 3. 在 AI 响应异常或格式错误时，提供基于文本行的降级处理逻辑。
 */
@Service
public class PlanDecompositionService {
  private final ModelService modelService;
  private final ObjectMapper objectMapper;

  public PlanDecompositionService(ModelService modelService, ObjectMapper objectMapper) {
    this.modelService = modelService;
    this.objectMapper = objectMapper;
  }

  /**
   * 执行拆解逻辑
   * @param objective 目标描述
   * @param providerOverride 指定 AI 供应商（可为空，使用默认）
   * @param modelOverride 指定模型（可为空，使用默认）
   * @param maxSteps 最大拆解步数限制
   * @return 结构化的拆解结果
   */
  public DecompositionResult decompose(
          String objective, String providerOverride, String modelOverride, int maxSteps) {
    if (objective == null || objective.isBlank()) {
      throw new IllegalArgumentException("objective is required");
    }

    // 强制约束步数范围在 1-20 之间，防止 AI 输出过长导致性能问题
    int limit = Math.max(1, Math.min(20, maxSteps));

    List<ChatMessage> prompt = new ArrayList<>();
    // 系统提示词：定义 AI 的角色、约束条件及严格的输出格式
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

    try {
      // 调用模型服务
      ModelResponse resp = modelService.chat(providerOverride, modelOverride, prompt);
      String text = resp.content() == null ? "" : resp.content().trim();

      // 尝试解析 JSON
      DecompositionResult parsed = tryParseJson(text, limit);
      if (parsed != null) {
        return parsed;
      }
    } catch (Exception e) {
      // 记录异常（实际生产环境中应使用 Logger），并进入降级逻辑
    }

    // 兜底方案：如果 AI 失败，则按行切分原始文本
    return fallback(objective, limit);
  }

  /**
   * JSON 解析逻辑
   * 采用 Jackson 进行防御性解析，确保即便 JSON 结构部分缺失也能正常运行。
   */
  private DecompositionResult tryParseJson(String text, int limit) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      // 针对部分模型可能包含 Markdown 代码块标记（```json）的处理需注意，此处假设 text 已是纯 JSON
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

      return steps.isEmpty() ? null : new DecompositionResult(title, steps);
    } catch (Exception e) {
      // 解析失败返回 null，由上层触发 fallback
      return null;
    }
  }

  /**
   * 降级处理逻辑
   * 实事求是：当 AI 不可用或输出不可读时，将原始输入按行拆分为任务步骤，确保业务不中断。
   */
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
    // 若拆分后仍为空，则将整个目标作为一个单步任务
    if (steps.isEmpty()) {
      steps.add(new Step(objective.trim(), null));
    }
    return new DecompositionResult(null, steps);
  }

  // --- 领域对象定义 ---

  public record Step(String title, String description) {}

  public record DecompositionResult(String title, List<Step> steps) {}
}