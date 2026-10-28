package com.saa.ejb.historico.service;

import com.saa.model.historico.LoteHistoricoNomina;
import com.saa.model.historico.ResultadoCargaHistorica;

import jakarta.ejb.Local;

/**
 * Cargador histórico de nómina (C13, cuadre Superintendencia ASOPREP, decisión D-012): reconstruye en el SAA, mes a
 * mes, el rol del contador en el módulo de nómina (empleados, período, nóminas, renglones, provisiones, órdenes de
 * pago, planillas IESS) y su contabilidad con los motores del SAA, sin recalcular. Orquestador sin transacción: cada
 * paso corre en la suya ({@link CargaHistoricaNominaPasoService}). Ver docs/logica-negocio/rhh/CARGA-HISTORICA-NOMINA.md.
 */
@Local
public interface CargaHistoricaNominaService {

    /**
     * Procesa un lote mensual en el orden empleados → período → asiento de rol → asiento de provisiones → pagos →
     * planillas IESS → cierre. Idempotente: un período ya cargado se retoma donde quedó.
     * @param lote      : Lote mensual (ver contrato)
     * @return          : Resultado por paso y contadores (mismo contrato que la carga de CxC/CxP)
     * @throws Throwable: Solo por errores de la petición; los errores de cada paso van en el resultado
     */
    ResultadoCargaHistorica cargar(LoteHistoricoNomina lote) throws Throwable;
}
