package com.saa.ejb.historico.service;

import com.saa.model.historico.LoteHistoricoCxcCxp;
import com.saa.model.historico.ResultadoCargaHistorica;

import jakarta.ejb.Local;

/**
 * Cargador histórico de CxC/CxP (C11, cuadre Superintendencia ASOPREP): reconstruye en el SAA, por los
 * módulos y con sus motores contables, las compras, retenciones, pagos, ventas, cobros y notas de crédito
 * del control del contador (jun-2025 en adelante). Orquestador sin transacción: cada documento del lote se
 * procesa en su propia transacción ({@link CargaHistoricaDocumentoService}), así un documento mal no tumba
 * el lote. Ver docs/logica-negocio/cxp/CARGA-HISTORICA-CXC-CXP.md.
 */
@Local
public interface CargaHistoricaCxcCxpService {

    /**
     * Procesa un lote mensual en el orden titulares → compras → retenciones emitidas → pagos → ventas →
     * retenciones recibidas → cobros → NC de compra → NC de venta. Idempotente por clave de acceso.
     * @param lote      : Lote mensual (ver contrato)
     * @return          : Resultado por documento y contadores
     * @throws Throwable: Solo por errores de la petición; los errores de documento van en el resultado
     */
    ResultadoCargaHistorica cargar(LoteHistoricoCxcCxp lote) throws Throwable;

    /**
     * D-013: reasigna parte del saldo inicial (titular SALDO INICIAL) a un titular real. Devuelve un resultado con un
     * solo ítem (CREADO / OMITIDO / RECHAZADO).
     */
    ResultadoCargaHistorica reasignarSaldoInicial(com.saa.model.historico.ReasignacionSaldoInicial reasignacion) throws Throwable;
}
