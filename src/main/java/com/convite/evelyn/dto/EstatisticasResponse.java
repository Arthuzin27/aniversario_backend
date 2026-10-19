package com.convite.evelyn.dto;

import java.math.BigDecimal;

public record EstatisticasResponse(long totalPessoas, long totalFamilias, BigDecimal mediaPessoasPorFamilia) {
}
