/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.service.PeriodoNominaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.rubros.RhhTipoPeriodoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PeriodoNominaService.
 *  Contiene los servicios relacionados con la entidad PeriodoNomina</p>
 */
@Stateless
public class PeriodoNominaServiceImpl implements PeriodoNominaService {

	@EJB
	private PeriodoNominaDaoService periodoNominaDaoService;

	@PersistenceContext
	private EntityManager em;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<PeriodoNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de periodoNomina service");
		for (PeriodoNomina registro:lista) {			
			periodoNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de periodoNomina service");
		//INSTANCIA UNA ENTIDAD
		PeriodoNomina periodoNomina = new PeriodoNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			
				periodoNominaDaoService.remove(periodoNomina, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<PeriodoNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) PeriodoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PeriodoNomina> result = periodoNominaDaoService.selectAll(NombreEntidadesRhh.PERIODO_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de periodoNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PeriodoNominaService#selectById(java.lang.Long)
	 */
	public PeriodoNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return periodoNominaDaoService.selectById(id, NombreEntidadesRhh.PERIODO_NOMINA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.PeriodoNominaService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<PeriodoNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) PeriodoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PeriodoNomina> result = periodoNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.PERIODO_NOMINA); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de periodoNomina no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public PeriodoNomina saveSingle(PeriodoNomina periodoNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) PeriodoNomina");
		validaPeriodo(periodoNomina);
		periodoNomina = periodoNominaDaoService.save(periodoNomina, periodoNomina.getCodigo());
		return periodoNomina;
	}

	/**
	 * Valida un periodo antes de guardarlo. No calcula ni toca ningun otro dato: solo
	 * comprueba que lo declarado sea coherente consigo mismo y que no choque con un
	 * periodo existente. Es la correccion 16 de {@code PLAN-CORRECCIONES-MOTOR.md}.
	 *
	 * <p>Antes de esta validacion, {@code saveSingle} era un paso directo al DAO: un
	 * periodo del 1 de enero al 21 de agosto se guardaba sin protestar y calculaba con
	 * 21 dias para todo el mundo. Con el cliente creando periodos desde pantalla, ese
	 * silencio deja de ser tolerable.</p>
	 *
	 * @param periodo		: Periodo a guardar, tal como llega de saveSingle
	 * @throws Throwable	: IncomeException con el detalle de que esta mal y como corregirlo
	 */
	private void validaPeriodo(PeriodoNomina periodo) throws Throwable {
		if (periodo.getAnio() == null || periodo.getMes() == null) {
			throw new IncomeException("El periodo debe declarar año (PRDNANOO) y mes (PRDNMSEE).");
		}
		if (periodo.getMes().intValue() < 1 || periodo.getMes().intValue() > 12) {
			throw new IncomeException("El mes declarado (" + periodo.getMes()
					+ ") no es valido: debe estar entre 1 y 12.");
		}
		if (periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
			throw new IncomeException("El periodo debe tener fecha de inicio (PRDNFCHI) y fecha de fin"
					+ " (PRDNFCHF).");
		}
		String periodoDeclarado = periodo.getAnio() + "-" + String.format("%02d", periodo.getMes());
		if (periodo.getFechaInicio().getYear() != periodo.getAnio().intValue()
				|| periodo.getFechaInicio().getMonthValue() != periodo.getMes().intValue()) {
			throw new IncomeException("La fecha de inicio " + periodo.getFechaInicio()
					+ " no corresponde al periodo declarado " + periodoDeclarado
					+ ". Corrija la fecha de inicio o el año/mes del periodo.");
		}
		if (periodo.getFechaFin().getYear() != periodo.getAnio().intValue()
				|| periodo.getFechaFin().getMonthValue() != periodo.getMes().intValue()) {
			throw new IncomeException("La fecha de fin " + periodo.getFechaFin()
					+ " no corresponde al periodo declarado " + periodoDeclarado
					+ ". Corrija la fecha de fin o el año/mes del periodo.");
		}
		if (periodo.getFechaInicio().isAfter(periodo.getFechaFin())) {
			throw new IncomeException("La fecha de inicio " + periodo.getFechaInicio()
					+ " es posterior a la fecha de fin " + periodo.getFechaFin() + ".");
		}
		if (periodo.getModo() == null) {
			throw new IncomeException("El periodo debe declarar el modo (PRDNMODO): 1 para historico sin"
					+ " contabilizar, 2 para productivo que contabiliza. Sin modo, el motor lo trata como"
					+ " historico en todas partes, sin avisar.");
		}
		if (periodo.getTipoPeriodo() == null) {
			throw new IncomeException("El periodo debe declarar el tipo (PRDNTPNM): mensual, quincenal,"
					+ " decimo tercero, decimo cuarto, utilidades, liquidacion o bono extraordinario.");
		}
		// La unicidad es prerequisito de este chequeo, no de los ocho pedidos: sin
		// empresa no hay contra que contrastar el (empresa, anio, mes, tipo).
		if (periodo.getEmpresa() == null || periodo.getEmpresa().getCodigo() == null) {
			throw new IncomeException("El periodo debe declarar la empresa (PJRQCDGO).");
		}

		// La unicidad es por (empresa, anio, mes, tipo de periodo), no por (empresa,
		// anio, mes): un MENSUAL y un DECIMO_TERCERO del mismo mes coexisten legitimos.
		StringBuilder jpql = new StringBuilder("select count(p) from PeriodoNomina p")
				.append(" where p.empresa.codigo = :idEmpresa and p.anio = :anio and p.mes = :mes")
				.append(" and p.tipoPeriodo = :tipoPeriodo");
		if (periodo.getCodigo() != null) {
			jpql.append(" and p.codigo <> :idPeriodo");
		}
		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", periodo.getEmpresa().getCodigo());
		query.setParameter("anio", periodo.getAnio());
		query.setParameter("mes", periodo.getMes());
		query.setParameter("tipoPeriodo", periodo.getTipoPeriodo());
		if (periodo.getCodigo() != null) {
			query.setParameter("idPeriodo", periodo.getCodigo());
		}
		Long existentes = (Long) query.getSingleResult();
		if (existentes != null && existentes.longValue() > 0) {
			throw new IncomeException("Ya existe un periodo " + periodoDeclarado + " de tipo "
					+ periodo.getTipoPeriodo() + " para esta empresa. Si necesita corregirlo, actualice"
					+ " ese registro en vez de crear uno nuevo.");
		}

		// AVISO, no bloqueo: solo para MENSUAL. Las reglas de arriba ya impiden el caso
		// peligroso (fechas fuera del mes); que un mensual arranque a mitad de mes puede
		// ser legitimo -una empresa que recien contrata-.
		if (Long.valueOf(RhhTipoPeriodoNomina.MENSUAL).equals(periodo.getTipoPeriodo())) {
			int ultimoDiaMes = periodo.getFechaInicio().lengthOfMonth();
			if (periodo.getFechaInicio().getDayOfMonth() != 1
					|| periodo.getFechaFin().getDayOfMonth() != ultimoDiaMes) {
				System.out.println("Aviso: el periodo mensual " + periodoDeclarado + " no cubre el mes"
						+ " completo (del " + periodo.getFechaInicio() + " al " + periodo.getFechaFin()
						+ "). Puede ser legitimo -una empresa que arranca a mitad de mes- pero conviene"
						+ " confirmarlo.");
			}
		}
	}
}