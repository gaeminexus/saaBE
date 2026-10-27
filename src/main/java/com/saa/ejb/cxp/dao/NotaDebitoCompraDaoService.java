package com.saa.ejb.cxp.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.NotaDebitoCompra;
import jakarta.ejb.Local;
@Local
public interface NotaDebitoCompraDaoService extends EntityDao<NotaDebitoCompra> {

	/**
	 * Notas de débito de compra activas cuyo sustento tributario SRI (NTDCCSUS) no se pudo
	 * resolver automáticamente ni se ha corregido a mano. Es la lista que hay que repasar
	 * antes de generar el ATS. Mismo criterio que {@code FacturaCompraDaoService.selectPendientesSustento}.
	 *
	 * @param idEmpresa		: Empresa a filtrar; null = todas
	 * @return				: Notas de débito con sustento pendiente, más recientes primero
	 * @throws Throwable	: Excepcion
	 */
	List<NotaDebitoCompra> selectPendientesSustento(Long idEmpresa) throws Throwable;

	/**
	 * Nota de débito de compra por clave de acceso del SRI y empresa (docs/logica-negocio/cxp/
	 * API-DOCUMENTOS-SEGUROS-CXP.md §5.1). Mismo criterio que
	 * {@code FacturaCompraDaoService.selectByClaveEmpresa}.
	 * @param clave      : Clave de acceso de 49 dígitos del SRI
	 * @param idEmpresa  : Id de la empresa
	 * @return           : Notas de débito con esa clave en esa empresa (normalmente 0 o 1)
	 * @throws Throwable : Excepcion
	 */
	List<NotaDebitoCompra> selectByClaveEmpresa(String clave, Long idEmpresa) throws Throwable;

	/**
	 * Notas de débito de compra registradas y vigentes (con asiento, ACTIVAS, no anuladas) y no
	 * bloqueadas por seguros de un proveedor, junto con la factura que modifican y lo aplicado a esa
	 * factura, en UNA consulta (subconsulta escalar; sin N+1) — para cruzar el saldo de una nota de
	 * crédito contra la nota de débito (docs/logica-negocio/cxp/NC-EXCEDE-SALDO-FACTURA.md §4.1).
	 * La factura se empareja por número sin guiones, titular y empresa, igual que
	 * {@code AplicacionPagoCxpDaoService.selectFacturaByNumero}. El filtro por saldo lo hace el servicio.
	 * <p>
	 * Columnas del Object[]: 0 nd.id, 1 nd.numero, 2 nd.fecha, 3 nd.total, 4 factura.id,
	 * 5 factura.numero, 6 factura.total, 7 aplicado a la factura (APLP activas).
	 * @param idTitular  : Id del proveedor
	 * @param idEmpresa  : Id de la empresa
	 * @return           : Filas, la más antigua primero
	 * @throws Throwable : Excepcion
	 */
	List<Object[]> selectVigentesConFacturaByTitularEmpresa(Long idTitular, Long idEmpresa) throws Throwable;
}
