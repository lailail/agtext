package com.agtext.agent.api;

import com.agtext.agent.service.AgentService;
import com.agtext.model.domain.ModelResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 暴露的 REST 接口层，负责处理外部请求的路由与响应封装。
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

  // 注入业务逻辑层，处理具体的 Agent 调用策略
  private final AgentService agents;

  public AgentController(AgentService agents) {
    this.agents = agents;
  }

  /**
   * 获取系统当前支持的所有 Agent 角色及其元数据。
   * @return 包含角色名称、描述等信息的列表
   */
  @GetMapping("/roles")
  public List<AgentService.RoleInfo> roles() {
    return agents.roles();
  }

  /**
   * 执行 Agent 任务的核心接口。
   * 将外部请求参数转发至 Service 层，并将底层模型返回的 ModelResponse 转换为前端所需的 DTO。
   * * @param req 包含角色、输入内容、供应商及模型规格的请求体
   * @return 包含模型执行结果及当前时间戳的响应对象
   */
  @PostMapping("/run")
  public AgentRunResponse run(@RequestBody AgentRunRequest req) {
    // 调用 Service 层执行推理，解耦了控制器与具体模型驱动（如 OpenAI, Claude 等）的实现细节
    ModelResponse r = agents.run(req.role(), req.input(), req.provider(), req.model());

    // 构造返回对象，Instant.now() 记录了 API 完成处理的时间点
    return new AgentRunResponse(r.provider(), r.model(), r.content(), Instant.now());
  }

  /**
   * 客户端请求载体
   * @param role     指定的 Agent 角色标识
   * @param input    用户的输入文本
   * @param provider 模型供应商标识（如 "openai", "anthropic"）
   * @param model    具体的模型版本号
   */
  public record AgentRunRequest(String role, String input, String provider, String model) {}

  /**
   * 接口输出载体
   * @param provider 实际响应的供应商
   * @param model    实际调用的模型名称
   * @param content  模型生成的文本内容
   * @param createdAt 响应生成的时刻
   */
  public record AgentRunResponse(
          String provider, String model, String content, Instant createdAt) {}
}