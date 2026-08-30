package com.saa.ejb.crd.serviceImpl;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.ejb.crd.service.ConfiguracionGeneracionAportesService;
import com.saa.ejb.crd.service.dto.EstadoGeneracionPorFaltante;
import com.saa.rubros.CrdGeneracionPorFaltante;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see ConfiguracionGeneracionAportesService
 *
 * Mismo patrón que {@code ConfiguracionContabilidadServiceImpl} (Fase 1): huella
 * "usuario | fecha | motivo" codificada en {@code PDTRVLRV} porque el rubro 242 no tiene
 * columnas de auditoría propias.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Stateless
public class ConfiguracionGeneracionAportesServiceImpl implements ConfiguracionGeneracionAportesService {

    /** Ver el mismo límite documentado en ConfiguracionContabilidadServiceImpl. */
    private static final int PDTRVLRV_MAX_BYTES = 100;

    private static final String SEPARADOR = " | ";

    @EJB
    private DetalleRubroDaoService detalleRubroDaoService;

    /**
     * Bug corregido 2026-08-28: si {@code detalleRubroDaoService} (otro @Stateless, REQUIRED)
     * lanza — p. ej. el catálogo del rubro 242 todavía no está cargado y
     * {@code getSingleResult()} tira {@code NoResultException} — el contenedor marca la
     * transacción AMBIENTE como rollback-only en cuanto la excepción sale de ese bean, ANTES de
     * que el {@code catch (Throwable)} de acá la reciba. El catch "atrapa" la excepción en Java
     * y devuelve {@code false} con normalidad, pero la transacción del LLAMADOR (p. ej.
     * {@code GeneracionArchivoPetroServiceImpl.procesarGeneracion}) queda envenenada: la
     * siguiente consulta que se ejecute ahí, sea cual sea, revienta con
     * {@code STATUS_MARKED_ROLLBACK} — así se manifestó el 2026-08-28 sobre una consulta de
     * CRD.HSTR/CRD.ENTD que no tenía nada que ver con el rubro. {@code REQUIRES_NEW} aísla esta
     * lectura en su propia transacción: si falla, esa transacción chica se revierte sola y el
     * llamador nunca se entera a nivel de JTA — que es lo que "apagado es el lado seguro" ya
     * pretendía, pero un try/catch en Java no alcanza a garantizar bajo CMT.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean porFaltanteActiva() {
        System.out.println("ConfiguracionGeneracionAportesService.porFaltanteActiva");
        try {
            Double valor = detalleRubroDaoService.selectValorNumericoByRubAltDetAlt(
                Rubros.CRD_GENERACION_POR_FALTANTE, CrdGeneracionPorFaltante.GENERACION_POR_FALTANTE_ACTIVA);
            return valor != null && valor == 1.0;
        } catch (Throwable e) {
            // Apagado es el lado seguro, y ademas el valor con el que se entrega esta fase.
            System.err.println("ConfiguracionGeneracionAportesService.porFaltanteActiva - "
                + "error al leer el rubro, se asume INACTIVA: " + e.getMessage());
            return false;
        }
    }

    /** Mismo motivo que {@link #porFaltanteActiva()}: aislar la lectura en su propia transacción. */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public EstadoGeneracionPorFaltante obtenerEstado() {
        System.out.println("ConfiguracionGeneracionAportesService.obtenerEstado");
        EstadoGeneracionPorFaltante estado = new EstadoGeneracionPorFaltante(false);
        try {
            Double valor = detalleRubroDaoService.selectValorNumericoByRubAltDetAlt(
                Rubros.CRD_GENERACION_POR_FALTANTE, CrdGeneracionPorFaltante.GENERACION_POR_FALTANTE_ACTIVA);
            estado.setActiva(valor != null && valor == 1.0);

            String huella = detalleRubroDaoService.selectValorStringByRubAltDetAlt(
                Rubros.CRD_GENERACION_POR_FALTANTE, CrdGeneracionPorFaltante.GENERACION_POR_FALTANTE_ACTIVA);
            parsearHuella(huella, estado);
        } catch (Throwable e) {
            System.err.println("ConfiguracionGeneracionAportesService.obtenerEstado - "
                + "error al leer el rubro, se devuelve estado en blanco: " + e.getMessage());
        }
        return estado;
    }

    @Override
    public boolean actualizar(boolean activa, String usuario, String motivo) throws Throwable {
        System.out.println("ConfiguracionGeneracionAportesService.actualizar - activa: " + activa
            + " - usuario: " + usuario + " - motivo: " + motivo);

        double nuevoValor = activa ? 1.0 : 0.0;
        String huella = construirHuella(usuario, motivo);

        int filas = detalleRubroDaoService.actualizarValorNumericoYAlfanumericoByRubAltDetAlt(
            Rubros.CRD_GENERACION_POR_FALTANTE, CrdGeneracionPorFaltante.GENERACION_POR_FALTANTE_ACTIVA,
            nuevoValor, huella);

        if (filas == 0) {
            throw new com.saa.basico.util.IncomeException(
                "No se encontro el detalle de rubro de generacion por faltante (rubro "
                + Rubros.CRD_GENERACION_POR_FALTANTE + ", detalle "
                + CrdGeneracionPorFaltante.GENERACION_POR_FALTANTE_ACTIVA
                + "). Falta cargar el catalogo -- ver "
                + "docs/logica-negocio/crd/sql/70_CATALOGO_RUBRO_GENERACION_POR_FALTANTE.sql");
        }

        System.out.println("ConfiguracionGeneracionAportesService.actualizar - camino nuevo de generacion ahora: "
            + (activa ? "ACTIVO" : "INACTIVO") + " - huella: " + huella);
        return activa;
    }

    // ------------------------------------------------------------------------
    // Huella "usuario | fecha | motivo" en PDTRVLRV (identico a ConfiguracionContabilidadServiceImpl)
    // ------------------------------------------------------------------------

    private String construirHuella(String usuario, String motivo) {
        String usuarioSeguro = usuario != null ? usuario : "";
        String fechaIso = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        String prefijo = usuarioSeguro + SEPARADOR + fechaIso + SEPARADOR;

        int bytesPrefijo = prefijo.getBytes(StandardCharsets.UTF_8).length;
        int presupuestoMotivo = PDTRVLRV_MAX_BYTES - bytesPrefijo;
        String motivoTruncado = presupuestoMotivo > 0 ? truncarUtf8(motivo, presupuestoMotivo) : "";

        return truncarUtf8(prefijo + motivoTruncado, PDTRVLRV_MAX_BYTES);
    }

    private void parsearHuella(String huella, EstadoGeneracionPorFaltante estado) {
        if (huella == null || huella.trim().isEmpty()) {
            return;
        }
        try {
            String[] partes = huella.split(Pattern.quote(SEPARADOR), 3);
            if (partes.length != 3) {
                return;
            }
            estado.setUsuarioUltimoCambio(partes[0]);
            estado.setFechaUltimoCambio(LocalDateTime.parse(partes[1]));
            estado.setMotivoUltimoCambio(partes[2]);
        } catch (Exception e) {
            estado.setUsuarioUltimoCambio(null);
            estado.setFechaUltimoCambio(null);
            estado.setMotivoUltimoCambio(null);
        }
    }

    private String truncarUtf8(String texto, int maxBytes) {
        if (texto == null) {
            return "";
        }
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return texto;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, maxBytes);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
        try {
            return decoder.decode(buffer).toString();
        } catch (Exception e) {
            return "";
        }
    }
}
