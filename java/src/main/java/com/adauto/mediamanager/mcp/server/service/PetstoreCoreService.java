package com.adauto.mediamanager.mcp.server.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adauto.mediamanager.mcp.server.adapter.PetstoreCoreAdapter;
import com.adauto.mediamanager.mcp.server.model.dto.PetDTO;

@Service
public class PetstoreCoreService
{
    private final PetstoreAuthService petstoreAuthService;
    private final PetstoreCoreAdapter petstoreCoreAdapter;

    @Autowired
    public PetstoreCoreService(PetstoreAuthService petstoreAuthService,
                               PetstoreCoreAdapter petstoreCoreAdapter)
    {
        this.petstoreAuthService = petstoreAuthService;
        this.petstoreCoreAdapter = petstoreCoreAdapter;
    }

    @Tool(description = "List all pets")
    public List<PetDTO> findPet()
    {
        return Arrays.asList(petstoreCoreAdapter.getPetstoreCoreWebClient()
                                                .get()
                                                .uri("/pet")
                                                .header("token", this.getToken())
                                                .retrieve()
                                                .bodyToMono(PetDTO[].class)
                                                .block());
    }

    private String getToken()
    {
        return this.petstoreAuthService.getToken();
    }
}
