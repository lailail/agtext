package com.agtext.task.config;

import com.agtext.task.service.TaskContextProperties;
import com.agtext.task.service.TaskReminderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({TaskContextProperties.class, TaskReminderProperties.class})
public class TaskConfig {}
