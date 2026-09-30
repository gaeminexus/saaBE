package com.saa.ejb.cxp.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.ejb.cxp.service.dto.DocumentoCartera;
import com.saa.ejb.cxp.service.dto.ReporteCartera;
import com.saa.ejb.cxp.service.dto.ResumenTitularCartera;
import com.saa.ejb.cxp.service.dto.TotalesCartera;
import com.saa.rubros.TipoDocPagoAplicacion;

/**
 * Cálculo de cartera por pagar/por cobrar (docs/logica-negocio/cxp/API-CARTERA-CXP-CXC.md §3.3 y
 * §3.4), compartido entre {@code AplicacionPagoCxpServiceImpl} y {@code AplicacionPagoCxcServiceImpl}
 * para no duplicar la regla dos veces. Cada lado resuelve sus propias consultas P1-P4 (distintas
 * entidades, distintas condiciones de "vigente") y le entrega a esta clase filas ya agrupadas por
 * documento -- acá vive solo el cálculo, no el acceso a datos.
 *
 * <p>Public, no package-private: lo usa un paquete distinto ({@code ejb.cxc.serviceImpl}).</p>
 */
public final class CarteraCalculo {

    /** Misma tolerancia que usan los dos {@code AplicacionPago*ServiceImpl} para saldo (§3.3). */
    public static final double TOLERANCIA = 0.01;

    private CarteraCalculo() {
    }

