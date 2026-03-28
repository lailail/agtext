package com.agtext.memory.config;

import com.agtext.memory.service.MemorySettingsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MemorySettingsProperties.class)
public class MemoryConfig {}
