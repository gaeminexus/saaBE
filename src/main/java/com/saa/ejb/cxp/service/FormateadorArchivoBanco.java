package com.saa.ejb.cxp.service;

import java.util.List;

import com.saa.model.cxp.LotePago;
import com.saa.model.cxp.PagoProgramado;

/**
 * Genera el contenido del archivo de transferencias que se carga en el portal de
 * la entidad financiera.
 *
 * El formato lo define cada banco, por eso se aísla en esta interfaz: cambiar de
 * banco (o recibir por fin el formato oficial) solo obliga a escribir otra
 * implementación, sin tocar el flujo de pagos.
 *
 * PENDIENTE: el formato oficial del banco todavía no está definido. La
 * implementación actual ({@code FormateadorArchivoBancoPlanoImpl}) es
 * provisional y NO sirve para producción.
 */
public interface FormateadorArchivoBanco {

	/**
	 * Construye el contenido completo del archivo a enviar al banco.
	 * @param lote  : Lote que agrupa los pagos
	 * @param pagos : Pagos incluidos en el lote
	 * @return      : Contenido del archivo
	 * @throws Throwable : Excepcion si falta algún dato obligatorio del formato
	 */
	String generarContenido(LotePago lote, List<PagoProgramado> pagos) throws Throwable;

	/**
	 * Nombre con el que se guarda y descarga el archivo.
	 * @param lote : Lote de pagos
	 * @return     : Nombre del archivo, con extensión
	 */
	String nombreArchivo(LotePago lote);
}
