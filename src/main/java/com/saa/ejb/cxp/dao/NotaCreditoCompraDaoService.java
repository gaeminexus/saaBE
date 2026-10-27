package com.saa.ejb.cxp.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.NotaCreditoCompra;
import jakarta.ejb.Local;
@Local
public interface NotaCreditoCompraDaoService extends EntityDao<NotaCreditoCompra> {

	/**
	 * Notas de crédito de compra activas cuyo sustento tributario SRI (NTCCCSUS) no se pudo
	 * resolver automáticamente ni se ha corregido a mano. Es la lista que hay que repasar
	 * antes de generar el ATS. Mismo criterio que {@code FacturaCompraDaoService.selectPendientesSustento}.
	 *
	 * @param idEmpresa		: Empresa a filtrar; null = todas
	 * @return				: Notas de crédito con sustento pendiente, más recientes primero
	 * @throws Throwable	: Excepcion
	 */
	List<NotaCreditoCompra> selectPendientesSustento(Long idEmpresa) throws Throwable;

	/**
	 * Nota de crédito de compra por clave de acceso del SRI y empresa (docs/logica-negocio/cxp/
	 * API-DOCUMENTOS-SEGUROS-CXP.md §5.1). Mismo criterio que
	 * {@code FacturaCompraDaoService.selectByClaveEmpresa}.
	 * @param clave      : Clave de acceso de 49 dígitos del SRI
	 * @param idEmpresa  : Id de la empresa
	 * @return           : Notas de crédito con esa clave en esa empresa (normalmente 0 o 1)
	 * @throws Throwable : Excepcion
	 */
	List<NotaCreditoCompra> selectByClaveEmpresa(String clave, Long idEmpresa) throws Throwable;

	/**
	 * Notas de crédito de compra registradas y vigentes de un proveedor, con lo aplicado y lo
	 * devuelto de cada una calculado en la base en UNA consulta (subconsultas escalares; sin N+1
	 * ni {@code in :ids}). El saldo se calcula, no se guarda:
	 * {@code saldo = total - aplicado - devuelto} (docs/logica-negocio/cxp/NC-EXCEDE-SALDO-FACTURA.md §2 y §6).
	 * <p>
	 * "Registrada y vigente": tiene asiento, {@code estado = ACTIVO} y {@code estadoEmision <> 3} (anulada).
	 * <p>
	 * Columnas del Object[]: 0 id, 1 numero, 2 fecha, 3 numDocModificado, 4 total,
	 * 5 aplicado (APLP activas con esta NC), 6 devuelto (DDPR de devoluciones ACTIVAS).
	 * Devuelve TODAS las vigentes (con y sin saldo); el filtro por saldo lo hace el servicio.
	 * @param idTitular  : Id del proveedor
	 * @param idEmpresa  : Id de la empresa
	 * @return           : Filas, la más antigua primero
	 * @throws Throwable : Excepcion
	 */
	List<Object[]> selectVigentesConAplicadoByTitularEmpresa(Long idTitular, Long idEmpresa) throws Throwable;

	/**
	 * Lo aplicado a facturas y lo devuelto por el proveedor de UNA nota de crédito, para calcular su
	 * saldo ({@code total - aplicado - devuelto}). Aplicado = suma de las APLP activas que tienen a
	 * la nota como instrumento (incluye los cruces contra otras facturas); devuelto = suma de las
	 * líneas DDPR de devoluciones ACTIVAS.
	 * @param idNotaCredito : Id de la nota de crédito de compra
	 * @return              : {aplicado, devuelto}; ambos 0.0 si no hay nada
	 * @throws Throwable    : Excepcion
	 */
	double[] selectAplicadoYDevuelto(Long idNotaCredito) throws Throwable;
}
