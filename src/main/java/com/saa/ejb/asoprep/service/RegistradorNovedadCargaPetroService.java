package com.saa.ejb.asoprep.service;

import com.saa.model.crd.ParticipeXCargaArchivo;

import jakarta.ejb.Local;

/**
 * Registra novedades de {@code CargaArchivoPetroServiceImpl.aplicarPagosArchivoPetro} en su
 * PROPIA transacción — VALIDACION-TOPE-AFECTACION-MANUAL.md §15.
 *
 * <p><b>Por qué esto necesita un bean aparte:</b> {@code aplicarPagosArchivoPetro} es
 * {@code @Stateful} + {@code @TransactionAttribute(REQUIRED)} — todo el proceso corre en UNA
 * sola transacción. Si algo generado DURANTE la aplicación (una novedad bloqueante del §12/§13,
 * o un excedente del motor de pagos) hace fallar la red del §11 después, la transacción entera
 * revierte — LLEVÁNDOSE la novedad que debía guiar al operador. El defecto se detectó verificado
 * en producción: el §12 (sobrante de aportes) generaba su novedad, el proceso seguía, la red del
 * §11 no cuadraba, lanzaba, y la novedad desaparecía con el rollback — el usuario veía el error
 * sin tener dónde actuar.</p>
 *
 * <p>Un método {@code @TransactionAttribute(REQUIRES_NEW)} en el propio
 * {@code CargaArchivoPetroServiceImpl} NO sirve: es una llamada de la clase a sí misma
 * (self-invocation), y la demarcación transaccional de EJB se aplica en el proxy — un método
 * privado nunca pasa por él, y ni siquiera un método público de la interfaz de negocio lo haría
 * si se llama sin pasar por el proxy. Peor: sobre un {@code @Stateful}, forzarlo a través de
 * {@code SessionContext#getBusinessObject(...)} sería una llamada "loopback" (el bean
 * reentrando sobre sí mismo), que el contenedor EJB puede rechazar. La solución, ya usada en
 * este mismo repositorio para el mismo problema exacto (ver
 * {@code com.saa.ejb.cxp.serviceImpl.MarcadoErrorDocumentoServiceImpl}, "marcado de error en
 * transacción propia"), es un bean {@code @Stateless} DISTINTO, invocado vía {@code @EJB} — una
 * llamada a otro bean sí pasa por su propio proxy y su propia transacción.</p>
 *
 * <p><b>Idempotente a propósito:</b> si la transacción principal revierte DESPUÉS de que esta
 * escribió la novedad, y el operador vuelve a correr {@code aplicarPagosArchivoPetro}, el mismo
 * evento (misma fila PXCA, mismo tipo de novedad) se va a volver a generar. Si eso insertara una
 * SEGUNDA fila {@code NovedadParticipeCarga} en vez de actualizar la existente, el pozo de
 * {@code disponibleParaTope} (VALIDACION-TOPE-AFECTACION-MANUAL.md §10) la contaría dos veces
 * para toda combinación con {@code codigoPrestamo == null} (no tiene clave de dedup) — sería
 * reintroducir el defecto de SANCHEZ por otra puerta. Por eso: si ya existe una
 * {@code NovedadParticipeCarga} de ese {@code tipoNovedad} para esa fila PXCA, se ACTUALIZA en
 * vez de crear otra.</p>
 */
@Local
public interface RegistradorNovedadCargaPetroService {

	/**
	 * Registra (o actualiza, si ya existe una del mismo tipo para esa fila) una
	 * {@code NovedadParticipeCarga}, en una transacción NUEVA e independiente de la del
	 * llamador — sobrevive aunque la transacción del llamador revierta después.
	 *
	 * Mismos parámetros y mismo significado que el {@code registrarNovedad} original de
	 * {@code CargaArchivoPetroServiceImpl} — este método reemplaza SOLO a las llamadas hechas
	 * DURANTE {@code aplicarPagosArchivoPetro} (las de Fase 1/2, en
	 * {@code procesarArchivoPetro}, ya corren en su propia transacción de nivel superior y no
	 * tienen este problema — esas siguen usando el {@code registrarNovedad} local).
	 *
	 * @param participe      Fila PXCA a la que pertenece la novedad (identidad para idempotencia:
	 *                       su {@code codigo} + {@code tipoNovedad})
	 * @param tipoNovedad    Código del tipo de novedad (rubro {@code ASPNovedadesCargaArchivo})
	 * @param descripcion    Descripción de la novedad
	 * @param codigoProducto Código de producto relacionado (opcional)
	 * @param codigoPrestamo Código de préstamo relacionado (opcional)
	 * @param montoEsperado  Monto esperado (opcional)
	 * @param montoRecibido  Monto recibido (opcional)
	 *
	 * Sin {@code throws}, a propósito e igual que el {@code registrarNovedad} original: cualquier
	 * fallo al dejar constancia de la novedad se absorbe adentro (se loguea) y NUNCA debe abortar
	 * la aplicación de pagos que la disparó.
	 */
	void registrarEnTransaccionPropia(ParticipeXCargaArchivo participe, int tipoNovedad, String descripcion,
			Long codigoProducto, Long codigoPrestamo, Double montoEsperado, Double montoRecibido);

}
