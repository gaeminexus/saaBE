package com.saa.ejb.cxp.serviceImpl;

/**
 * BE-6 (PLAN-CLASIFICACION-POR-TARIFA-COMPRAS.md): acumula la base de un documento de compra por
 * bucket de tarifa, clasificando por {@code codigoPorcentaje} (Tabla 17 del SRI). ES LA UNICA REGLA
 * de reparto: factura, nota de credito, nota de debito y liquidacion (ProcesoCargaDocumentosServiceImpl)
 * y el registro manual de nota de venta (FacturaCompraServiceImpl, BE-8) la usan, cada una recorriendo
 * su detalle una vez.
 * <ul>
 * <li>0 -> tarifa 0% &middot; 6 -> no objeto &middot; 7 -> exento &middot; 5 -> 5% &middot; 8 -> 8%.</li>
 * <li>2, 3, 4, 10 (12%, 14%, 15%, 13%) y cualquier otro codigo: sin bucket propio; quedan en la
 * gravada, que es la resta (SUBTOTAL menos los cinco buckets).</li>
 * <li>{@code null} NO es gravado: es "no se sabe". No entra a ningun bucket y se traza; su base
 * sigue dentro de SUBTOTAL.</li>
 * </ul>
 */
class BasesPorTarifa {
    private final String etiqueta;
    private double base0, noObjeto, exenta, base5, base8;

    BasesPorTarifa(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * BE-8: {@code true} si el codigo NO traslada IVA (0%, no objeto, exento: 0, 6, 7). Cualquier otro
     * codigo (incluidos 5 y 8, que si cobran IVA) es gravado. Vive aca para que la lista de codigos sin
     * IVA no se copie en otro lado.
     */
    static boolean sinIva(long codigoPorcentaje) {
        return codigoPorcentaje == 0L || codigoPorcentaje == 6L || codigoPorcentaje == 7L;
    }

    /** @param indice posicion de la linea (o grupo de cabecera) en el XML, solo para la traza */
    void acumular(int indice, Long codigoPorcentaje, double base) {
        if (codigoPorcentaje == null) {
            System.out.println("⚠ " + etiqueta + ": linea " + indice + " sin codigoPorcentaje (base "
                    + String.format(java.util.Locale.US, "%.2f", base) + "): no se suma a ningun bucket de "
                    + "tarifa; queda dentro de SUBTOTAL y el ATS la tomaria como gravada. Revisar el XML.");
            return;
        }
        switch (codigoPorcentaje.intValue()) {
            case 0: base0 += base; break;
            case 6: noObjeto += base; break;
            case 7: exenta += base; break;
            case 5: base5 += base; break;
            case 8: base8 += base; break;
            default: break; // 2, 3, 4, 10: gravada, por resta
        }
    }

    boolean hayBase() {
        return base0 > 0.0 || noObjeto > 0.0 || exenta > 0.0 || base5 > 0.0 || base8 > 0.0;
    }

    // NUMBER en la base: se redondea aca para que el valor en memoria sea el que queda grabado.
    double base0() { return r2(base0); }
    double noObjeto() { return r2(noObjeto); }
    double exenta() { return r2(exenta); }
    double base5() { return r2(base5); }
    double base8() { return r2(base8); }

    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }
}
