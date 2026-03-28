package com.agtext.tool.platform.service;

import com.agtext.model.service.ModelService;
import java.net.http.HttpClient;

public record ToolContext(HttpClient http, ModelService models, ToolSecurityProperties security) {}
