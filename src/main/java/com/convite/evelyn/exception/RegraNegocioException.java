package com.convite.evelyn.exception;

/** Violação de regra de negócio causada pelos dados enviados (resulta em HTTP 400). */
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
