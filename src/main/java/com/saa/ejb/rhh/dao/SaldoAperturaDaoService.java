package com.saa.ejb.rhh.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.SaldoApertura;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService SaldoApertura.
 */
@Local
public interface SaldoAperturaDaoService extends EntityDao<SaldoApertura> {

	/**
	 * Recupera todos los saldos de apertura de una empresa y una fecha de corte,
	 * ordenados por identificacion y tipo de saldo.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @return				: Listado de saldos; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<SaldoApertura> selectByEmpresaYCorte(Long idEmpresa, LocalDate fechaCorte) throws Throwable;

	/**
	 * Recupera los saldos de un corte que todavia no se han materializado
	 * (SLAPAPLC distinto de 'S'), ordenados por tipo de saldo para que la
	 * antiguedad se aplique antes que los saldos que dependen de ella.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @return				: Listado de saldos pendientes; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<SaldoApertura> selectPendientesPorAplicar(Long idEmpresa, LocalDate fechaCorte) throws Throwable;

	/**
	 * Recupera los saldos de un corte que ya fueron materializados, en orden
	 * inverso al de aplicacion, para poder revertirlos.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @return				: Listado de saldos aplicados; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<SaldoApertura> selectAplicados(Long idEmpresa, LocalDate fechaCorte) throws Throwable;

	/**
	 * Cuenta cuantas veces aparece la misma combinacion de identificacion, tipo de
	 * saldo y anio dentro de un corte. Sirve para detectar duplicados en el archivo.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @return				: Listado de arreglos [identificacion, tipoSaldo, anio, cantidad] con cantidad mayor a uno
	 * @throws Throwable	: Excepcion
	 */
	List<Object[]> selectDuplicados(Long idEmpresa, LocalDate fechaCorte) throws Throwable;

}
