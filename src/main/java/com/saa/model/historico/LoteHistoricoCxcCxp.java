package com.saa.model.historico;

import java.util.ArrayList;
import java.util.List;

/**
 * Lote mensual de la carga histórica de CxC/CxP (C11, cuadre Superintendencia ASOPREP). Lo produce
 * {@code cuadreSuper/03-trabajo/cxc-cxp/scripts/07-genera-lotes.js} desde el control del contador y lo
 * consume {@code POST /SaaBE/rest/historico/cargarHistoricoCxcCxp}. Contrato completo en
 * docs/logica-negocio/cxp/CARGA-HISTORICA-CXC-CXP.md.
 *
 * <p>POJO de transporte (sin {@code @Entity}): campos públicos a propósito, es solo un contenedor de datos
 * que Jackson llena desde el JSON. Las fechas van como {@code yyyy-MM-dd}; los importes con punto decimal.</p>
 */
public class LoteHistoricoCxcCxp {

    /** Mes del lote, {@code AAAA-MM}. Define el período contable que debe existir. */
    public String mes;
    /** Etiqueta de la fuente, p. ej. "control asoprep 27-oct-2026": va en la observación de todo lo creado. */
    public String etiqueta;
    public Long idEmpresa;
    /** Usuario del SAA (SCP.PJRQ) con el que se graban documentos y pagos. Si viene nulo se usa el 38 ("usuario 1"). */
    public Long idUsuario;
    /** Nombre de usuario que queda en los asientos. */
    public String usuario;
    /** Observación base: "CARGA HISTORICA [etiqueta] mes AAAA-MM". Si viene vacía se arma aquí. */
    public String observacion;
    /** true = solo valida catálogos y reporta lo que haría, sin grabar nada. */
    public boolean soloValidar;
    /** true = carga también los documentos con {@code requiereConfirmacion = true} (cuenta inferida por regla). */
    public boolean aceptarInferidos;

    public List<TitularLote> titulares = new ArrayList<>();
    public List<CompraLote> compras = new ArrayList<>();
    public List<RetencionEmitidaLote> retencionesEmitidas = new ArrayList<>();
    public List<PagoLote> pagos = new ArrayList<>();
    public List<VentaLote> ventas = new ArrayList<>();
    public List<CobroLote> cobros = new ArrayList<>();
    public List<RetencionRecibidaLote> retencionesRecibidas = new ArrayList<>();
    public List<NotaCreditoCompraLote> ncCompras = new ArrayList<>();
    public List<NotaCreditoVentaLote> ncVentas = new ArrayList<>();

    /**
     * D-013: saldos iniciales de clientes y proveedores al 31-may-2025 cargados en los módulos como documentos
     * SIN asiento (la contabilidad ya está en el asiento de apertura). Solo viene en el lote 2025-05.
     */
    public SaldosInicialesLote saldosIniciales;

    /** Resumen que escribe el generador; el cargador no lo usa, solo lo devuelve. */
    public Object resumen;

    public static class SaldosInicialesLote {
        /** Fecha de corte de la apertura: 2025-05-31. Ningún documento de saldo inicial puede ser posterior. */
        public String fecha;
        /** CNT.ASNT.ASNTCDGO del asiento de apertura (12043): el cargador lee de sus líneas los saldos de 2.3.90.10 y 1.4.90.10 y exige que los documentos sumen exactamente eso. */
        public Long idAsientoApertura;
        /** Número visible del asiento de apertura (DI-A-2025-05-0001), solo para la observación. */
        public String asientoApertura;
        /** Titular genérico "SALDO INICIAL" (cliente y proveedor). Se crea/completa como cualquier titular del lote. */
        public TitularLote titularGenerico;
        /** Documentos por pagar al corte: los identificados a titulares reales y el genérico con el resto. */
        public List<DocumentoSaldoInicialLote> proveedores = new ArrayList<>();
        /** Documentos por cobrar al corte. */
        public List<DocumentoSaldoInicialLote> clientes = new ArrayList<>();
    }

    /**
     * Un documento de saldo inicial (PGS.FCTC si es proveedor, CBR.FCTR si es cliente) SIN líneas de detalle y SIN
     * asiento. Si {@code esGenerico} el titular es {@link SaldosInicialesLote#titularGenerico}; si no, el titular
     * real {@code identificacion} (que debe venir en {@code titulares} del lote o existir en el SAA).
     */
    public static class DocumentoSaldoInicialLote {
        public String ref;
        public boolean esGenerico;
        public String identificacion;
        /** Número del comprobante real (001-002-000000531) o, para el genérico, 999-999-000000001. */
        public String numero;
        /** Clave de acceso si se conoce; el genérico no lleva. */
        public String claveAcceso;
        /** Fecha de emisión (≤ fecha de corte); vacía = la fecha de corte. */
        public String fechaEmision;
        public Double monto;
        /** De dónde sale el monto: "mayor 149010 mayo-2025 fila 76", "apertura 2.3.90.10 menos identificados"... */
        public String origen;
    }

    // ─────────────────────────── tipos anidados ───────────────────────────

    public static class TitularLote {
        public String identificacion;
        /** RUC, CEDULA o PASAPORTE. */
        public String tipoIdentificacion;
        /** NATURAL o JURIDICA. */
        public String tipoPersona;
        public String razonSocial;
        public boolean esProveedor;
        public boolean esCliente;
        public Boolean existeEnSaa;
    }