    public static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /**
     * Calcula el documento de cartera de una fila de P1, o {@code null} si no entra al reporte
     * (§3.3: {@code |saldo| <= TOLERANCIA}).
     *
     * @param tipoDocumento    : "FACTURA", "NOTA_VENTA" o "LIQUIDACION" (§4)
     * @param idDocumento      : Id del documento (FCTC/LQCC/Factura -- OJO, numeraciones
     *                           independientes entre tipos, §6.6)
     * @param numEstablecimiento · @param numPtoEmision · @param secuencial : arman numeroDocumento
     * @param fechaEmision     : Fecha de emisión del documento
     * @param total            : Total del documento
     * @param intermediario    : true/false si aplica (FCTC), null si no (LQCC, CxC)
     * @param soportaCajaChica : true en CxP, false en CxC -- decide si {@code cajaChica} es 0.0 o null
     * @param idTitular · @param identificacion · @param razonSocial · @param nombre : titular del documento
     * @param aplicaciones     : filas de P2 de ESTE documento, {tipoDocPago, montoAplicado}
     * @param plazos           : filas de P3 de ESTE documento, {plazo, unidadTiempo}
     * @param fechaCorte       : fecha de corte del reporte
     * @param unidadesDesconocidas : unidades de tiempo no reconocidas, acumuladas para un solo
     *                           aviso por unidad al final del reporte (§3.4.1), no uno por documento
     * @param avisos           : avisos del reporte -- acá solo se agrega el de tipo de aplicación
     *                           desconocido (§3.3), que sí es uno por ocurrencia
     * @return                 : el documento, o null si no entra al reporte
     */
    public static DocumentoCartera calcularDocumento(String tipoDocumento, Long idDocumento,
            String numEstablecimiento, String numPtoEmision, String secuencial, LocalDate fechaEmision,
            double total, Boolean intermediario, boolean soportaCajaChica,
            Long idTitular, String identificacion, String razonSocial, String nombre,
            List<Object[]> aplicaciones, List<Object[]> plazos, LocalDate fechaCorte,
            Set<String> unidadesDesconocidas, List<String> avisos) {

        double pagado = 0.0, notasCredito = 0.0, retenciones = 0.0, anticipos = 0.0,
                notasDebito = 0.0, cajaChica = 0.0, aplicado = 0.0;
        for (Object[] fila : aplicaciones) {
            int tipoDocPago = ((Number) fila[0]).intValue();
            double monto = redondear(((Number) fila[1]).doubleValue());
            aplicado += monto;
            if (tipoDocPago == TipoDocPagoAplicacion.COBRO_DIRECTO) {
                pagado += monto;
            } else if (tipoDocPago == TipoDocPagoAplicacion.NOTA_CREDITO) {
                notasCredito += monto;
            } else if (tipoDocPago == TipoDocPagoAplicacion.RETENCION) {
                retenciones += monto;
            } else if (tipoDocPago == TipoDocPagoAplicacion.ANTICIPO) {
                anticipos += monto;
            } else if (tipoDocPago == TipoDocPagoAplicacion.NOTA_DEBITO) {
                notasDebito += Math.abs(monto);
            } else if (tipoDocPago == TipoDocPagoAplicacion.CAJA_CHICA) {
                cajaChica += monto;
            } else {
                avisos.add("Documento " + tipoDocumento + " " + idDocumento + ": tipo de aplicación "
                        + tipoDocPago + " no reconocido (fuera de 1-6) -- se sumó igual al aplicado "
                        + "para que el saldo coincida con /aplp/saldo o /aplc/saldo.");
            }
        }
        aplicado = redondear(aplicado);
        double totalRedondeado = redondear(total);
        double saldo = redondear(totalRedondeado - aplicado);

        if (Math.abs(saldo) <= TOLERANCIA) {
            return null;
        }

        Long plazoDias = plazoMaximoDias(plazos, unidadesDesconocidas);
        LocalDate fechaVencimiento = (plazoDias != null) ? fechaEmision.plusDays(plazoDias.longValue())
                : fechaEmision;
        long diasVencido = ChronoUnit.DAYS.between(fechaVencimiento, fechaCorte);
        String tramo = tramoDe(diasVencido);

        DocumentoCartera doc = new DocumentoCartera();
        doc.setTipoDocumento(tipoDocumento);
        doc.setIdDocumento(idDocumento);
        doc.setNumeroDocumento(nvl(numEstablecimiento) + "-" + nvl(numPtoEmision) + "-" + nvl(secuencial));
        doc.setFechaEmision(fechaEmision);
        doc.setPlazoDias(plazoDias);
        doc.setFechaVencimiento(fechaVencimiento);
        doc.setDiasVencido(diasVencido);
        doc.setTramo(tramo);
        doc.setIdTitular(idTitular);
        doc.setIdentificacion(identificacion);
        doc.setTitular((razonSocial != null && !razonSocial.trim().isEmpty()) ? razonSocial : nombre);
        doc.setTotal(totalRedondeado);
        doc.setPagado(pagado);
        doc.setNotasCredito(notasCredito);
        doc.setRetenciones(retenciones);
        doc.setAnticipos(anticipos);
        doc.setNotasDebito(notasDebito);
        doc.setCajaChica(soportaCajaChica ? Double.valueOf(cajaChica) : null);
        doc.setAplicado(aplicado);
        doc.setSaldo(saldo);
        doc.setSobrepagado(saldo < 0);
        doc.setIntermediario(intermediario);
        return doc;
    }

    /**
     * §3.4.1: el mayor de los plazos de P3 de un documento, convertido a días según
     * {@code unidadTiempo}. Plazo nulo o 0 se ignora (no cuenta como "0 días", cuenta como "sin
     * plazo" para esa fila). Una unidad que no encaja en ninguna regla se ignora y se acumula en
     * {@code unidadesDesconocidas} para un aviso único al final del reporte.
     */
    private static Long plazoMaximoDias(List<Object[]> plazos, Set<String> unidadesDesconocidas) {
        Long maximo = null;
        for (Object[] fila : plazos) {
            Number plazoNum = (Number) fila[0];
            if (plazoNum == null || plazoNum.longValue() == 0L) {
                continue;
            }
            String unidad = (String) fila[1];
            String unidadNorm = normalizarUnidad(unidad);
            long factor;
            if (unidadNorm.contains("DIA")) {
                factor = 1L;
            } else if (unidadNorm.contains("SEMANA")) {
                factor = 7L;
            } else if (unidadNorm.contains("MES")) {
                factor = 30L;
            } else if (unidadNorm.contains("ANIO") || unidadNorm.contains("ANO")) {
                factor = 365L;
            } else {
                unidadesDesconocidas.add(unidad);
                continue;
            }
            long dias = plazoNum.longValue() * factor;
            if (maximo == null || dias > maximo.longValue()) {
                maximo = Long.valueOf(dias);
            }
        }
        return maximo;
    }

