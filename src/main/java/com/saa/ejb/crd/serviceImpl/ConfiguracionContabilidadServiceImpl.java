package com.saa.ejb.crd.serviceImpl;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.dto.EstadoContabilidadCrd;
import com.saa.rubros.CrdParametroContabilidad;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see ConfiguracionContabilidadService
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Stateless
public class ConfiguracionContabilidadServiceImpl implements ConfiguracionContabilidadService {

    /**
     * SCP.PDTR.PDTRVLRV es VARCHAR2(100 BYTE) — verificado contra ALL_TAB_COLUMNS el
     * 2026-08-27. Ahí se codifica la huella "usuario | fecha | motivo" porque el rubro 237
     * no tiene columnas de auditoría propias.
     */
    private static final int PDTRVLRV_MAX_BYTES = 100;

    private static final String SEPARADOR = " | ";

    @EJB
    private DetalleRubroDaoService detalleRubroDaoService;

    /**
     * Bug corregido 2026-08-28: mismo defecto que
     * {@code ConfiguracionGeneracionAportesServiceImpl.porFaltanteActiva} (rubro 242) — si
     * {@code detalleRubroDaoService} lanza (p. ej. el rubro 237 sin catálogo cargado), el
     * contenedor marca la transacción del LLAMADOR como rollback-only antes de que este
     * {@code catch} la reciba, aunque acá se "atrape" y se devuelva {@code false}. Cualquier
     * consulta posterior en esa misma transacción revienta con {@code STATUS_MARKED_ROLLBACK},
     * sin relación aparente con este método. {@code REQUIRES_NEW} aísla la lectura en su propia
     * transacción, que es lo que "apagado es el lado seguro" ya pretendía lograr.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean contabilidadActiva() {
        System.out.println("ConfiguracionContabilidadService.contabilidadActiva");
        try {
            Double valor = detalleRubroDaoService.selectValorNumericoByRubAltDetAlt(
                Rubros.CRD_PARAMETROS_CONTABILIDAD, CrdParametroContabilidad.CONTABILIDAD_ACTIVA);
            return valor != null && valor == 1.0;
        } catch (Throwable e) {
            // Apagado es el lado seguro: si la lectura del rubro falla no se debe
            // contabilizar por accidente.
            System.err.println("ConfiguracionContabilidadService.contabilidadActiva - "
                + "error al leer el rubro, se asume INACTIVA: " + e.getMessage());
            return false;
        }
    }

    /** Mismo motivo que {@link #contabilidadActiva()}: aislar la lectura en su propia transacción. */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public EstadoContabilidadCrd obtenerEstado() {
        System.out.println("ConfiguracionContabilidadService.obtenerEstado");
        EstadoContabilidadCrd estado = new EstadoContabilidadCrd(false);
        try {
            Double valor = detalleRubroDaoService.selectValorNumericoByRubAltDetAlt(
                Rubros.CRD_PARAMETROS_CONTABILIDAD, CrdParametroContabilidad.CONTABILIDAD_ACTIVA);
            estado.setActiva(valor != null && valor == 1.0);

            String huella = detalleRubroDaoService.selectValorStringByRubAltDetAlt(
                Rubros.CRD_PARAMETROS_CONTABILIDAD, CrdParametroContabilidad.CONTABILIDAD_ACTIVA);
            parsearHuella(huella, estado);
        } catch (Throwable e) {
            // Mismo lado seguro que contabilidadActiva(): si la lectura falla, se devuelve
            // el estado en blanco en vez de fallar el GET.
            System.err.println("ConfiguracionContabilidadService.obtenerEstado - "
                + "error al leer el rubro, se devuelve estado en blanco: " + e.getMessage());
        }
        return estado;
    }

    @Override
    public boolean actualizar(boolean activa, String usuario, String motivo) throws Throwable {
        System.out.println("ConfiguracionContabilidadService.actualizar - activa: " + activa
            + " - usuario: " + usuario + " - motivo: " + motivo);

        double nuevoValor = activa ? 1.0 : 0.0;
        String huella = construirHuella(usuario, motivo);

        int filas = detalleRubroDaoService.actualizarValorNumericoYAlfanumericoByRubAltDetAlt(
            Rubros.CRD_PARAMETROS_CONTABILIDAD, CrdParametroContabilidad.CONTABILIDAD_ACTIVA,
            nuevoValor, huella);

        if (filas == 0) {
            throw new com.saa.basico.util.IncomeException(
                "No se encontro el detalle de rubro de contabilidad de CRD (rubro "
                + Rubros.CRD_PARAMETROS_CONTABILIDAD + ", detalle "
                + CrdParametroContabilidad.CONTABILIDAD_ACTIVA + ")");
        }

        System.out.println("ConfiguracionContabilidadService.actualizar - contabilidad de CRD ahora: "
            + (activa ? "ACTIVA" : "INACTIVA") + " - huella: " + huella);
        return activa;
    }

    // ------------------------------------------------------------------------
    // Huella "usuario | fecha | motivo" en PDTRVLRV
    // ------------------------------------------------------------------------

    /**
     * Arma la huella respetando el límite de {@link #PDTRVLRV_MAX_BYTES}. Sólo el motivo se
     * trunca: usuario y fecha se preservan enteros salvo que ni ellos quepan, caso extremo
     * en el que se trunca el resultado completo como último resguardo.
     */
    private String construirHuella(String usuario, String motivo) {
        String usuarioSeguro = usuario != null ? usuario : "";
        String fechaIso = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        String prefijo = usuarioSeguro + SEPARADOR + fechaIso + SEPARADOR;

        int bytesPrefijo = prefijo.getBytes(StandardCharsets.UTF_8).length;
        int presupuestoMotivo = PDTRVLRV_MAX_BYTES - bytesPrefijo;
        String motivoTruncado = presupuestoMotivo > 0 ? truncarUtf8(motivo, presupuestoMotivo) : "";

        return truncarUtf8(prefijo + motivoTruncado, PDTRVLRV_MAX_BYTES);
    }

    /**
     * Decodifica la huella hacia {@code estado}. Si el valor es nulo/vacío, no tiene
     * exactamente tres partes, o la fecha no parsea, deja los tres campos en {@code null}
     * sin lanzar: es texto libre guardado por este mismo método, no una fuente confiable.
     */
    private void parsearHuella(String huella, EstadoContabilidadCrd estado) {
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

    /** Trunca a lo sumo a {@code maxBytes} bytes UTF-8, sin partir un carácter multibyte. */
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
