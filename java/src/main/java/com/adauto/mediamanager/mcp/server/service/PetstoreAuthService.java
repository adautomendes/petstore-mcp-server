package com.adauto.mediamanager.mcp.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adauto.mediamanager.mcp.server.adapter.PetstoreAuthAdapter;
import com.adauto.mediamanager.mcp.server.model.dto.LoginRequestDTO;
import com.adauto.mediamanager.mcp.server.model.dto.LoginResponseDTO;

@Service
public class PetstoreAuthService
{
    private final PetstoreAuthAdapter petstoreAuthAdapter;

    @Autowired
    public PetstoreAuthService(PetstoreAuthAdapter petstoreAuthAdapter)
    {
        this.petstoreAuthAdapter = petstoreAuthAdapter;
    }

    public String getToken()
    {
        LoginRequestDTO loginRequestDTO = LoginRequestDTO.builder()
                                                         .user("admin")
                                                         .pass("admin")
                                                         .build();

        return petstoreAuthAdapter.getPetstoreAuthWebClient()
                                  .post().uri("/auth/login")
                                  .bodyValue(loginRequestDTO)
                                  .retrieve()
                                  .bodyToMono(LoginResponseDTO.class)
                                  .block()
                                  .getToken();
    }
}
