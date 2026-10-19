package com.convite.evelyn.service;

/** Normalização de nomes digitados pelos usuários. */
public final class NomeUtils {

    private NomeUtils() {
    }

    /** Remove espaços no início e no fim e reduz espaços internos repetidos a um único espaço. */
    public static String normalizar(String nome) {
        if (nome == null) {
            return null;
        }
        return nome.strip().replaceAll("\\s+", " ");
    }
}
