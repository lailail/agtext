package com.agtext.chat.config;

import com.agtext.chat.service.ChatSettingsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatSettingsProperties.class)
public class ChatConfig {}
