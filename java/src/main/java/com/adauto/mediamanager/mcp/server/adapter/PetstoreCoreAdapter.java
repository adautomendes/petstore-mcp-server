package com.adauto.mediamanager.mcp.server.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PetstoreCoreAdapter
{
    @Value("${petstore-core.host}")
    private String petstoreCoreHost;

    @Value("${petstore-core.port}")
    private String petstoreCorePort;

    @Bean
    public String getPetstoreCoreUrl()
    {
        return String.format("http://%s:%s", this.petstoreCoreHost, this.petstoreCorePort);
    }

    @Bean
    public WebClient getPetstoreCoreWebClient()
    {
        return WebClient.builder()
                        .baseUrl(this.getPetstoreCoreUrl())
                        .build();
    }
}
