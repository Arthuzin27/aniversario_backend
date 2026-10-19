package com.convite.evelyn.controller;

import com.convite.evelyn.dto.EstatisticasResponse;
import com.convite.evelyn.dto.FamiliaAtualizacaoRequest;
import com.convite.evelyn.dto.FamiliaResponse;
import com.convite.evelyn.dto.PaginaResponse;
import com.convite.evelyn.service.FamiliaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administração", description = "Gerenciamento de famílias e indicadores")
@SecurityRequirement(name = "bearer")
public class FamiliaAdminController {

    private final FamiliaService familiaService;

    public FamiliaAdminController(FamiliaService familiaService) {
        this.familiaService = familiaService;
    }

    @GetMapping("/familias")
    @Operation(summary = "Lista famílias (mais recentes primeiro), com busca por nome da família ou de participante")
    public PaginaResponse<FamiliaResponse> listar(
            @RequestParam(required = false) @Size(max = 100, message = "A busca deve ter no máximo 100 caracteres.") String busca,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página deve ser maior ou igual a zero.") int pagina,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O tamanho mínimo da página é 1.")
            @Max(value = 100, message = "O tamanho máximo da página é 100.") int tamanho) {
        return familiaService.listar(busca, pagina, tamanho);
    }

    @GetMapping("/familias/{id}")
    @Operation(summary = "Consulta uma família com seus participantes")
    public FamiliaResponse buscar(@PathVariable Long id) {
        return familiaService.buscarPorId(id);
    }

    @PutMapping("/familias/{id}")
    @Operation(summary = "Atualiza o nome da família e sincroniza a lista de participantes")
    public FamiliaResponse atualizar(@PathVariable Long id, @Valid @RequestBody FamiliaAtualizacaoRequest request) {
        return familiaService.atualizar(id, request);
    }

    @DeleteMapping("/familias/{id}")
    @Operation(summary = "Exclui a família e todos os seus participantes")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        familiaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Totais calculados a partir do banco de dados")
    public EstatisticasResponse estatisticas() {
        return familiaService.estatisticas();
    }
}
