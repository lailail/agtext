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

@Component
public class ContentGenerateTool implements ToolHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public ToolDefinition definition() {
    return new ToolDefinition(
        "content.generate",
        "内容生成（调用模型），返回生成文本。",
        ToolType.READ,
        false,
        20_000,
        """
        {"type":"object","properties":{"instruction":{"type":"string"},"systemPrompt":{"type":"string"},"provider":{"type":"string"},"model":{"type":"string"}},"required":["instruction"]}
        """,
        """
        {"type":"object","properties":{"provider":{"type":"string"},"model":{"type":"string"},"content":{"type":"string"}},"required":["content"]}
        """);
  }

  @Override
  public ToolResult execute(ToolContext ctx, JsonNode input) {
    String instruction =
        input == null || input.get("instruction") == null
            ? null
            : input.get("instruction").asText();
    if (instruction == null || instruction.isBlank()) {
      throw new IllegalArgumentException("instruction is required");
    }
    String system =
        input != null && input.get("systemPrompt") != null
            ? input.get("systemPrompt").asText()
            : null;
    String provider =
        input != null && input.get("provider") != null ? input.get("provider").asText() : null;
    String model = input != null && input.get("model") != null ? input.get("model").asText() : null;

    List<ChatMessage> prompt = new ArrayList<>();
    if (system != null && !system.isBlank()) {
      prompt.add(ChatMessage.system(system));
    }
    prompt.add(ChatMessage.user(instruction.trim()));

    ModelResponse r = ctx.models().chat(provider, model, prompt);
    ObjectNode data = MAPPER.createObjectNode();
    data.put("provider", r.provider());
    data.put("model", r.model());
    data.put("content", r.content());
    return new ToolResult("generated", data);
  }
}
