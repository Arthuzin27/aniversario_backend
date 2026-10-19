package com.convite.evelyn.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "familia")
public class Familia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "chave_idempotencia", unique = true, updatable = false)
    private UUID chaveIdempotencia;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private Instant dataCadastro;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant dataAtualizacao;

    @OneToMany(mappedBy = "familia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Convidado> convidados = new ArrayList<>();

    protected Familia() {
    }

    public Familia(String nome, UUID chaveIdempotencia) {
        this.nome = nome;
        this.chaveIdempotencia = chaveIdempotencia;
    }

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        dataCadastro = agora;
        dataAtualizacao = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        dataAtualizacao = Instant.now();
    }

    public Convidado adicionarConvidado(String nomeCompleto) {
        Convidado convidado = new Convidado(nomeCompleto, this);
        convidados.add(convidado);
        return convidado;
    }

    public void removerConvidado(Convidado convidado) {
        convidados.remove(convidado);
    }

    /** Garante que a data de atualização mude mesmo quando só a lista de convidados foi alterada. */
    public void marcarAtualizacao() {
        dataAtualizacao = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public UUID getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public Instant getDataCadastro() {
        return dataCadastro;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }

    public List<Convidado> getConvidados() {
        return convidados;
    }
}
