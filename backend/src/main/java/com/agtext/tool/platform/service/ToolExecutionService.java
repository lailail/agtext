package com.agtext.tool.platform.service;

import com.agtext.common.api.NotFoundException;
import com.agtext.common.ids.IdCodec;
import com.agtext.model.service.ModelService;
import com.agtext.settings.service.AppSettingsService;
import com.agtext.tool.platform.domain.ToolDefinition;
import com.agtext.tool.platform.domain.ToolResult;
import com.agtext.tool.service.ConfirmationService;
import com.agtext.tool.service.ExecutionRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class ToolExecutionService {
  private static final String CONF_PREFIX = "cnf_";

  private final ToolRegistry registry;
  private final ConfirmationService confirmations;
  private final ExecutionRecordService executions;
  private final ToolSecurityProperties security;
  private final AppSettingsService appSettings;
  private final HttpClient http;
  private final ModelService models;

  public ToolExecutionService(
      ToolRegistry registry,
      ConfirmationService confirmations,
      ExecutionRecordService executions,
      ToolSecurityProperties security,
      AppSettingsService appSettings,
      HttpClient http,
      ModelService models) {
    this.registry = registry;
    this.confirmations = confirmations;
    this.executions = executions;
    this.security = security;
    this.appSettings = appSettings;
    this.http = http;
    this.models = models;
  }

  public ExecuteResult execute(String actor, ExecuteRequest req) {
    if (req == null || req.toolName() == null || req.toolName().isBlank()) {
      throw new IllegalArgumentException("toolName is required");
    }

    ToolHandler handler =
        registry
            .find(req.toolName())
            .orElseThrow(() -> new NotFoundException("TOOL_NOT_FOUND", "Tool not found"));
    ToolDefinition def = handler.definition();

    if (!isToolEnabled(def.name())) {
      return ExecuteResult.failed("TOOL_DISABLED", "Tool disabled: " + def.name());
    }

    if (def.requiresConfirmation()) {
      if (req.confirmationId() == null || req.confirmationId().isBlank()) {
        String summary = "Tool execute: " + def.name();
        String payload = req.input() == null ? null : req.input().toString();
        var item = confirmations.create(null, "tool.execute", "tool", def.name(), summary, payload);
        return ExecuteResult.confirmationRequired(IdCodec.encode(CONF_PREFIX, item.id()));
      }
      long rawId = IdCodec.decode(CONF_PREFIX, req.confirmationId());
      var item = confirmations.get(rawId);
      if (!"approved".equalsIgnoreCase(item.status())) {
        return ExecuteResult.confirmationRequired(req.confirmationId());
      }
    }

    long start = System.currentTimeMillis();
    try {
      ToolSecurityProperties sec = buildSecurity();
      ToolResult r =
          CompletableFuture.supplyAsync(
                  () -> {
                    try {
                      return handler.execute(new ToolContext(http, models, sec), req.input());
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  })
              .orTimeout(def.timeoutMs(), TimeUnit.MILLISECONDS)
              .join();

      executions.record(
          actor == null ? "user" : actor,
          "tool",
          "tool.execute." + def.name(),
          "tool",
          def.name(),
          null,
          req.input() == null ? null : req.input().toString(),
          r.summary(),
          "succeeded",
          null,
          System.currentTimeMillis() - start);
      return ExecuteResult.succeeded(r.summary(), r.data());
    } catch (RuntimeException e) {
      String code = errorCode(e);
      executions.record(
          actor == null ? "user" : actor,
          "tool",
          "tool.execute." + def.name(),
          "tool",
          def.name(),
          null,
          req.input() == null ? null : req.input().toString(),
          null,
          "failed",
          code,
          System.currentTimeMillis() - start);
      return ExecuteResult.failed(code, safeMessage(e));
    }
  }

  public record ExecuteRequest(String toolName, JsonNode input, String confirmationId) {}

  public record ExecuteResult(
      String status,
      String confirmationId,
      String summary,
      String errorCode,
      String errorMessage,
      JsonNode data) {
    public static ExecuteResult succeeded(String summary, JsonNode data) {
      return new ExecuteResult("succeeded", null, summary, null, null, data);
    }

    public static ExecuteResult confirmationRequired(String confirmationId) {
      return new ExecuteResult("confirmation_required", confirmationId, null, null, null, null);
    }

    public static ExecuteResult failed(String errorCode, String errorMessage) {
      return new ExecuteResult("failed", null, null, errorCode, errorMessage, null);
    }
  }

  private static String errorCode(RuntimeException e) {
    Throwable t = unwrap(e);
    if (t instanceof java.util.concurrent.TimeoutException) {
      return "TOOL_TIMEOUT";
    }
    return t.getClass().getSimpleName();
  }

  private static String safeMessage(RuntimeException e) {
    Throwable t = unwrap(e);
    String msg = t.getMessage();
    if (msg == null || msg.isBlank()) {
      return t.getClass().getSimpleName();
    }
    if (msg.length() > 500) {
      return msg.substring(0, 500);
    }
    return msg;
  }

  private static Throwable unwrap(RuntimeException e) {
    Throwable t = e;
    while (t.getCause() != null
        && (t instanceof java.util.concurrent.CompletionException
            || t instanceof RuntimeException)) {
      if (t.getCause() == t) {
        break;
      }
      t = t.getCause();
    }
    return t;
  }

  private boolean isToolEnabled(String name) {
    return appSettings.getBoolean("tool.enabled." + name).orElse(true);
  }

  private ToolSecurityProperties buildSecurity() {
    var list =
        appSettings.getStringListJson("tool.domainAllowlist").orElse(security.domainAllowlist());
    return new ToolSecurityProperties(list);
  }
}
