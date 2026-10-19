package com.convite.evelyn.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioCsvExporterTest {

    @Test
    void escapaAspasENeutralizaFormulas() {
        assertThat(RelatorioCsvExporter.campo("Ana \"Aninha\" Souza")).isEqualTo("\"Ana \"\"Aninha\"\" Souza\"");
        assertThat(RelatorioCsvExporter.campo("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(RelatorioCsvExporter.campo("+55")).isEqualTo("\"'+55\"");
        assertThat(RelatorioCsvExporter.campo("Família; Silva")).isEqualTo("\"Família; Silva\"");
    }
}
