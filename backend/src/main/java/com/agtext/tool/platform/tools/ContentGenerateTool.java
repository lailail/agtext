package com.agtext.tool.platform.tools;

import com.agtext.model.domain.ChatMessage;
import com.agtext.model.domain.ModelResponse;
import com.agtext.tool.platform.domain.ToolDefinition;
import com.agtext.tool.platform.domain.ToolResult;
import com.agtext.tool.platform.domain.ToolType;
import com.agtext.tool.platform.service.ToolContext;
import com.agtext.tool.platform.service.ToolHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 内容生成工具
 * 实现 ToolHandler 接口，用于封装大语言模型（LLM）的调用逻辑
 */
@Component
public class ContentGenerateTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * 定义工具元数据
   * 设置为 READ 类型（无副作用），超时时间为 20 秒
   * 包含详细的 JSON Schema 用于约束输入（指令、系统提示词、供应商、模型名）
   */
  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
            "content.generate",
            "内容生成（调用模型），返回生成文本。",
            ToolType.READ,
            false, // 无需人工二次确认
            20_000, // 超时时间 20s
            """
            {
              "type":"object",
              "properties":{
                "instruction":{"type":"string", "description": "需要模型执行的具体指令"},
                "systemPrompt":{"type":"string", "description": "系统提示词"},
                "provider":{"type":"string", "description": "模型供应商，如 openai, anthropic"},
                "model":{"type":"string", "description": "具体模型名称"}
              },
              "required":["instruction"]
            }
            """,
            """
            {
              "type":"object",
              "properties":{
                "provider":{"type":"string"},
                "model":{"type":"string"},
                "content":{"type":"string"}
              },
              "required":["content"]
            }
            """);
  }

  /**
   * 执行模型调用逻辑
   */
  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) {
    // 1. 参数提取与必填项校验
    String instruction =
            input == null || input.get("instruction") == null
                    ? null
                    : input.get("instruction").asText();
    if (instruction == null || instruction.isBlank()) {
      throw new IllegalArgumentException("instruction is required");
    }

    // 2. 提取可选参数
    String system =
            input != null && input.get("systemPrompt") != null
                    ? input.get("systemPrompt").asText()
                    : null;
    String provider =
            input != null && input.get("provider") != null ? input.get("provider").asText() : null;
    String model = input != null && input.get("model") != null ? input.get("model").asText() : null;

    // 3. 构建模型对话上下文
    List<ChatMessage> prompt = new ArrayList<>();
    if (system != null && !system.isBlank()) {
      prompt.add(ChatMessage.system(system));
    }
    prompt.add(ChatMessage.user(instruction.trim()));

    // 4. 通过上下文中的 ModelService 发起实际调用
    // 注意：此处是同步调用，受 ToolExecutionService 中的超时机制管控
    ModelResponse r = ctx.models().chat(provider, model, prompt);

    // 5. 封装结构化返回结果
    ObjectNode data = MAPPER.createObjectNode();
    data.put("provider", r.provider());
    data.put("model", r.model());
    data.put("content", r.content());

    return new ToolResult("generated", data);
  }
}