    /** Una línea de documento: una por tarifa de IVA, con la cuenta contable y el centro de costo que manda el Excel. */
    public static class LineaLote {
        public String descripcion;
        public Double base;
        /** 0, 5, 8 o 15. */
        public Integer tarifa;
        /** Código de porcentaje de IVA del SRI (0, 2, 3, 4=15 %, 5=5 %, 6 no objeto, 7 exento, 8). */
        public Integer codigoIvaSri;
        public Double iva;
        /** Cuenta contable de movimiento con puntos (CNT.PLNN.PLNNCNTA). */
        public String cuenta;
        /** Número del centro de costo (CNT.CNCS.CNCSNMRO), p. ej. 1.01. Solo compras y NC de compra. */
        public String centroCosto;
    }

    public static class CompraLote {
        public String ref;
        public String claveAcceso;
        public String numero;
        public String fechaEmision;
        public String fechaAutorizacion;
        public String proveedor;
        public String razonSocial;
        public Double baseCero;
        public Double base5;
        public Double base15;
        public Double baseNoObjeto;
        public Double iva;
        public Double total;
        public Double diferenciaRedondeo;
        public String cuenta;
        public String nombreCuenta;
        public String origenCuenta;
        public boolean cuentaInferida;
        public boolean requiereConfirmacion;
        public String centroCosto;
        public boolean centroInferido;
        public List<LineaLote> lineas = new ArrayList<>();
        public String estadoPagoExcel;
        public String formaPagoExcel;
        public String tipoGasto;
        public String observacionExcel;
        public Object retencionEmitida;
        public Object pago;
        public Boolean pagoSinFecha;
        public String fechaPagoSugerida;
        public Boolean quedaPorPagar;
        public Boolean anuladaConNc;
    }

    public static class LineaRetencionLote {
        /** RENTA o IVA. */
        public String impuesto;
        /** Código del SRI: lista 608 (renta: 303, 304B, 320, 3440, ...) o lista 20 (IVA: 1, 2, 3, 9, 10). */
        public String codigoSri;
        public Double base;
        public Double porcentaje;
        public Double valor;
        public boolean codigoInferido;
    }

    public static class RetencionEmitidaLote {
        public String ref;
        public String numero;
        public String claveAcceso;
        public String fechaEmision;
        public String fechaAutorizacion;
        public String proveedor;
        public String razonSocial;
        public String facturaNumero;
        public String facturaClave;
        public String facturaFecha;
        /** MM/AAAA. */
        public String periodoFiscal;
        public List<LineaRetencionLote> lineas = new ArrayList<>();
        public Double total;
        public String fuente;
    }

    public static class PagoLote {
        public String ref;
        public String facturaClave;
        public String facturaNumero;
        public String facturaMes;
        public String proveedor;
        public String razonSocial;
        public String fechaPago;
        /** TRANSFERENCIA o DEBITO_AUTOMATICO. */
        public String formaPago;
        public String formaPagoExcel;
        public String cuentaBancaria;
        public Long idCuentaBancaria;
        public String banco;
        public Double valor;
        public Double totalFactura;
        public Double retenido;
        public String referencia;
        public String retencionEnOtroMes;
    }

    public static class VentaLote {
        public String ref;
        public String claveAcceso;
        public String numero;
        public String fechaEmision;
        public String fechaAutorizacion;
        public String cliente;
        public String razonSocial;
        public Double base15;
        public Double base0;
        public Double iva;
        public Double total;
        public String cuentaIngreso;
        public List<LineaLote> lineas = new ArrayList<>();
        public String estadoCobroExcel;
        public String medioPagoExcel;
        public Object retencionRecibida;
        public Object cobro;
        public Boolean cobroSinFecha;
        public Boolean quedaPorCobrar;
        public String cobradaConNc;
    }

    public static class CobroLote {
        public String ref;
        public String facturaClave;
        public String facturaNumero;
        public String facturaMes;
        public String cliente;
        public String razonSocial;
        public String fechaCobro;
        public String cuentaBancaria;
        public Long idCuentaBancaria;
        public String banco;
        public Double valor;
        public Double totalFactura;
        public Double retenido;
        public String referencia;
        public String estadoCobroExcel;
    }

    public static class RetencionRecibidaLote {
        public String ref;
        public String numero;
        public String claveAcceso;
        public String fechaEmision;
        public String fechaAutorizacion;
        public String cliente;
        public String razonSocial;
        public String facturaNumero;
        public String facturaClave;
        public String facturaMes;
        public String periodoFiscal;
        public List<LineaRetencionLote> lineas = new ArrayList<>();
        public Double total;
    }

    public static class NotaCreditoCompraLote {
        public String ref;
        public String claveAcceso;
        public String numero;
        public String fechaEmision;
        public String fechaAutorizacion;
        public String proveedor;
        public String razonSocial;
        public String facturaAfectadaNumero;
        public String facturaAfectadaClave;
        public String facturaAfectadaMes;
        public Double baseCero;
        public Double base5;
        public Double base15;
        public Double iva;
        public Double total;
        public String cuenta;
        public String nombreCuenta;
        public String origenCuenta;
        public String centroCosto;
        public List<LineaLote> lineas = new ArrayList<>();
    }

    public static class NotaCreditoVentaLote {
        public String ref;
        public String claveAcceso;
        public String numero;
        public String fechaEmision;
        public String fechaAutorizacion;
        public String cliente;
        public String razonSocial;
        public String facturaAfectadaNumero;
        public String facturaAfectadaClave;
        public String facturaAfectadaMes;
        public Double base15;
        public Double base0;
        public Double iva;
        public Double total;
        public String cuentaIngreso;
        public List<LineaLote> lineas = new ArrayList<>();
    }
}
