package com.saa.ejb.cxp.seguimiento;

import jakarta.persistence.EntityManager;

/**
 * ÍTEM 21 (2026-09-15, docs/logica-negocio/tsr/PLAN-SEGUIMIENTO-PAGOS.md §3.1): resuelve el
 * documento de origen de un {@code PagoProgramado} para {@code GET /pgtr/seguimiento/{idPago}}.
 * Una implementación por clave de origen (tabla §3 del contrato), elegida por
 * {@code PagoProgramadoServiceImpl} con un {@code Map<String, ResolutorOrigenPago>} en vez de un
 * {@code if}/{@code switch} encadenado — el origen N+1 que no tiene resolutor no revienta ni pasa
 * en silencio: el llamador lo trata como {@code resuelto=false}, nunca un 500.
 * <p>
 * Los resolutores de {@code crd} leen, NO escriben: consultan por {@link EntityManager#find} sin
 * importar ninguna clase de {@code com.saa.ejb.crd} (módulo de otro equipo).
 */
public interface ResolutorOrigenPago {

	/**
	 * @param idOrigen : el id del documento de origen ({@code PagoProgramado.idOrigen}, o el id
	 *                   de la entidad enlazada por FK para los cuatro orígenes propios de CXP)
	 * @param em       : para consultar la entidad de origen; sólo lectura
	 * @return         : el documento resuelto, o {@code null} si el id no existe (dato borrado,
	 *                   id inconsistente) — el llamador lo trata igual que "sin resolutor"
	 * @throws Throwable : cualquier error de la consulta -- el llamador lo atrapa y también lo
	 *                     trata como {@code resuelto=false}, nunca un 500
	 */
	DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable;
}
