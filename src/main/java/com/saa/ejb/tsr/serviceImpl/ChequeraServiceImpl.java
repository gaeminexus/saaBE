package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.ChequeDaoService;
import com.saa.ejb.tsr.dao.ChequeraDaoService;
import com.saa.ejb.tsr.service.ChequeService;
import com.saa.ejb.tsr.service.ChequeraService;
import com.saa.model.tsr.Cheque;
import com.saa.model.tsr.Chequera;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.EstadoCheque;
import com.saa.rubros.EstadoChequera;
import com.saa.rubros.MotivoAnulacionCheque;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ChequeraService.
 *  Contiene los servicios relacionados con la entidad Chequera.</p>
 */
@Stateless
public class ChequeraServiceImpl implements ChequeraService {

	@EJB
	private ChequeraDaoService chequeraDaoService;

	@EJB
	private ChequeDaoService chequeDaoService;

	@EJB
	private ChequeService chequeService;

	@PersistenceContext
	private EntityManager em;

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de chequera service");
		Chequera chequera = new Chequera();
		for (Long registro : id) {
				chequeraDaoService.remove(chequera, registro);	
		}		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.List<Chequera>)
	 */
	public void save(List<Chequera> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de chequera service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Chequera registro : lista) {			
			chequeraDaoService.save(registro, registro.getCodigo()); 
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	public List<Chequera> selectAll() throws Throwable{
		System.out.println("Ingresa al metodo (selectAll) chequera Service");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Chequera> result = chequeraDaoService.selectAll(NombreEntidadesTesoreria.CHEQUERA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaLogicaPorCajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Chequera> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Chequera");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Chequera> result = chequeraDaoService.selectByCriteria
		(datos, NombreEntidadesTesoreria.CHEQUERA); 
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total CajaLogicaPorCajaFisica no devolvio ningun registro");
		}	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.ChequeraService#selectById(java.lang.Long)
	 */
	public Chequera selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return chequeraDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUERA);
	}

	@Override
	public Chequera saveSingle(Chequera  chequera) throws Throwable {
		System.out.println("saveSingle - Chequera");
		chequera = chequeraDaoService.save(chequera, chequera.getCodigo());
		return chequera;
	}

	// =====================================================================
	// Recepción de chequeras y ciclo de vida
	// =====================================================================

	@Override
	public Long sugerirNumeroInicial(Long idCuentaBancaria) throws Throwable {
		System.out.println("=== sugerirNumeroInicial | cuenta=" + idCuentaBancaria + " ===");
		Long max = chequeraDaoService.selectMaxFinalizaByCuenta(idCuentaBancaria);
		return (max != null) ? Long.valueOf(max.longValue() + 1) : Long.valueOf(1L);
	}

	@Override
	public Chequera registrarRecepcion(Long idCuentaBancaria, Long comienza, Long finaliza,
			LocalDateTime fechaEntrega, Long idUsuario) throws Throwable {
		// idUsuario se recibe por simetría con el resto de los registrar*/procesar*
		// del módulo (y por si mañana Chequera agrega auditoría de quién la
		// recibió), pero hoy no se usa: la entidad Chequera no tiene columna de
		// usuario.

		System.out.println("=== registrarRecepcion | cuenta=" + idCuentaBancaria
				+ " | rango=" + comienza + "-" + finaliza + " ===");

		if (idCuentaBancaria == null) {
			throw new IncomeException("Debe indicar la cuenta bancaria.");
		}
		CuentaBancaria cuenta = em.find(CuentaBancaria.class, idCuentaBancaria);
		if (cuenta == null) {
			throw new IncomeException("No se encontró la cuenta bancaria con ID: " + idCuentaBancaria);
		}
		if (cuenta.getManejaChequera() == null || cuenta.getManejaChequera().intValue() != 1) {
			throw new IncomeException("La cuenta bancaria '" + cuenta.getNumeroCuenta()
					+ "' no maneja chequeras. Actívela en Tesorería → Cuentas bancarias.");
		}
		if (comienza == null || comienza.longValue() < 1) {
			throw new IncomeException("El número inicial de la chequera debe ser mayor o igual a 1.");
		}
		if (finaliza == null || finaliza.longValue() < comienza.longValue()) {
			throw new IncomeException("El número final debe ser mayor o igual al número inicial.");
		}
		if (finaliza.longValue() - comienza.longValue() + 1 > 500) {
			throw new IncomeException("El rango " + comienza + "-" + finaliza + " tiene "
					+ (finaliza.longValue() - comienza.longValue() + 1) + " cheques: una chequera no "
					+ "puede tener más de 500 (probablemente sea un error de tipeo en el rango).");
		}
		if (chequeraDaoService.existeSolape(idCuentaBancaria, comienza, finaliza)) {
			throw new IncomeException("El rango " + comienza + "-" + finaliza
					+ " se solapa con una chequera existente de la cuenta.");
		}

		Chequera chequera = new Chequera();
		chequera.setCuentaBancaria(cuenta);
		chequera.setComienza(comienza);
		chequera.setFinaliza(finaliza);
		chequera.setNumeroCheques(Long.valueOf(finaliza.longValue() - comienza.longValue() + 1));
		chequera.setFechaSolicitud(LocalDateTime.now());
		chequera.setFechaEntrega(fechaEntrega != null ? fechaEntrega : LocalDateTime.now());
		chequera.setRubroEstadoChequeraP(Long.valueOf(Rubros.ESTADO_CHEQUERA));
		chequera.setRubroEstadoChequeraH(Long.valueOf(EstadoChequera.ACTIVA));
		chequera = chequeraDaoService.save(chequera, chequera.getCodigo());
		em.flush();

		chequeService.crearChequesDeChequera(chequera.getCodigo(), chequera.getNumeroCheques(), comienza);

		System.out.println("✓ Chequera registrada: id=" + chequera.getCodigo()
				+ " | rango=" + comienza + "-" + finaliza);
		return chequera;
	}

	@Override
	public Map<String, Object> resumen(Long idChequera) throws Throwable {
		System.out.println("=== resumen chequera | id=" + idChequera + " ===");

		Chequera chequera = selectChequeraOrThrow(idChequera);
		List<Cheque> cheques = chequeDaoService.selectByChequera(idChequera);

		long disponibles = 0, generados = 0, impresos = 0, entregados = 0, anulados = 0;
		Long siguiente = null;
		for (Cheque cheque : cheques) {
			int estado = (cheque.getRubroEstadoChequeH() != null)
					? cheque.getRubroEstadoChequeH().intValue() : 0;
			switch (estado) {
				case EstadoCheque.ACTIVO:
					disponibles++;
					if (siguiente == null || cheque.getNumero().longValue() < siguiente.longValue()) {
						siguiente = cheque.getNumero();
					}
					break;
				case EstadoCheque.GENERADO:  generados++;  break;
				case EstadoCheque.IMPRESO:   impresos++;   break;
				case EstadoCheque.ENTREGADO: entregados++; break;
				case EstadoCheque.ANULADO:   anulados++;   break;
				default: break;
			}
		}

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("comienza", chequera.getComienza());
		resultado.put("finaliza", chequera.getFinaliza());
		resultado.put("total", Long.valueOf(cheques.size()));
		resultado.put("disponibles", disponibles);
		resultado.put("generados", generados);
		resultado.put("impresos", impresos);
		resultado.put("entregados", entregados);
		resultado.put("anulados", anulados);
		resultado.put("siguiente", siguiente);
		return resultado;
	}

	@Override
	public List<Chequera> selectByCuentaBancaria(Long idCuentaBancaria) throws Throwable {
		System.out.println("=== selectByCuentaBancaria | cuenta=" + idCuentaBancaria + " ===");
		return chequeraDaoService.selectByCuentaBancaria(idCuentaBancaria);
	}

	@Override
	public void anularChequera(Long idChequera, String motivo, Long idUsuario) throws Throwable {
		System.out.println("=== anularChequera | id=" + idChequera + " ===");

		if (motivo == null || motivo.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el motivo de la anulación.");
		}
		Chequera chequera = selectChequeraOrThrow(idChequera);

		List<Cheque> cheques = chequeDaoService.selectByChequera(idChequera);
		for (Cheque cheque : cheques) {
			int estado = (cheque.getRubroEstadoChequeH() != null)
					? cheque.getRubroEstadoChequeH().intValue() : 0;
			if (estado == EstadoCheque.GENERADO || estado == EstadoCheque.IMPRESO
					|| estado == EstadoCheque.ENTREGADO) {
				throw new IncomeException("La chequera tiene el cheque N° " + cheque.getNumero()
						+ " en uso. No se puede anular mientras tenga cheques generados, "
						+ "impresos o entregados.");
			}
		}
		for (Cheque cheque : cheques) {
			if (cheque.getRubroEstadoChequeH() != null
					&& cheque.getRubroEstadoChequeH().intValue() == EstadoCheque.ACTIVO) {
				cheque.setRubroEstadoChequeH(Long.valueOf(EstadoCheque.ANULADO));
				cheque.setRubroMotivoAnulacionH(Long.valueOf(MotivoAnulacionCheque.CHEQUERA_ANULADA));
				cheque.setFechaAnulacion(LocalDateTime.now());
				chequeDaoService.save(cheque, cheque.getCodigo());
			}
		}

		chequera.setRubroEstadoChequeraH(Long.valueOf(EstadoChequera.ANULADA));
		chequeraDaoService.save(chequera, chequera.getCodigo());

		System.out.println("✓ Chequera " + idChequera + " anulada. Motivo: " + motivo);
	}

	@Override
	public void cerrarSiTerminada(Long idChequera) throws Throwable {
		Chequera chequera;
		try {
			chequera = chequeraDaoService.selectById(idChequera, NombreEntidadesTesoreria.CHEQUERA);
		} catch (jakarta.persistence.NoResultException e) {
			// Silencioso a propósito: este método es un efecto colateral de
			// asignar/anular un cheque, no un endpoint; si la chequera ya no
			// existe no hay nada que cerrar.
			return;
		}
		int estado = (chequera.getRubroEstadoChequeraH() != null)
				? chequera.getRubroEstadoChequeraH().intValue() : 0;
		if (estado == EstadoChequera.TERMINADA || estado == EstadoChequera.ANULADA) {
			return;
		}
		for (Cheque cheque : chequeDaoService.selectByChequera(idChequera)) {
			if (cheque.getRubroEstadoChequeH() != null
					&& cheque.getRubroEstadoChequeH().intValue() == EstadoCheque.ACTIVO) {
				return; // aún quedan cheques disponibles
			}
		}
		chequera.setRubroEstadoChequeraH(Long.valueOf(EstadoChequera.TERMINADA));
		chequeraDaoService.save(chequera, chequera.getCodigo());
		System.out.println("✓ Chequera " + idChequera + " pasó a TERMINADA (sin cheques disponibles).");
	}

	/**
	 * Busca la chequera por id traduciendo la excepción a un mensaje accionable.
	 * {@code EntityDaoImpl.selectById} usa {@code getSingleResult()}: nunca
	 * devuelve null, lanza {@code NoResultException} cuando no encuentra nada.
	 * @param idChequera : Id de la chequera
	 * @return           : Chequera encontrada
	 * @throws Throwable : IncomeException si no existe
	 */
	private Chequera selectChequeraOrThrow(Long idChequera) throws Throwable {
		try {
			return chequeraDaoService.selectById(idChequera, NombreEntidadesTesoreria.CHEQUERA);
		} catch (jakarta.persistence.NoResultException e) {
			throw new IncomeException("No se encontró la chequera con ID: " + idChequera);
		}
	}

}
