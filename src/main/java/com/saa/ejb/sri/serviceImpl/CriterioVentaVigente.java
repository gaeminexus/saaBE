package com.saa.ejb.sri.serviceImpl;

/**
 * ÍTEM 10, encargo 2026-09-09 (segunda tarea urgente del día, sobre el diagnóstico del ítem 9).
 * Criterio único para saber si un DOCUMENTO ELECTRÓNICO DE VENTA (Factura, NotaCredito,
 * NotaDebito, RetencionV2 — los cuatro que ASOPREP emite y somete al SRI) está vigente para
 * {@code <ventas>} del ATS y para los cuadres 103/104.
 *
 * <p><b>Por qué esta clase y no un literal repetido en cada consulta.</b> En estas cuatro
 * entidades la columna {@code estado} NO es el flag genérico activo/inactivo (0/1) que usa el
 * resto del sistema — incluido el lado COMPRA de los mismos procesos ({@code FacturaCompra},
 * {@code LiquidacionCompraCompra}, {@code NotaCreditoCompra}, {@code NotaDebitoCompra},
 * {@code DetalleRetencionCompraV2}, {@code DetalleRetencionV2}: esas SÍ usan
 * {@code Estado.ACTIVO}=1 tal cual — no tocar). Acá {@code estado} guarda el flujo de emisión
 * electrónica ante el SRI: 1=creada, 3=firmada, 4=enviada, <b>5=autorizada</b>, 6=no autorizada —
 * ver {@code FacturaServiceImpl.java:1516} ({@code factura.setEstado(5L)} al autorizar) y
 * {@code :1580} ({@code factura.setEstado(6L)} al rechazar); mismo patrón verificado en
 * {@code NotaCreditoServiceImpl}, {@code NotaDebitoServiceImpl} y {@code RetencionV2ServiceImpl}
 * (sus propios {@code setEstado(5L)}/{@code setEstado(6L)}). Filtrar por
 * {@code estado = Estado.ACTIVO} (=1) nunca encuentra un documento ya autorizado — es la causa
 * medida de que el ATS de agosto/2026 saliera con {@code <ventas>} vacío y de que los cuadres
 * 103/104 dieran cero del lado venta. Ver
 * {@code docs/logica-negocio/sri/DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md}.
 *
 * <p>Un documento vigente es el que está <b>autorizado</b> ({@code estado = 5}) y que
 * <b>no fue anulado después</b> ({@code estadoEmision <> 3}, ANULADA): un documento autorizado y
 * luego anulado se declara en {@code <anulados>}, no en {@code <ventas>} ni en los cuadres, y sin
 * este segundo filtro se contaría en los dos lados a la vez.
 *
 * <p>⚠️ <b>Este criterio es una recomendación del árbitro, todavía SIN CONFIRMAR contra la base al
 * 2026-09-09.</b> Correr
 * {@code docs/logica-negocio/sri/sql/e2-37-por-que-el-ats-de-agosto-salio-sin-ventas.sql}
 * (bloques 2 y 3) para verificar que no hay otro valor de {@code estado} en juego antes de dar
 * esto por definitivo. Si el script muestra algo distinto, el ajuste es de una línea: acá.
 */
final class CriterioVentaVigente {

    /** {@code estado = 5} (AUTORIZADA) — nunca {@code Estado.ACTIVO} para estas 4 entidades. */
    static final Long ESTADO_AUTORIZADA = Long.valueOf(5L);

    /** {@code estadoEmision = 3} (ANULADA) — excluir siempre del lado "vigente". */
    static final Long ESTADO_EMISION_ANULADA = Long.valueOf(3L);

    private CriterioVentaVigente() {
    }
}
