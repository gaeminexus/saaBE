package com.saa.ejb.historico.service;

import java.util.List;

import com.saa.model.historico.LoteHistoricoNomina;
import com.saa.model.historico.ResultadoCargaHistorica.ItemResultado;

import jakarta.ejb.Local;

/**
 * Pasos de la carga histórica de nómina (C13). Cada método corre en su propia transacción (REQUIRES_NEW): si falla,
 * se deshace solo ese paso y el orquestador ({@link CargaHistoricaNominaService}) lo informa como RECHAZADO.
 * Ver docs/logica-negocio/rhh/CARGA-HISTORICA-NOMINA.md.
 */
@Local
public interface CargaHistoricaNominaPasoService {

    /** Datos de la corrida que comparten todos los pasos. */
    class Contexto {
        public Long idEmpresa;
        public Long idUsuario;
        public String usuario;
        public String mes;
        public String observacion;
        public boolean soloValidar;
        public boolean aceptarInferidos;
        public String centroCosto;
    }

    /** Empresa, usuario, períodos contables, conceptos, cuentas bancarias y cuentas contables del lote. */
    List<String> validarLote(Contexto ctx, LoteHistoricoNomina lote) throws Throwable;

    /** Crea el empleado y su contrato si no existen (por cédula); si existe, lo deja como está. */
    ItemResultado cargarEmpleado(Contexto ctx, LoteHistoricoNomina.EmpleadoLote e) throws Throwable;

    /**
     * Crea el período de nómina del mes (modo productivo, mensual) con sus nóminas, renglones y provisiones con los
     * valores del lote, y lo deja APROBADO. Si ya existe un período del mes con la traza de la carga, lo devuelve
     * OMITIDO (con su id); si existe sin la traza, RECHAZADO.
     */
    ItemResultado cargarPeriodo(Contexto ctx, LoteHistoricoNomina lote) throws Throwable;

    /** Asiento de rol con el motor ({@code contabilizarRol}), centro de costo en las líneas de gasto y traza. */
    ItemResultado contabilizarRol(Contexto ctx, Long idPeriodo) throws Throwable;

    /** Asiento de provisiones con el motor ({@code contabilizarProvisiones}), centro de costo y traza. */
    ItemResultado contabilizarProvisiones(Contexto ctx, Long idPeriodo) throws Throwable;

    /** Orden de pago con su detalle y asiento de pago del motor ({@code contabilizarPago}) con la fecha del banco. */
    ItemResultado cargarPago(Contexto ctx, Long idPeriodo, LoteHistoricoNomina.PagoLote p) throws Throwable;

    /** Planilla IESS (PLIS/DLIS) y, si el lote trae su pago, el asiento del débito bancario. */
    ItemResultado cargarPlanillaIess(Contexto ctx, Long idPeriodo, LoteHistoricoNomina.PlanillaIessLote p) throws Throwable;

    /** Cierre del período con el motor ({@code cerrarPeriodo}: acumulados ACMN) si quedó PAGADO. */
    ItemResultado cerrarPeriodo(Contexto ctx, Long idPeriodo) throws Throwable;
}
