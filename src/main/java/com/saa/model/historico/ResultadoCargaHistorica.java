package com.saa.model.historico;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de {@code POST /SaaBE/rest/historico/cargarHistoricoCxcCxp}: un renglón por documento del lote
 * con su estado (CREADO, OMITIDO por idempotencia, RECHAZADO con causa, VALIDADO en modo soloValidar) y los
 * contadores. {@code erroresLote} trae lo que impide procesar el lote entero (período, facturador, catálogos).
 */
public class ResultadoCargaHistorica {

    public static final String CREADO = "CREADO";
    public static final String OMITIDO = "OMITIDO";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String VALIDADO = "VALIDADO";

    public String mes;
    public boolean exito;
    public boolean soloValidar;
    public List<String> erroresLote = new ArrayList<>();
    public List<String> avisosLote = new ArrayList<>();
    public int creados;
    public int omitidos;
    public int rechazados;
    public int validados;
    public List<ItemResultado> items = new ArrayList<>();

    public static class ItemResultado {
        /** titular, compra, retencion-emitida, pago, venta, cobro, retencion-recibida, nc-compra, nc-venta. */
        public String tipo;
        public String ref;
        public String documento;
        public String estado;
        public String mensaje;
        public Long id;
        public Long idAsiento;
        public String asiento;

        public ItemResultado() {
        }

        public ItemResultado(String tipo, String ref, String documento) {
            this.tipo = tipo;
            this.ref = ref;
            this.documento = documento;
        }

        public ItemResultado creado(Long id, Long idAsiento, String asiento, String mensaje) {
            this.estado = CREADO;
            this.id = id;
            this.idAsiento = idAsiento;
            this.asiento = asiento;
            this.mensaje = mensaje;
            return this;
        }

        public ItemResultado omitido(Long id, String mensaje) {
            this.estado = OMITIDO;
            this.id = id;
            this.mensaje = mensaje;
            return this;
        }

        public ItemResultado rechazado(String mensaje) {
            this.estado = RECHAZADO;
            this.mensaje = mensaje;
            return this;
        }

        public ItemResultado validado(String mensaje) {
            this.estado = VALIDADO;
            this.mensaje = mensaje;
            return this;
        }
    }

    public void agregar(ItemResultado item) {
        items.add(item);
        if (CREADO.equals(item.estado)) creados++;
        else if (OMITIDO.equals(item.estado)) omitidos++;
        else if (RECHAZADO.equals(item.estado)) rechazados++;
        else if (VALIDADO.equals(item.estado)) validados++;
    }
}
