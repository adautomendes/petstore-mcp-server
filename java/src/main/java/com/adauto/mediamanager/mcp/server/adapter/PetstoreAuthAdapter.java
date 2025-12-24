package com.adauto.mediamanager.mcp.server.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PetstoreAuthAdapter
{
    @Value("${petstore-auth.host}")
    private String petstoreAuthHost;

    @Value("${petstore-auth.port}")
    private String petstoreAuthPort;

    @Bean
    public String getPetstoreAuthUrl()
    {
        return String.format("http://%s:%s", this.petstoreAuthHost, this.petstoreAuthPort);
    }

    @Bean
    public WebClient getPetstoreAuthWebClient()
    {
        return WebClient.builder()
                        .baseUrl(this.getPetstoreAuthUrl())
                        .build();
    }
}
