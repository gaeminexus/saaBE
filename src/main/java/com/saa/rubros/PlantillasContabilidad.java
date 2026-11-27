package com.saa.rubros;

/**
 * C52: plantillas contables propias del modulo de contabilidad (CNT.PLNS, codigo alterno). El alterno 40 se reservo en
 * docs/logica-negocio/REGISTRO-RESERVAS-EQUIPOS.md §2c (los 1 a 39 estan ocupados por Plantillas*.java).
 */
public interface PlantillasContabilidad {

    /** «CNT REGULARIZACION SALDOS CONTRARIOS»: cuentas de la regularizacion de redondeos y de las propuestas. Semilla: cnt/sql/415. */
    int REGULARIZACION_SALDOS = 40;

    /** Papel 1 (DEBE), aux2 = 1, 2, ...: cuenta de redondeo de gasto con saldo acreedor que se lleva a otros ingresos. */
    int REDONDEO_ORIGEN = 1;
    /** Papel 2 (HABER), aux2 = 0: otros ingresos, destino de los redondeos. */
    int OTROS_INGRESOS_DESTINO = 2;
    /** Papel 3 (DEBE), aux2 = 0: contrapartida sugerida de un pasivo/patrimonio con saldo deudor (se reclasifica a un activo). */
    int CONTRA_PASIVO_DEUDOR = 3;
    /** Papel 4 (HABER), aux2 = 0: contrapartida sugerida de un activo con saldo acreedor (se reclasifica a un pasivo). */
    int CONTRA_ACTIVO_ACREEDOR = 4;
}
