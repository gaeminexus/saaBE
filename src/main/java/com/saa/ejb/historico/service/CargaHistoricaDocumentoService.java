package com.saa.ejb.historico.service;

import java.util.List;

import com.saa.model.historico.LoteHistoricoCxcCxp;
import com.saa.model.historico.ResultadoCargaHistorica.ItemResultado;

import jakarta.ejb.Local;

/**
 * Trabajo por documento de la carga histórica de CxC/CxP (C11). Cada método corre en su propia transacción
 * ({@code REQUIRES_NEW}) y, ante cualquier error, marca rollback y relanza: el orquestador
 * ({@link CargaHistoricaCxcCxpService}) lo convierte en un renglón RECHAZADO del resultado. Todos reciben el
 * {@link Contexto} del lote (empresa, usuario, observación base, banderas).
 */
@Local
public interface CargaHistoricaDocumentoService {

    /** Datos del lote que necesita cada documento. */
    class Contexto {
        public Long idEmpresa;
        public Long idUsuario;
        public String usuario;
        public String mes;
        public String observacion;
        public boolean soloValidar;
        public boolean aceptarInferidos;
    }

    /**
     * Valida lo que debe existir antes de tocar el lote: período contable del mes abierto, facturador de la
     * empresa, usuario, centros de costo y cuentas de las líneas, cuentas bancarias de pagos y cobros.
     * @return lista de errores bloqueantes (vacía si se puede procesar)
     */
    List<String> validarLote(Contexto ctx, LoteHistoricoCxcCxp lote) throws Throwable;

    ItemResultado cargarTitular(Contexto ctx, LoteHistoricoCxcCxp.TitularLote t) throws Throwable;

    ItemResultado cargarCompra(Contexto ctx, LoteHistoricoCxcCxp.CompraLote c) throws Throwable;

    ItemResultado cargarRetencionEmitida(Contexto ctx, LoteHistoricoCxcCxp.RetencionEmitidaLote r) throws Throwable;

    ItemResultado cargarPago(Contexto ctx, LoteHistoricoCxcCxp.PagoLote p) throws Throwable;

    ItemResultado cargarVenta(Contexto ctx, LoteHistoricoCxcCxp.VentaLote v) throws Throwable;

    ItemResultado cargarCobro(Contexto ctx, LoteHistoricoCxcCxp.CobroLote c) throws Throwable;

    ItemResultado cargarRetencionRecibida(Contexto ctx, LoteHistoricoCxcCxp.RetencionRecibidaLote r) throws Throwable;

    ItemResultado cargarNotaCreditoCompra(Contexto ctx, LoteHistoricoCxcCxp.NotaCreditoCompraLote n) throws Throwable;

    ItemResultado cargarNotaCreditoVenta(Contexto ctx, LoteHistoricoCxcCxp.NotaCreditoVentaLote n) throws Throwable;

    /**
     * D-013: crea el documento de saldo inicial (factura de compra si {@code lado} = PROVEEDOR, de venta si
     * CLIENTE) con fecha ≤ corte, SIN líneas de detalle y SIN asiento, para el titular genérico SALDO INICIAL o
     * para el titular real identificado. Idempotente por empresa, titular y número.
     */
    ItemResultado cargarSaldoInicial(Contexto ctx, String lado, LoteHistoricoCxcCxp.SaldosInicialesLote si,
            LoteHistoricoCxcCxp.DocumentoSaldoInicialLote d) throws Throwable;

    /**
     * D-013: mueve {@code monto} del documento genérico de SALDO INICIAL a un documento del titular real (lo crea
     * igual que {@link #cargarSaldoInicial}), sin tocar la contabilidad. Idempotente por titular y número destino.
     */
    ItemResultado reasignarSaldoInicial(Contexto ctx, com.saa.model.historico.ReasignacionSaldoInicial r) throws Throwable;
}