    /**
     * ÍTEM 7 (2026-09-30, API-CARTERA-CXP-CXC.md §3.4.1): PGS.FPFM trae la unidad de tiempo con
     * dos grafías -- 'DIAS' y 'DÍAS' con tilde (51 filas medidas, con plazos de hasta 15 días) --
     * y {@code toUpperCase} solo no alcanza: {@code "DÍAS".contains("DIA")} da {@code false}. Se
     * quitan los diacríticos (NFD + eliminar la marca combinante) ANTES de comparar; el texto
     * ORIGINAL, con tilde, es el que se muestra en el aviso de unidad desconocida -- nunca el
     * normalizado.
     */
    private static String normalizarUnidad(String unidad) {
        if (unidad == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(unidad, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase(Locale.ROOT).replace(" ", "");
    }

    /** §3.4.4. */
    private static String tramoDe(long diasVencido) {
        if (diasVencido <= 0) {
            return "POR_VENCER";
        }
        if (diasVencido <= 30) {
            return "D1_30";
        }
        if (diasVencido <= 60) {
            return "D31_60";
        }
        if (diasVencido <= 90) {
            return "D61_90";
        }
        return "MAS_90";
    }

    /**
     * Arma el {@link ReporteCartera} final a partir de los documentos ya calculados: agrega los
     * avisos de unidad de tiempo desconocida (uno por unidad distinta, §3.4.1), arma el resumen
     * por titular ordenado por saldo DESC, los totales, y ordena los documentos por titular y
     * luego por fecha de emisión (§3.3/§4).
     */
    public static ReporteCartera armarReporte(String tipo, LocalDate fechaCorte,
            List<DocumentoCartera> documentos, Map<Long, Double> anticiposDisponiblesPorTitular,
            Set<String> unidadesDesconocidas, List<String> avisos) {

        for (String unidad : unidadesDesconocidas) {
            avisos.add("Unidad de tiempo de plazo no reconocida: '" + unidad + "' -- los plazos con "
                    + "esa unidad se ignoraron (el documento queda sin plazo, exigible desde su "
                    + "emisión).");
        }

        List<DocumentoCartera> ordenados = new ArrayList<>(documentos);
        Collections.sort(ordenados, new Comparator<DocumentoCartera>() {
            @Override
            public int compare(DocumentoCartera a, DocumentoCartera b) {
                int porTitular = compararNullable(a.getIdTitular(), b.getIdTitular());
                if (porTitular != 0) {
                    return porTitular;
                }
                return compararNullable(a.getFechaEmision(), b.getFechaEmision());
            }
        });

        Map<Long, ResumenTitularCartera> resumenPorTitular = new LinkedHashMap<>();
        TotalesCartera totales = new TotalesCartera();
        for (DocumentoCartera doc : ordenados) {
            ResumenTitularCartera resumen = resumenPorTitular.get(doc.getIdTitular());
            if (resumen == null) {
                resumen = new ResumenTitularCartera();
                resumen.setIdTitular(doc.getIdTitular());
                resumen.setIdentificacion(doc.getIdentificacion());
                resumen.setTitular(doc.getTitular());
                double anticiposDisponibles = (anticiposDisponiblesPorTitular != null
                        && anticiposDisponiblesPorTitular.containsKey(doc.getIdTitular()))
                        ? anticiposDisponiblesPorTitular.get(doc.getIdTitular()).doubleValue() : 0.0;
                resumen.setAnticiposDisponibles(anticiposDisponibles);
                resumenPorTitular.put(doc.getIdTitular(), resumen);
                totales.setTitulares(totales.getTitulares() + 1);
                totales.setAnticiposDisponibles(redondear(totales.getAnticiposDisponibles() + anticiposDisponibles));
            }
            acumular(resumen, doc);
            acumular(totales, doc);
        }

        List<ResumenTitularCartera> resumenes = new ArrayList<>(resumenPorTitular.values());
        for (ResumenTitularCartera resumen : resumenes) {
            resumen.setSaldoNeto(redondear(resumen.getSaldo() - resumen.getAnticiposDisponibles()));
        }
        Collections.sort(resumenes, new Comparator<ResumenTitularCartera>() {
            @Override
            public int compare(ResumenTitularCartera a, ResumenTitularCartera b) {
                return Double.compare(b.getSaldo(), a.getSaldo());
            }
        });
        totales.setSaldoNeto(redondear(totales.getSaldo() - totales.getAnticiposDisponibles()));

        ReporteCartera reporte = new ReporteCartera();
        reporte.setTipo(tipo);
        reporte.setFechaCorte(fechaCorte);
        reporte.setTotales(totales);
        reporte.setResumen(resumenes);
        reporte.setDocumentos(ordenados);
        reporte.setAvisos(avisos);
        return reporte;
    }

    private static void acumular(ResumenTitularCartera resumen, DocumentoCartera doc) {
        resumen.setDocumentos(resumen.getDocumentos() + 1);
        resumen.setTotal(redondear(resumen.getTotal() + doc.getTotal()));
        resumen.setAplicado(redondear(resumen.getAplicado() + doc.getAplicado()));
        resumen.setSaldo(redondear(resumen.getSaldo() + doc.getSaldo()));
        acumularTramo(resumen, doc);
    }

    private static void acumular(TotalesCartera totales, DocumentoCartera doc) {
        totales.setDocumentos(totales.getDocumentos() + 1);
        totales.setTotal(redondear(totales.getTotal() + doc.getTotal()));
        totales.setAplicado(redondear(totales.getAplicado() + doc.getAplicado()));
        totales.setSaldo(redondear(totales.getSaldo() + doc.getSaldo()));
        acumularTramo(totales, doc);
    }

    // §3.4.5: los importes por tramo suman el SALDO del documento, no una cantidad.
    private static void acumularTramo(ResumenTitularCartera resumen, DocumentoCartera doc) {
        switch (doc.getTramo()) {
            case "POR_VENCER": resumen.setPorVencer(redondear(resumen.getPorVencer() + doc.getSaldo())); break;
            case "D1_30": resumen.setD1a30(redondear(resumen.getD1a30() + doc.getSaldo())); break;
            case "D31_60": resumen.setD31a60(redondear(resumen.getD31a60() + doc.getSaldo())); break;
            case "D61_90": resumen.setD61a90(redondear(resumen.getD61a90() + doc.getSaldo())); break;
            default: resumen.setMas90(redondear(resumen.getMas90() + doc.getSaldo()));
        }
    }

    private static void acumularTramo(TotalesCartera totales, DocumentoCartera doc) {
        switch (doc.getTramo()) {
            case "POR_VENCER": totales.setPorVencer(redondear(totales.getPorVencer() + doc.getSaldo())); break;
            case "D1_30": totales.setD1a30(redondear(totales.getD1a30() + doc.getSaldo())); break;
            case "D31_60": totales.setD31a60(redondear(totales.getD31a60() + doc.getSaldo())); break;
            case "D61_90": totales.setD61a90(redondear(totales.getD61a90() + doc.getSaldo())); break;
            default: totales.setMas90(redondear(totales.getMas90() + doc.getSaldo()));
        }
    }

    private static int compararNullable(Long a, Long b) {
        if (a == null && b == null) { return 0; }
        if (a == null) { return -1; }
        if (b == null) { return 1; }
        return a.compareTo(b);
    }

    private static int compararNullable(LocalDate a, LocalDate b) {
        if (a == null && b == null) { return 0; }
        if (a == null) { return -1; }
        if (b == null) { return 1; }
        return a.compareTo(b);
    }

    private static String nvl(String valor) {
        return valor != null ? valor : "";
    }
}
