package com.saa.ejb.cxc.service;

import java.util.List;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Lote 12 (VCN #7): el servidor valida las líneas de RETENCIÓN EN LA FUENTE DEL IMPUESTO A LA RENTA (código de impuesto 1, tabla 608) y de IVA (código 2, tabla 20; la base es el IVA del documento: solo se exige base > 0 y valor = base × % ± 1 centavo) contra la tarifa activa de
 * CBR.TSRI (tabla 608 del SRI, vía CBR.LSRI), en vez de escribir en el XML lo que llega del navegador. El ISD (código 6) no se valida.
 * tarifas sembradas; sigue como hoy hasta que el contador valide la semilla. TSRI no tiene vigencia: se usa la tarifa ACTIVA (ESTADO = 1) de hoy.
 */
@Local
public interface TarifaRetencionService {

    /** Una línea a validar. {@code valor} es el que mandó el navegador. */
    final class Linea {
        public final String codImpuesto;
        public final String codRetencion;
        public final Double base;
        public final Double porcentaje;
        public final Double valor;
        public final String etiqueta;

        public Linea(String codImpuesto, String codRetencion, Double base, Double porcentaje, Double valor, String etiqueta) {
            this.codImpuesto = codImpuesto;
            this.codRetencion = codRetencion;
            this.base = base;
            this.porcentaje = porcentaje;
            this.valor = valor;
            this.etiqueta = etiqueta;
        }
    }

    /**
     * Valida las líneas y devuelve el valor a retener de cada una (mismo orden): el del navegador, o el recalculado por el servidor si difiere hasta
     * un centavo con el mismo porcentaje. Las líneas que no son de renta se devuelven sin tocar. Si alguna se rechaza (sin tarifa activa, varias
     * tarifas activas, porcentaje distinto de la tarifa, valor que difiere más de un centavo, base no positiva) lanza {@code IncomeException} con
     * TODAS las líneas rechazadas.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    List<Double> validarRenta(List<Linea> lineas) throws Exception;
}
