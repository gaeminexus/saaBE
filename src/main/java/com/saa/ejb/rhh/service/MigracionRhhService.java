package com.saa.ejb.rhh.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Migracion de apertura del modulo de RRHH.</p>
 *
 * <p>El proceso tiene cuatro pasos deliberadamente separados, para que nada se
 * materialice sin haber sido revisado antes:</p>
 *
 * <ol>
 *   <li><b>Cargar</b> — lee el archivo y solo inserta filas en <code>RHH.SLAP</code>.
 *       No toca ninguna tabla operativa.</li>
 *   <li><b>Validar</b> — contrasta los SLAP contra el maestro y devuelve la lista de
 *       inconsistencias. No modifica nada.</li>
 *   <li><b>Aplicar</b> — materializa los SLAP en las tablas operativas. Es idempotente
 *       gracias a <code>SLAPAPLC</code>: un saldo ya aplicado se salta.</li>
 *   <li><b>Revertir</b> — deshace la aplicacion usando <code>SLAPRFTB</code> y
 *       <code>SLAPRFID</code>, que guardan exactamente que registro creo cada saldo.</li>
 * </ol>
 *
 * <p>Con 18 a 25 empleados el resultado se revisa a mano comodamente, que es
 * justamente el criterio de aceptacion de esta fase.</p>
 */
@Local
public interface MigracionRhhService {

	/**
	 * Carga masiva de saldos desde un archivo CSV. Solo inserta en RHH.SLAP: no
	 * materializa nada todavia. El formato del archivo esta documentado en
	 * <code>docs/logica-negocio/rhh/REGLAS-MIGRACION-APERTURA.md</code>.
	 *
	 * @param archivo		: Contenido del archivo
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @param usuario		: Usuario que registra
	 * @return				: Numero de saldos cargados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int cargarSaldosApertura(InputStream archivo, Long idEmpresa, LocalDate fechaCorte,
			String usuario) throws Throwable;

	/**
	 * Valida los SLAP de un corte contra el maestro: identificaciones inexistentes o
	 * ambiguas, tipos incompatibles con el contrato, montos negativos, campos
	 * obligatorios vacios segun el tipo de saldo, y duplicados.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @return				: Listado de inconsistencias; vacio significa que se puede aplicar
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	List<String> validarSaldosApertura(Long idEmpresa, LocalDate fechaCorte) throws Throwable;

	/**
	 * Materializa los SLAP en las tablas operativas. Es idempotente por SLAPAPLC:
	 * volver a ejecutarlo no duplica nada. Rechaza la ejecucion si la validacion
	 * devuelve inconsistencias.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @param usuario		: Usuario que registra
	 * @return				: Numero de saldos materializados en esta ejecucion
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int aplicarSaldosApertura(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable;

	/**
	 * Deshace la aplicacion usando SLAPRFTB y SLAPRFID, y deja los saldos listos para
	 * volver a aplicarse.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha de corte de la migracion
	 * @param usuario		: Usuario que registra
	 * @return				: Numero de saldos revertidos
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int revertirSaldosApertura(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable;

}
