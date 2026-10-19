package com.convite.evelyn.service;

import com.convite.evelyn.dto.RelatorioConvidadosResponse;
import com.convite.evelyn.dto.RelatorioConvidadosResponse.Item;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.HeaderFooter;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Gera o relatório geral de convidados em PDF (A4), pronto para impressão. */
@Component
public class RelatorioPdfExporter {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final Color COR_CABECALHO = new Color(0xF6, 0xE3, 0xE8);
    private static final Color COR_LINHA_ALTERNADA = new Color(0xFC, 0xF8, 0xF4);

    public byte[] exportar(RelatorioConvidadosResponse relatorio) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 48, 48);
        PdfWriter.getInstance(documento, saida);

        HeaderFooter rodape = new HeaderFooter(new Phrase("Página ", fonte(9, Font.NORMAL)), true);
        rodape.setAlignment(Element.ALIGN_CENTER);
        rodape.setBorder(Rectangle.NO_BORDER);
        documento.setFooter(rodape);

        documento.open();
        Paragraph titulo = new Paragraph(relatorio.titulo(), fonte(16, Font.BOLD));
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph data = new Paragraph("Gerado em " + relatorio.geradoEm().format(DATA), fonte(10, Font.NORMAL));
        data.setAlignment(Element.ALIGN_CENTER);
        data.setSpacingAfter(16);
        documento.add(data);

        PdfPTable tabela = new PdfPTable(new float[]{1f, 6f, 4f});
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        cabecalho(tabela, "Nº");
        cabecalho(tabela, "Nome completo");
        cabecalho(tabela, "Família");
        for (Item item : relatorio.convidados()) {
            Color fundo = item.numero() % 2 == 0 ? COR_LINHA_ALTERNADA : Color.WHITE;
            celula(tabela, String.valueOf(item.numero()), fundo, Element.ALIGN_RIGHT);
            celula(tabela, item.nomeCompleto(), fundo, Element.ALIGN_LEFT);
            celula(tabela, item.familia(), fundo, Element.ALIGN_LEFT);
        }
        documento.add(tabela);

        Paragraph total = new Paragraph("TOTAL DE PESSOAS: " + relatorio.totalPessoas(), fonte(12, Font.BOLD));
        total.setSpacingBefore(14);
        documento.add(total);

        documento.close();
        return saida.toByteArray();
    }

    private static void cabecalho(PdfPTable tabela, String texto) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, fonte(10, Font.BOLD)));
        celula.setBackgroundColor(COR_CABECALHO);
        celula.setPadding(6);
        tabela.addCell(celula);
    }

    private static void celula(PdfPTable tabela, String texto, Color fundo, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, fonte(10, Font.NORMAL)));
        celula.setBackgroundColor(fundo);
        celula.setHorizontalAlignment(alinhamento);
        celula.setPadding(5);
        tabela.addCell(celula);
    }

    private static Font fonte(float tamanho, int estilo) {
        return FontFactory.getFont(FontFactory.HELVETICA, "Cp1252", false, tamanho, estilo);
    }
}
