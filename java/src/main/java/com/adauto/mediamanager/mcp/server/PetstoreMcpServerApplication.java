package com.adauto.mediamanager.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.adauto.mediamanager.mcp.server.service.PetstoreCoreService;

@SpringBootApplication
public class PetstoreMcpServerApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(PetstoreMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider petTools(PetstoreCoreService petstoreCoreService)
    {
        return MethodToolCallbackProvider.builder().toolObjects(petstoreCoreService).build();
    }

}
