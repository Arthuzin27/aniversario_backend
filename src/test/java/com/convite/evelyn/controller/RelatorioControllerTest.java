package com.convite.evelyn.controller;

import com.convite.evelyn.IntegracaoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RelatorioControllerTest extends IntegracaoTestBase {

    private String token;

    @BeforeEach
    void autenticar() throws Exception {
        token = tokenAdmin();
    }

    @Test
    void relatorioOrdenaAlfabeticamenteSemDiferenciarMaiusculasENumera() throws Exception {
        cadastrarFamilia("Família Silva", "pedro Silva", "Carlos Silva");
        cadastrarFamilia("Família Oliveira", "Maria Oliveira", "bruno Oliveira");
        cadastrarFamilia("Família Souza", "Ana Souza", "Álvaro Souza");

        mockMvc.perform(get("/api/admin/relatorios/convidados").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("LISTA GERAL DE CONVIDADOS"))
                .andExpect(jsonPath("$.geradoEm").isNotEmpty())
                .andExpect(jsonPath("$.convidados[*].nomeCompleto", contains(
                        "Álvaro Souza", "Ana Souza", "bruno Oliveira", "Carlos Silva", "Maria Oliveira", "pedro Silva")))
                .andExpect(jsonPath("$.convidados[*].numero", contains(1, 2, 3, 4, 5, 6)))
                .andExpect(jsonPath("$.convidados[2].familia").value("Família Oliveira"))
                .andExpect(jsonPath("$.totalPessoas").value(6));
    }

    @Test
    void relatorioRefleteEdicoesEExclusoes() throws Exception {
        long silva = cadastrarFamilia("Família Silva", "Carlos Silva");
        long souza = cadastrarFamilia("Família Souza", "Ana Souza");

        mockMvc.perform(put("/api/admin/familias/" + silva).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Família Silva\",\"convidados\":[{\"nomeCompleto\":\"Zeca Silva\"},{\"nomeCompleto\":\"Bia Silva\"}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/familias/" + souza).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/relatorios/convidados").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.convidados[*].nomeCompleto", contains("Bia Silva", "Zeca Silva")))
                .andExpect(jsonPath("$.totalPessoas").value(2));
    }

    @Test
    void exportaCsvCompativelComExcel() throws Exception {
        cadastrarFamilia("Família Silva", "Carlos Silva");
        cadastrarFamilia("Família Souza", "Ana Souza");

        byte[] corpo = mockMvc.perform(get("/api/admin/relatorios/convidados/csv").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".csv")))
                .andReturn().getResponse().getContentAsByteArray();

        String csv = new String(corpo, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿");
        assertThat(csv).contains("1;\"Ana Souza\";\"Família Souza\"\r\n2;\"Carlos Silva\";\"Família Silva\"");
        assertThat(csv).contains("TOTAL DE PESSOAS;2");
    }

    @Test
    void exportaPdf() throws Exception {
        cadastrarFamilia("Família Silva", "Carlos Silva");

        byte[] pdf = mockMvc.perform(get("/api/admin/relatorios/convidados/pdf").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void relatorioVazio() throws Exception {
        mockMvc.perform(get("/api/admin/relatorios/convidados").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.convidados").isEmpty())
                .andExpect(jsonPath("$.totalPessoas").value(0));
        mockMvc.perform(get("/api/admin/relatorios/convidados/pdf").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }
}
