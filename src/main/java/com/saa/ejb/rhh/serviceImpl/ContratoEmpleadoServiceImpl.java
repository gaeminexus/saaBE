/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial").
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.service.ContratoEmpleadoService;
import com.saa.ejb.rhh.service.NovedadIessService;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ContratoService.
 *  Contiene los servicios relacionados con la entidad Contrato</p>
 */
@Stateless
public class ContratoEmpleadoServiceImpl implements ContratoEmpleadoService {

	@EJB
	private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

	@EJB
	private NovedadIessService novedadIessService;

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ContratoEmpleado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de contrato service");
		for (ContratoEmpleado registro:lista) {
			contratoEmpleadoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de contrato service");
		//INSTANCIA UNA ENTIDAD
		ContratoEmpleado contratoEmpleado = new ContratoEmpleado();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {

				contratoEmpleadoDaoService.remove(contratoEmpleado, registro);
			}

	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ContratoEmpleado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) Contrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ContratoEmpleado> result = contratoEmpleadoDaoService.selectAll(NombreEntidadesRhh.CONTRATO_EMPLEADO);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de contrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ContratoService#selectById(java.lang.Long)
	 */
	public ContratoEmpleado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);
		return contratoEmpleadoDaoService.selectById(id, NombreEntidadesRhh.CONTRATO_EMPLEADO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ContratoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ContratoEmpleado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Contrato");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ContratoEmpleado> result = contratoEmpleadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CONTRATO_EMPLEADO);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de contrato no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ContratoEmpleado saveSingle(ContratoEmpleado contratoEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Contrato Empleado");

		// FOTO DEL ESTADO ANTERIOR, ANTES DE GUARDAR. Se copian valores sueltos y no la
		// entidad: al hacer merge, Hibernate escribe sobre la instancia gestionada, asi
		// que quedarse con la referencia daria los valores NUEVOS y la comparacion
		// siempre diria "no cambio nada".
		Long anteriorRelacion = null;
		String anteriorSectorial = null;
		Long anteriorJornada = null;
		Long anteriorDias = null;
		Double anteriorSalario = null;
		// La vigencia tambien se captura ANTES, y por una razon distinta: es @Transient, y
		// el merge no copia los transitorios. Leerla del objeto que devuelve save() daria
		// siempre null y todas las novedades saldrian con fecha estimada.
		LocalDate vigenciaCambio = contratoEmpleado.getFechaVigenciaCambio();
		boolean esActualizacion = contratoEmpleado.getCodigo() != null;
		if (esActualizacion) {
			ContratoEmpleado previo = contratoEmpleadoDaoService.selectById(
					contratoEmpleado.getCodigo(), NombreEntidadesRhh.CONTRATO_EMPLEADO);
			if (previo != null) {
				anteriorRelacion = previo.getTipoRelacionLaboral();
				anteriorSectorial = previo.getCodigoSectorialIess();
				anteriorJornada = previo.getJornada();
				anteriorDias = previo.getDiasDeclaradosIess();
				anteriorSalario = previo.getSalarioBase();
			}
		}

		contratoEmpleado = contratoEmpleadoDaoService.save(contratoEmpleado, contratoEmpleado.getCodigo());

		if (esActualizacion) {
			generaNovedadesDelCambio(contratoEmpleado, vigenciaCambio, anteriorRelacion, anteriorSectorial,
					anteriorJornada, anteriorDias, anteriorSalario);
		}
		return contratoEmpleado;
	}

	/**
	 * Crea las novedades del IESS que corresponden a lo que cambio en el contrato.
	 *
	 * <p>Dos hechos distintos, dos novedades distintas:</p>
	 * <ul>
	 *   <li><b>Relacion de trabajo o codigo sectorial</b> cambian la forma en que el IESS
	 *       clasifica la relacion, no lo que se paga.</li>
	 *   <li><b>Jornada o dias declarados</b> cambian los dias de la planilla y con ellos
	 *       el seguro de salud de tiempo parcial. Por eso no se reportan como un sueldo
	 *       nuevo: la modificacion de sueldo no lleva dias, y la planilla del mes
	 *       siguiente saldria con los de la jornada anterior.</li>
	 * </ul>
	 *
	 * <p><b>Falla sin ruido a proposito.</b> Guardar un contrato no puede romperse porque
	 * el catalogo de plazos este incompleto; la novedad que no se creo la reclama despues
	 * <code>cerrarPeriodo</code>. Queda traza en el log.</p>
	 *
	 * <p><b>De donde sale la fecha del hecho.</b> Del campo <code>fechaVigenciaCambio</code>
	 * que manda la pantalla, para que el plazo legal se cuente desde el dia en que el
	 * cambio empezo a regir y no desde el dia en que alguien lo tecleo. Si no viene se usa
	 * la de hoy, <b>y la novedad lo dice en su observacion</b>: una fecha estimada que se
	 * anuncia se puede corregir; una que se hace pasar por real, no.</p>
	 *
	 * @param contrato			: Contrato ya guardado
	 * @param vigenciaCambio	: Fecha de vigencia informada por la pantalla, o null
	 * @param anteriorRelacion	: Relacion laboral antes del cambio
	 * @param anteriorSectorial	: Codigo sectorial antes del cambio
	 * @param anteriorJornada	: Jornada antes del cambio
	 * @param anteriorDias		: Dias declarados antes del cambio
	 * @param anteriorSalario	: Salario base antes del cambio
	 */
	private void generaNovedadesDelCambio(ContratoEmpleado contrato, LocalDate vigenciaCambio, Long anteriorRelacion,
			String anteriorSectorial, Long anteriorJornada, Long anteriorDias, Double anteriorSalario) {
		boolean vigenciaEstimada = vigenciaCambio == null;
		LocalDate vigencia = vigenciaEstimada ? LocalDate.now() : vigenciaCambio;
		String avisoFecha = vigenciaEstimada
				? " (fecha del hecho estimada: no se informo la vigencia del cambio, se uso la de registro)"
				: "";
		try {
			boolean cambioRelacion = distintos(anteriorRelacion, contrato.getTipoRelacionLaboral());
			boolean cambioSectorial = distintos(anteriorSectorial, contrato.getCodigoSectorialIess());
			if (cambioRelacion || cambioSectorial) {
				StringBuilder detalle = new StringBuilder();
				if (cambioRelacion) {
					detalle.append("relacion laboral ").append(anteriorRelacion).append(" -> ")
							.append(contrato.getTipoRelacionLaboral());
				}
				if (cambioSectorial) {
					if (detalle.length() > 0) {
						detalle.append("; ");
					}
					detalle.append("codigo sectorial ").append(anteriorSectorial).append(" -> ")
							.append(contrato.getCodigoSectorialIess());
				}
				novedadIessService.generarCambioRelacionTrabajo(contrato.getCodigo(), vigencia,
						detalle.toString() + avisoFecha, contrato.getUsuarioRegistro());
			}
		} catch (Throwable e) {
			System.out.println("Aviso: no se pudo generar la novedad de cambio de relacion de trabajo"
					+ " del contrato " + contrato.getCodigo() + ": " + e.getMessage());
		}
		try {
			if (distintos(anteriorJornada, contrato.getJornada())
					|| distintos(anteriorDias, contrato.getDiasDeclaradosIess())) {
				novedadIessService.generarCambioJornada(contrato.getCodigo(), vigencia,
						anteriorSalario, contrato.getSalarioBase(),
						contrato.getDiasDeclaradosIess(), avisoFecha, contrato.getUsuarioRegistro());
			}
		} catch (Throwable e) {
			System.out.println("Aviso: no se pudo generar la novedad de cambio de jornada"
					+ " del contrato " + contrato.getCodigo() + ": " + e.getMessage());
		}
	}

	/**
	 * Compara dos valores tratando el nulo como un valor mas: pasar de nulo a algo, o de
	 * algo a nulo, es un cambio que el IESS tiene que conocer.
	 *
	 * @param anterior	: Valor antes del cambio
	 * @param actual	: Valor despues del cambio
	 * @return			: true si son distintos
	 */
	private boolean distintos(Object anterior, Object actual) {
		return anterior == null ? actual != null : !anterior.equals(actual);
	}

}
