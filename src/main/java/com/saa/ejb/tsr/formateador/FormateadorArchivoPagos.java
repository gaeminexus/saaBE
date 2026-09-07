package com.saa.ejb.tsr.formateador;

import java.util.List;

import com.saa.model.cxp.LotePago;
import com.saa.model.cxp.PagoProgramado;

/**
 * Genera el archivo de pagos en el formato oficial de un banco concreto.
 * Ver docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md.
 */
public interface FormateadorArchivoPagos {

	/**
	 * Construye el archivo completo a enviar (o a pegar en la macro) del banco.
	 * @param lote  : Lote que agrupa los pagos
	 * @param pagos : Pagos incluidos en el lote
	 * @return      : Archivo generado, listo para guardar y/o descargar
	 * @throws Throwable : Excepcion si falta algún dato obligatorio del formato
	 */
	ArchivoPagosGenerado generar(LotePago lote, List<PagoProgramado> pagos) throws Throwable;
}
