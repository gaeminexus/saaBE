package com.saa.ejb.rhh.service;

import com.saa.model.rhh.ArchivoBatchIess;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Genera el archivo de carga batch de novedades del IESS.</p>
 *
 * <p><b>Un archivo por tipo y un envio por mes</b>, que es como lo pide el portal. El
 * formato es el del anexo oficial: ASCII, separador <code>;</code>, fechas
 * <code>YYYYMMDD</code>, importes con dos decimales y punto, un registro por afiliado.</p>
 *
 * <p><b>Se niega a generar antes que mandar basura.</b> Si a alguna novedad le falta un
 * dato obligatorio del registro, o si algun codigo del catalogo sigue en <code>'?'</code>
 * --los anexos del portal exigen login y hay dos sin leer--, el archivo no se produce y
 * el error dice exactamente que falta y de quien. Un archivo con un <code>'?'</code>
 * dentro lo rechaza el IESS entero, y el rechazo llega dias despues.</p>
 */
@Local
public interface ExportacionNovedadesIessService {

	/**
	 * Genera el contenido del archivo batch de un tipo de novedad para el periodo.
	 *
	 * <p><b>Recibe el periodo, no sus fechas</b>, y es deliberado: la ventana de novedades
	 * se calcula en un solo sitio. Si el cliente mandara <code>desde</code> y
	 * <code>hasta</code>, cliente y servidor podrian estar mirando conjuntos distintos sin
	 * que nada lo delatara --exactamente el riesgo que la regla de cierre existe para
	 * evitar--.</p>
	 *
	 * <p><b>Aqui se sella el codigo IESS de cada novedad</b>, con el mismo valor que se
	 * escribe en el archivo. Es el instante correcto: el codigo que viaja al IESS por esta
	 * via es el que queda en el <code>.txt</code>, asi que sellarlo despues --al marcar
	 * enviada-- podria grabar un valor distinto del que se mando si entre medias alguien
	 * completo el catalogo.</p>
	 *
	 * @param idPeriodo		: Id del periodo de nomina, del que salen empresa y ventana
	 * @param tipoNovedad	: Codigo alterno del detalle del rubro 204
	 * @param usuario		: Usuario que genera
	 * @return				: El archivo con su nombre y, si lo hay, el aviso que debe acompanarlo
	 * @throws Throwable	: IncomeException si no hay novedades o si falta algun dato
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	ArchivoBatchIess generarArchivo(Long idPeriodo, Long tipoNovedad, String usuario) throws Throwable;

}
