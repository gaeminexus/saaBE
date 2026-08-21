/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.rubros.RhhEstadoEmpleado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ContratoEmpleadoDaoService. 
 */
@Stateless
public class ContratoEmpleadoDaoServiceImpl extends EntityDaoImpl<ContratoEmpleado>  implements ContratoEmpleadoDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ContratoEmpleado");
		return new String[]{"codigo",
							"empleado",
							"tipoContratoEmpleado",
							"numero",
							"fechaInicio",
							"fechaFin",
							"salarioBase",
							"estado",
							"fechaFirma",
							"observacion",
							"fechaRegistro",
							"usuarioRegistro",
							"tipoRelacionLaboral",
							"jornada",
							"horasSemanales",
							"valorHora",
							"modalidadDecimoTercero",
							"modalidadDecimoCuarto",
							"modalidadFondosReserva",
							"derechoDecimoCuarto",
							"aportaIess",
							"retieneFuente",
							"porcentajeRetencionFuente",
							"ocupacionMdt",
							"causalTerminacion",
							"fechaTerminacion",
							"centroCosto",
							"turno"};
	}
	

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService#selectActivosEnPeriodo(java.lang.Long, java.time.LocalDate, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ContratoEmpleado> selectActivosEnPeriodo(Long idEmpresa, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable {
		System.out.println("Ingresa al metodo selectActivosEnPeriodo de ContratoEmpleado, del "
				+ desde + " al " + hasta);
		// La empresa se compara contra la del empleado porque el contrato no la lleva.
		// Se admite null para no dejar fuera a los empleados aun sin empresa asignada.
		//
		// QUIEN SALIO DESPUES DE ESTE MES SI ESTABA ESTE MES.
		//
		// El estado del empleado es de HOY; el periodo que se recalcula puede ser de hace
		// cinco meses. Filtrar por `estado <> CESANTE` a secas hacia desaparecer de enero a
		// quien se fue en marzo: al recalcular enero salian 20 personas en vez de 22. Es la
		// misma familia que se corrigio en generarRdep --el maestro de hoy decidiendo sobre
		// un periodo pasado--, aqui en el selector propio de la nomina.
		//
		// LA ASIMETRIA ES DELIBERADA, y la marca la fecha de terminacion, no el estado:
		//
		//   - Contrato SIN fecha de terminacion -> se mira el estado del empleado. Es la red
		//     de seguridad original: alguien liquidado cuyo contrato se quedo sin fecha por
		//     un olvido de captura no debe volver a entrar en ningun rol.
		//   - Contrato CON fecha de terminacion -> se mira SOLO la fecha, y con `> :hasta`,
		//     no `>= :desde`. El mes de la salida NO va por nomina: lo paga el finiquito.
		//     Con `>= :desde`, Castro Arce y Cevallos Aleman --salida 06-03-- volverian a
		//     entrar en marzo, que esta correcto con 20 porque a ellos los pago la
		//     liquidacion. Verificado en los datos: Torres (15-01) y Benitez (16-01) no estan
		//     en enero; Castro y Cevallos si estan en enero y febrero, y no en marzo.
		Query query = em.createQuery(" select   t "
				+ " from     ContratoEmpleado t "
				+ " where    (t.empleado.empresa is null or t.empleado.empresa.codigo = :idEmpresa) "
				+ "          and t.fechaInicio <= :hasta "
				+ "          and (t.fechaFin is null or t.fechaFin >= :desde) "
				+ "          and ( "
				+ "                (t.fechaTerminacion is null "
				+ "                     and (t.empleado.estado is null or t.empleado.estado <> :cesante)) "
				+ "             or (t.fechaTerminacion is not null and t.fechaTerminacion > :hasta) "
				+ "          ) "
				+ " order by t.empleado.apellidos, t.empleado.nombres ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("cesante", Long.valueOf(RhhEstadoEmpleado.CESANTE));
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		return query.getResultList();
	}
}
