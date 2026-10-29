package com.saa.rubros;

/**
 * Detalles del rubro {@link Rubros#CNT_PARAMETROS_ESTADOS_FINANCIEROS} (250): PDTRALTR = estas constantes,
 * valor en PDTRVLRV. Semilla: docs/logica-negocio/rpr/sql/C19-04-estados-financieros-oficiales.sql.
 */
public interface CntParametroEstadoFinanciero {
	/** Razon social que encabeza los estados (por defecto, el nombre de la empresa en SCP.PJRQ). */
	int RAZON_SOCIAL = 1;
	/** RUC de la entidad (sin el, la linea de RUC no se imprime). */
	int RUC = 2;
	/** Nombre del representante legal (primera firma; en blanco = se firma a mano). */
	int NOMBRE_REPRESENTANTE = 3;
	/** Cargo de la primera firma (por defecto GERENTE GENERAL). */
	int CARGO_REPRESENTANTE = 4;
	/** Nombre del contador general (segunda firma). */
	int NOMBRE_CONTADOR = 5;
	/** Cargo de la segunda firma (por defecto CONTADOR GENERAL). */
	int CARGO_CONTADOR = 6;
	/** Registro profesional del contador (CPA). */
	int REGISTRO_CONTADOR = 7;
	/** Cuenta de nivel 1 de los ingresos (FCPC: 5). */
	int GRUPO_INGRESOS = 8;
	/** Cuenta de nivel 1 de los gastos (FCPC: 4). */
	int GRUPO_GASTOS = 9;
	/** Cuenta de nivel 1 del activo (1). */
	int GRUPO_ACTIVO = 10;
	/** Cuenta de nivel 1 del pasivo (2). */
	int GRUPO_PASIVO = 11;
	/** Cuenta de nivel 1 del patrimonio (3). */
	int GRUPO_PATRIMONIO = 12;
	/** Ruta de archivo del logo oficial (png/jpg) en el servidor; vacio = el logo empaquetado /rep/img/logo_aso.jpeg. */
	int RUTA_LOGO = 13;
	/** Digitos maximos del codigo de cuenta en los estados oficiales (Res. SBS-2013-0507: 6). */
	int NIVEL_MAXIMO = 14;
	/** Linea de moneda bajo el periodo. */
	int TEXTO_MONEDA = 15;
}
