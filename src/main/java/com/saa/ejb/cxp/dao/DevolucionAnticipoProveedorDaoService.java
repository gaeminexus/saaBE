package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.DevolucionAnticipoProveedor;

import jakarta.ejb.Local;

@Local
public interface DevolucionAnticipoProveedorDaoService extends EntityDao<DevolucionAnticipoProveedor> {

	/**
	 * Listado de devoluciones de un proveedor en una empresa, como proyección escalar (no la
	 * entidad completa), más reciente primero — docs/logica-negocio/cxp/API-DEVOLUCION-ANTICIPO-PROVEEDOR.md §4.2.
	 * <p>
	 * Columnas del Object[]: 0 codigo, 1 fecha, 2 valor, 3 referencia, 4 estado,
	 * 5 cuentaBancaria.banco.nombre, 6 cuentaBancaria.numeroCuenta, 7 asiento.numeroAlterno,
	 * 8 motivoAnulacion.
	 * @param idEmpresa  : Id de la empresa
	 * @param idTitular  : Id del proveedor; null = todos
	 * @return           : Filas de devoluciones, más reciente primero
	 * @throws Throwable : Excepcion
	 */
	List<Object[]> selectListadoByEmpresaTitular(Long idEmpresa, Long idTitular) throws Throwable;

	/**
	 * Detalle (por anticipo) de todas las devoluciones de un proveedor en una empresa, para
	 * agrupar en el servicio junto al listado de {@link #selectListadoByEmpresaTitular}.
	 * <p>
	 * Columnas del Object[]: 0 devolucion.codigo, 1 anticipo.id, 2 anticipo.numeroDoc, 3 valor.
	 * @param idEmpresa  : Id de la empresa
	 * @param idTitular  : Id del proveedor; null = todos
	 * @return           : Filas de detalle de todas las devoluciones del filtro
	 * @throws Throwable : Excepcion
	 */
	List<Object[]> selectDetalleByEmpresaTitular(Long idEmpresa, Long idTitular) throws Throwable;

	/**
	 * Devoluciones ACTIVAS que tienen al menos un detalle sobre el anticipo indicado — para la
	 * guarda de {@code AnticipoProveedorServiceImpl.anularAnticipo} (§4.3): un anticipo con una
	 * devolución activa no se puede anular sin anular antes la devolución.
	 * @param idAnticipo : Id del anticipo (PGS.ANTP)
	 * @return           : Devoluciones activas que lo devuelven, normalmente 0 ó 1
	 * @throws Throwable : Excepcion
	 */
	List<DevolucionAnticipoProveedor> selectActivasByAnticipo(Long idAnticipo) throws Throwable;
}
