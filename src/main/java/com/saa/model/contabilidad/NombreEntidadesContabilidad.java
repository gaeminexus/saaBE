/**
 * Copyright o Gaemi Soft Coa. Ltda. , 2011 Reservados todos los derechos  
 * Joso Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa esto protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproduccion o la distribucion no autorizadas de este programa, o de cualquier parte del mismo, 
 * esto penada por la ley y con severas sanciones civiles y penales, y sero objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informacion confidencial y se utilizaro solo en  conformidad  
 * con los torminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.model.contabilidad;

/**
 * <p>
 * Interface que contiene constantes con
 * Nombre de las Entidades del Sistema.
 * </p>
 */
public interface NombreEntidadesContabilidad {
    // Entidades principales
    String ANIO_MOTOR = "AnioMotor";
    String ASIENTO = "Asiento";
    String CENTRO_COSTO = "CentroCosto";
    String PLAN_CUENTA = "PlanCuenta";
    String MAYORIZACION = "Mayorizacion";
    String TIPO_ASIENTO = "TipoAsiento";
    String REPORTE_CONTABLE = "ReporteContable";
    String PERIODO = "Periodo";
    String NATURALEZA_CUENTA = "NaturalezaCuenta";
    String MAYOR_ANALITICO = "MayorAnalitico";
    String PLANTILLA = "Plantilla";
    String TEMP_REPORTES = "TempReportes";
    String DESGLOSE_MAYORIZACION_CC = "DesgloseMayorizacionCC";
    String DETALLE_ASIENTO = "DetalleAsiento";
    String DETALLE_MAYORIZACION_CC = "DetalleMayorizacionCC";
    String DETALLE_MAYORIZACION = "DetalleMayorizacion";
    
    // Entidades históricos
    String HIST_ASIENTO = "HistAsiento";
    String HIST_MAYORIZACION = "HistMayorizacion";
    String HIST_DETALLE_ASIENTO = "HistDetalleAsiento";
    String HIST_DETALLE_MAYORIZACION = "HistDetalleMayorizacion";
    
    // Entidades de centro de costo
    String MAYORIZACION_CC = "MayorizacionCC";
    String REPORTE_CUENTA_CC = "ReporteCuentaCC";
    String DETALLE_REPORTE_CUENTA_CC = "DetalleReporteCuentaCC";
    String SALDOS = "Saldos";
    
    // Entidades de detalle
    String DETALLE_PLANTILLA = "DetallePlantilla";
    String DETALLE_REPORTE_CONTABLE = "DetalleReporteContable";
    String DETALLE_MAYOR_ANALITICO = "DetalleMayorAnalitico";
    
    // Entidades de relación
    String MATCH_CUENTA = "MatchCuenta";
}
