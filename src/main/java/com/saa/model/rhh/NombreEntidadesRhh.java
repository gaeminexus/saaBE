package com.saa.model.rhh;

/**
 * <p>Interface que contiene constantes con
 * los nombres de las Entidades del módulo RHH.</p>
 */
public interface NombreEntidadesRhh {

    String ANEXO_CONTRATO        = "AnexoContrato"; 
    String APORTES_RETENCIONES   = "AportesRetenciones";
    String CARGO                 = "Cargo";
    String CATALOGO              = "Catalogo";
    String CONTRATO_EMPLEADO     = "ContratoEmpleado";
    String DEPARTAMENTO		     = "Departamento";
    String DEPARTAMENTO_CARGO    = "DepartamentoCargo";
    String DETALLE_LIQUIDACION   = "DetalleLiquidacion";
    String DETALLE_TURNO         = "DetalleTurno";
    String EMPLEADO              = "Empleado";
    String HISTORIAL             = "Historial";
    String LIQUIDACION           = "Liquidacion";
    String MARCACIONES           = "Marcaciones";
    String NOMINA                = "Nomina";
    String PERIODO_NOMINA        = "PeriodoNomina";
    String PETICIONES            = "Peticiones";
    String REGLON_NOMINA         = "ReglonNomina";
    String RESUMEN_NOMINA        = "ResumenNomina";
    String ROL_PAGO              = "RolPago";
    String SALDO_VACACIONES      = "SaldoVacaciones";
    String SOLICITUD_VACACIONES  = "SolicitudVacaciones";
    String DETALLE_CONSUMO_VACACIONES = "DetalleConsumoVacaciones";
    String TIPO_CONTRATO_EMPLEADO= "TipoContratoEmpleado";
    String TURNO                 = "Turno";

    /* ---------- Fase 1: parametria de nomina ----------
     * El valor debe coincidir EXACTAMENTE con el prefijo de los @NamedQuery
     * de la entidad, porque EntityDaoImpl resuelve la consulta concatenando
     * el valor + "All" / + "Id". Un desajuste falla en runtime, no al compilar. */
    String CONCEPTO_NOMINA           = "ConceptoNomina";
    String CONFIGURACION_NOMINA      = "ConfiguracionNomina";
    String PARAMETRO_NOMINA          = "ParametroNomina";
    String TABLA_IMPUESTO_RENTA      = "TablaImpuestoRenta";
    String TOPE_GASTO_PERSONAL       = "TopeGastoPersonal";
    String CAUSAL_TERMINACION        = "CausalTerminacion";
    String FORMATO_ARCHIVO_MARCACION = "FormatoArchivoMarcacion";
    String DETALLE_FORMATO_MARCACION = "DetalleFormatoMarcacion";

    /* ---------- Fase 2: maestro de personal ---------- */
    String CARGA_FAMILIAR             = "CargaFamiliar";
    String CUENTA_BANCARIA_EMPLEADO   = "CuentaBancariaEmpleado";
    String GASTO_PERSONAL_PROYECTADO  = "GastoPersonalProyectado";
    String CONCEPTO_FIJO_EMPLEADO     = "ConceptoFijoEmpleado";
    String NOVEDAD_IESS               = "NovedadIess";

    /* ---------- Fase 3: migracion de apertura ---------- */
    String ACUMULADO_NOMINA      = "AcumuladoNomina";
    String DESCUENTO_RECURRENTE  = "DescuentoRecurrente";
    String CUOTA_DESCUENTO       = "CuotaDescuento";
    String SALDO_APERTURA        = "SaldoApertura";

    /* ---------- Fase 4: motor de calculo ---------- */
    String NOVEDAD_NOMINA                = "NovedadNomina";
    String PROVISION_NOMINA              = "ProvisionNomina";
    String PROYECCION_IMPUESTO_RENTA     = "ProyeccionImpuestoRenta";
    String LIQUIDACION_BENEFICIO_SOCIAL  = "LiquidacionBeneficioSocial";
    String HORA_EXTRA                    = "HoraExtra";

    // ================= FASE 6: ORDEN DE PAGO =================
    String ORDEN_PAGO_NOMINA             = "OrdenPagoNomina";
    String DETALLE_ORDEN_PAGO_NOMINA     = "DetalleOrdenPagoNomina";
    String FORMATO_ARCHIVO_BANCARIO      = "FormatoArchivoBancario";
    String DETALLE_FORMATO_BANCARIO      = "DetalleFormatoBancario";

    // ================= FASE 7: ASISTENCIA =================
    String CARGA_MARCACIONES             = "CargaMarcaciones";

    // ================= FASE 9: SALIDAS OFICIALES Y UTILIDADES =================
    String UTILIDAD                      = "Utilidad";
    String DETALLE_UTILIDAD              = "DetalleUtilidad";
    String SALIDA_OFICIAL                = "SalidaOficial";

    // ================= ANTICIPOS A TRABAJADORES =================
    String ANTICIPO_EMPLEADO             = "AnticipoEmpleado";

}
