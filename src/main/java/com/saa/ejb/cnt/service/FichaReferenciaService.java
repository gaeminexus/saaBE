package com.saa.ejb.cnt.service;

import java.util.Map;

import jakarta.ejb.Local;

/**
 * C25b — Ficha de solo lectura de cualquier elemento que se cite en la pantalla de Seguimiento Novedades (asiento,
 * préstamo u operación, pago de préstamo, entidad, cobro, fila de extracto, carga Petro, corrida de cierre de cartera,
 * cuenta contable, documento de CxP o CxC, empleado, ejecución de estructuras G, decisión y novedad).
 *
 * <p>Existe para que toda referencia de la pantalla sea un enlace aunque el módulo dueño del elemento no tenga una
 * pantalla que abra un registro por id: C25 no modifica pantallas de otros módulos. Cuando sí existe esa pantalla, la
 * ficha trae su ruta ({@code ruta}) para ofrecer "Abrir en el módulo". Solo SELECT.</p>
 *
 * <p>Respuesta: {@code tipo, valor, encontrado, titulo, subtitulo, mensaje, campos[{etiqueta, valor, ref?{tipo, valor}}],
 * tablas[{titulo, columnas[], refs{columna: tipo}, filas[[...]], total}], ruta?{path[], queryParams{}}, idNovedad?}.</p>
 */
@Local
public interface FichaReferenciaService {

	/** Tipos admitidos (en mayúsculas). */
	String[] TIPOS = { "ASIENTO", "PRESTAMO", "OPERACION", "PAGO", "ENTIDAD", "COBRO", "FILA", "CARGA", "CORRIDA", "CUENTA",
			"CXP", "CXC", "EMPLEADO", "EJECUCION", "DECISION", "NOVEDAD" };

	/**
	 * Ficha de un elemento. {@code anio}/{@code mes} dan el contexto (mes de la novedad) para las cuentas (movimiento y
	 * saldo del mes) y para resolver códigos de novedad sin mes ("C-01").
	 */
	Map<String, Object> ficha(Long idEmpresa, String tipo, String valor, Long anio, Long mes) throws Throwable;
}
