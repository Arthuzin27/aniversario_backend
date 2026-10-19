package com.convite.evelyn.repository;

import com.convite.evelyn.entity.Familia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamiliaRepository extends JpaRepository<Familia, Long> {

    Optional<Familia> findByChaveIdempotencia(UUID chaveIdempotencia);

    @Query("select f from Familia f left join fetch f.convidados where f.id = :id")
    Optional<Familia> findComConvidadosById(@Param("id") Long id);

    /** Busca pelo nome da família ou pelo nome de qualquer participante. {@code termo} já deve vir em minúsculas e com curingas. */
    @Query("""
            select f from Familia f
            where lower(f.nome) like :termo escape '\\'
               or exists (select 1 from Convidado c
                          where c.familia = f and lower(c.nomeCompleto) like :termo escape '\\')
            """)
    Page<Familia> buscar(@Param("termo") String termo, Pageable pageable);

    /** Dentre os nomes informados (normalizados em minúsculas), retorna os que aparecem em mais de uma família. */
    @Query("""
            select lower(f.nome) from Familia f
            where lower(f.nome) in :nomes
            group by lower(f.nome)
            having count(f) > 1
            """)
    List<String> nomesRepetidos(@Param("nomes") Collection<String> nomes);
}
