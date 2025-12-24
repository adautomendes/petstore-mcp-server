package com.adauto.mediamanager.mcp.server.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

    @Tool(description = "Register a new pet")
    public PetDTO registerPet(@ToolParam(description = "The pet name") String nome,
                              @ToolParam(description = "The pet breed") String raca,
                              @ToolParam(description = "The pet age") Integer idade)
    {
        PetDTO petDTO = PetDTO.builder()
                              .nome(nome)
                              .raca(raca)
                              .idade(idade)
                              .build();

        return petstoreCoreAdapter.getPetstoreCoreWebClient()
                                  .post()
                                  .uri("/pet")
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .bodyValue(petDTO)
                                  .header("token", this.getToken())
                                  .retrieve()
                                  .bodyToMono(PetDTO.class)
                                  .block();
    }

    @Tool(description = "Returns a list with all registered pets")
    public List<PetDTO> findAllPet()
    {
        return Arrays.asList(petstoreCoreAdapter.getPetstoreCoreWebClient()
                                                .get()
                                                .uri("/pet")
                                                .header("token", this.getToken())
                                                .retrieve()
                                                .bodyToMono(PetDTO[].class)
                                                .block());
    }

    @Tool(description = "Returns a list with registered pets with a given name")
    public List<PetDTO> findPetByName(
        @ToolParam(description = "The name of pet to be found") String name)
    {
        return Arrays.asList(petstoreCoreAdapter.getPetstoreCoreWebClient()
                                                .get()
                                                .uri(String.format("/pet?nome=%s", name))
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
