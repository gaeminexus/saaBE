package com.saa.rubros;

/**
 * Rubro {@link Rubros#CNT_PARAMETROS_ASIENTOS_AUTOMATICOS} (249): vigencia de la regla de tipos de asiento
 * automaticos IN-A / EG-A / DI-A y numeracion por modulo (docs/logica-negocio/cnt/DISENO-TIPOS-ASIENTO-AUTOMATICOS.md
 * §7).
 * <p>
 * Cada detalle es una empresa: {@code PDTRALTR} = {@code PJRQCDGO} de la empresa (o
 * {@link #VIGENCIA_TODAS_LAS_EMPRESAS} para el valor comun), {@code PDTRVLRV} = fecha de vigencia en formato
 * {@code yyyy-MM-dd}, {@code PDTRESTD} = 1. La lectura busca primero el detalle de la empresa, despues el 0 y, si no
 * hay ninguno o la fecha no se puede leer, usa {@link TipoAsientoAutomatico#FECHA_VIGENCIA_DEFECTO} (2026-11-01):
 * sin configurar, el sistema se comporta exactamente igual que antes de este rubro.
 * <p>
 * Script que lo crea para la empresa 1236 con vigencia 2025-06-01 (decision D-006 del cuadre con la
 * Superintendencia): docs/logica-negocio/cnt/sql/c12-01-vigencia-tipos-automaticos.sql.
 */
public interface CntParametroAsientoAutomatico {

	/** Codigo alterno del detalle que vale para toda empresa sin detalle propio. */
	int VIGENCIA_TODAS_LAS_EMPRESAS = 0;

}
