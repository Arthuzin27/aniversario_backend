package com.convite.evelyn.exception;

import com.convite.evelyn.dto.ErroResponse;
import com.convite.evelyn.dto.ErroResponse.CampoErro;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/** Converte exceções em respostas JSON padronizadas, sem expor detalhes internos. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new CampoErro(fe.getField(), fe.getDefaultMessage()))
                .distinct()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos informados.", req, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErroResponse> validacaoParametros(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<CampoErro> campos = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new CampoErro(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos.", req, campos);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        List<CampoErro> campos = ex.getCampo() == null ? List.of() : List.of(new CampoErro(ex.getCampo(), ex.getMessage()));
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), req, campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErroResponse> requisicaoMalFormada(Exception ex, HttpServletRequest req) {
        log.debug("Requisição mal formada em {}: {}", req.getRequestURI(), ex.getMessage());
        return resposta(HttpStatus.BAD_REQUEST,
                "Requisição inválida. Verifique se os valores foram informados no formato correto.", req, List.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado.", req, List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ErroResponse> credenciaisInvalidas(BadCredentialsException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para este endereço.", req, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErroResponse> tipoNaoSuportado(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie os dados no formato JSON.", req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> conflito(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getClass().getSimpleName());
        return resposta(HttpStatus.CONFLICT,
                "Não foi possível salvar porque os dados conflitam com um registro existente. "
                        + "Se você acabou de enviar esta confirmação, ela já foi recebida.", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> erroInesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente em alguns instantes.", req, List.of());
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, HttpServletRequest req,
                                                  List<CampoErro> campos) {
        ErroResponse corpo = new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                mensagem, req.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
