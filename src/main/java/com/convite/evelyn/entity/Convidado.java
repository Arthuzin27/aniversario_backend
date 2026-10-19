package com.convite.evelyn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "convidado")
public class Convidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    protected Convidado() {
    }

    Convidado(String nomeCompleto, Familia familia) {
        this.nomeCompleto = nomeCompleto;
        this.familia = familia;
    }

    @PrePersist
    void aoCriar() {
        dataCadastro = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    public Familia getFamilia() {
        return familia;
    }

    public Instant getDataCadastro() {
        return dataCadastro;
    }
}
