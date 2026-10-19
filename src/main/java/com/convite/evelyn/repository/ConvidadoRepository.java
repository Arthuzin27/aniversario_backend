package com.convite.evelyn.repository;

import com.convite.evelyn.dto.ConvidadoComFamilia;
import com.convite.evelyn.entity.Convidado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ConvidadoRepository extends JpaRepository<Convidado, Long> {

    /** Carrega, em uma única consulta, os convidados de várias famílias (evita N+1 na listagem paginada). */
    @Query("select c from Convidado c where c.familia.id in :familiaIds order by c.id")
    List<Convidado> findByFamiliaIds(@Param("familiaIds") Collection<Long> familiaIds);

    @Query("""
            select new com.convite.evelyn.dto.ConvidadoComFamilia(c.id, c.nomeCompleto, f.nome)
            from Convidado c join c.familia f
            """)
    List<ConvidadoComFamilia> findTodosComFamilia();
}
