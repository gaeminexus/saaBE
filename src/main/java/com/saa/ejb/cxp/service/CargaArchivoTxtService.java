package com.saa.ejb.cxp.service;
import java.util.List;
import java.util.Map;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.CargaArchivoTxt;
import jakarta.ejb.Local;
@Local
public interface CargaArchivoTxtService extends EntityService<CargaArchivoTxt> {
	List<CargaArchivoTxt> selectByEmpresa(Long idEmpresa) throws Throwable;

	/**
	 * ÍTEM 25 (2026-09-16, docs/logica-negocio/cxp/API-CARGAS-TXT-BANDEJA-ELECTRONICA.md §2):
	 * busca cargas de TXT por empresa, con filtros opcionales. Más recientes primero
	 * (fechaCarga desc, id desc). Sin resultados devuelve lista vacía, nunca lanza por eso.
	 * @param idEmpresa      : obligatorio
	 * @param desde          : yyyy-MM-dd, sobre fechaCarga, inclusive; null = sin límite inferior
	 * @param hasta          : yyyy-MM-dd, sobre fechaCarga, inclusive TODO el día; null = sin
	 *                         límite superior
	 * @param idPeriodo      : exacto sobre periodoContable; null = sin filtro
	 * @param nombreArchivo  : contiene, sin distinguir mayúsculas; null/blank = sin filtro
	 * @param estado         : exacto; null = sin filtro
	 * @param limite         : máximo de filas; null o &lt;=0 usa 100; se recorta a 500
	 * @return : cada fila con idCarga, fechaCarga ("yyyy-MM-dd"), nombreArchivo, totalLeidos,
	 *           nuevos, duplicados, novedades, estado, estadoTexto, usuario, periodo
	 *           ({idPeriodo, nombre} o null), registrados y pendientes (documentos de la carga
	 *           en estadoDocumento=3 y el resto)
	 * @throws Throwable : IncomeException si falta idEmpresa o alguna fecha no se puede leer
	 */
	List<Map<String, Object>> buscar(Long idEmpresa, String desde, String hasta, Long idPeriodo,
			String nombreArchivo, Long estado, Integer limite) throws Throwable;
}