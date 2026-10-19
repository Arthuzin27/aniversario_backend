package com.convite.evelyn.controller;

import com.convite.evelyn.dto.RelatorioConvidadosResponse;
import com.convite.evelyn.service.RelatorioCsvExporter;
import com.convite.evelyn.service.RelatorioPdfExporter;
import com.convite.evelyn.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin/relatorios/convidados")
@Tag(name = "Relatórios", description = "Lista geral de convidados em ordem alfabética")
@SecurityRequirement(name = "bearer")
public class RelatorioController {

    private static final DateTimeFormatter NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final MediaType CSV = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final RelatorioService relatorioService;
    private final RelatorioPdfExporter pdfExporter;
    private final RelatorioCsvExporter csvExporter;

    public RelatorioController(RelatorioService relatorioService, RelatorioPdfExporter pdfExporter,
                               RelatorioCsvExporter csvExporter) {
        this.relatorioService = relatorioService;
        this.pdfExporter = pdfExporter;
        this.csvExporter = csvExporter;
    }

    @GetMapping
    @Operation(summary = "Relatório em JSON (usado na pré-visualização)")
    public RelatorioConvidadosResponse visualizar() {
        return relatorioService.gerar();
    }

    @GetMapping("/pdf")
    @Operation(summary = "Exporta o relatório em PDF")
    public ResponseEntity<byte[]> pdf() {
        RelatorioConvidadosResponse relatorio = relatorioService.gerar();
        return arquivo(pdfExporter.exportar(relatorio), MediaType.APPLICATION_PDF, nomeArquivo(relatorio, "pdf"));
    }

    @GetMapping("/csv")
    @Operation(summary = "Exporta o relatório em CSV (compatível com Excel)")
    public ResponseEntity<byte[]> csv() {
        RelatorioConvidadosResponse relatorio = relatorioService.gerar();
        return arquivo(csvExporter.exportar(relatorio), CSV, nomeArquivo(relatorio, "csv"));
    }

    private static String nomeArquivo(RelatorioConvidadosResponse relatorio, String extensao) {
        return "lista-convidados-" + relatorio.geradoEm().format(NOME_ARQUIVO) + "." + extensao;
    }

    private static ResponseEntity<byte[]> arquivo(byte[] conteudo, MediaType tipo, String nome) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nome).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(conteudo);
    }
}
