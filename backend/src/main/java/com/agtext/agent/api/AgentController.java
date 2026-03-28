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

@RestController
@RequestMapping("/api/agents")
public class AgentController {
  private final AgentService agents;

  public AgentController(AgentService agents) {
    this.agents = agents;
  }

  @GetMapping("/roles")
  public List<AgentService.RoleInfo> roles() {
    return agents.roles();
  }

  @PostMapping("/run")
  public AgentRunResponse run(@RequestBody AgentRunRequest req) {
    ModelResponse r = agents.run(req.role(), req.input(), req.provider(), req.model());
    return new AgentRunResponse(r.provider(), r.model(), r.content(), Instant.now());
  }

  public record AgentRunRequest(String role, String input, String provider, String model) {}

  public record AgentRunResponse(
      String provider, String model, String content, Instant createdAt) {}
}
