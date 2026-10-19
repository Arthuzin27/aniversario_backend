package com.convite.evelyn.service;

import com.convite.evelyn.dto.RelatorioConvidadosResponse;
import com.convite.evelyn.dto.RelatorioConvidadosResponse.Item;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Gera CSV compatível com o Excel em português: UTF-8 com BOM, separador ";" e quebras CRLF.
 */
@Component
public class RelatorioCsvExporter {

    private static final String SEPARADOR = ";";
    private static final String QUEBRA = "\r\n";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportar(RelatorioConvidadosResponse relatorio) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(campo(relatorio.titulo())).append(QUEBRA);
        csv.append(campo("Gerado em: " + relatorio.geradoEm().format(DATA))).append(QUEBRA);
        csv.append(QUEBRA);
        csv.append("Nº").append(SEPARADOR).append("Nome completo").append(SEPARADOR).append("Família").append(QUEBRA);
        for (Item item : relatorio.convidados()) {
            csv.append(item.numero()).append(SEPARADOR)
                    .append(campo(item.nomeCompleto())).append(SEPARADOR)
                    .append(campo(item.familia())).append(QUEBRA);
        }
        csv.append(QUEBRA);
        csv.append("TOTAL DE PESSOAS").append(SEPARADOR).append(relatorio.totalPessoas()).append(QUEBRA);
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Escapa aspas e neutraliza fórmulas (=, +, -, @) para evitar injeção de fórmulas no Excel. */
    static String campo(String valor) {
        String texto = valor == null ? "" : valor;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
