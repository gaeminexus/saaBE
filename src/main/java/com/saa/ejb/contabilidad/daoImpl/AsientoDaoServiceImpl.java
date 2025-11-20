/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.daoImpl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.AsientoDaoService;
import com.saa.model.contabilidad.Asiento;
import com.saa.rubros.EstadoAsiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 *         Clase AsientoDaoServiceImpl.
 */
@Stateless
public class AsientoDaoServiceImpl extends EntityDaoImpl<Asiento> implements AsientoDaoService {

	//Inicializa persistence context
		@PersistenceContext
		EntityManager em;
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
		 */
		public String[] obtieneCampos() {
			System.out.println("Ingresa al metodo (campos) Ambito");
			return new String[]{"codigo",
								"empresa",
								"tipoAsiento",
								"fechaAsiento",
								"numero",
								"estado",
								"observaciones",
								"nombreUsuario",
								"idReversion",
								"numeroMes",
								"numeroAnio",
								"moneda",
								"mayorizacion",
								"rubroModuloClienteP",
								"rubroModuloClienteH",
								"fechaIngreso",
								"periodo",
								"rubroModuloSistemaP",
								"rubroModuloSistemaH"};
		}
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectAniosAsiento(java.lang.Long)
		 */
		@SuppressWarnings("unchecked")
		public List<Long> selectAniosAsiento(Long empresa) throws Throwable {
			System.out.println("Ingresa al metodo selectAniosAsiento de asiento con empresa: " + empresa);
			Query query = em.createQuery(" select   distinct b.numeroAnio " +
										 " from     Asiento b " +
										 " where    b.empresa.codigo = :empresa " +
										 " order by b.numeroAnio");
			query.setParameter("empresa", empresa);		
			return query.getResultList();
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectUsuario(java.lang.Long)
		 */
		@SuppressWarnings("unchecked")
		public List<String> selectUsuario(Long empresa) throws Throwable {		
			System.out.println("Ingresa al metodo selectUsuario de asiento con empresa: " + empresa);
			Query query = em.createQuery(" select   distinct Upper(b.nombreUsuario) " +
										 " from     Asiento b " +
										 " where    b.empresa.codigo = :empresa " +
										 " order by Upper(b.nombreUsuario)");
			query.setParameter("empresa", empresa);		
			return query.getResultList();
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectAsientoReverso(java.lang.Long)
		 */
		@SuppressWarnings("unchecked")
		public List<Asiento> selectAsientoReverso(Long empresa) throws Throwable {
			System.out.println("Ingresa al metodo selectAsientoReverso de asiento con empresa: " + empresa);
			Query query = em.createQuery(" select   distinct b " +
										 " from     Asiento b " +
										 " where    b.empresa.codigo = :empresa " +
										 "          and b.codigo in ( Select   c.idReversion" +
										 " 							 from     Asiento c " +
										 "	                         where    c.idReversion IS NOT NULL " +
										 "                                    and c.empresa.codigo = :empresa)" +
										 " order by b.numero");
			query.setParameter("empresa", empresa);		
			return query.getResultList();
		}
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectByMesAnio(java.lang.Long, java.lang.Long, java.lang.Long)
		 */
		@SuppressWarnings("unchecked")
		public List<Asiento> selectByMesAnio(Long mes, Long anio, Long empresa) throws Throwable {
			System.out.println("Ingresa al metodo selectByMesAnio de anio: " + anio + ", mes: " + mes + ", empresa: " + empresa);
			Query query = em.createQuery(" select b " +
										 " from   Asiento b " +
										 " where  b.empresa.codigo = :empresa " +
										 "        and   b.numeroMes = :mes" +
										 "        and   b.numeroAnio = :anio " +
										 "        and   b.estado in (" + EstadoAsiento.ACTIVO + "," + EstadoAsiento.REVERSADO + ")");
			query.setParameter("empresa", empresa);		
			query.setParameter("mes", mes);
			query.setParameter("anio", anio);
			return query.getResultList();
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectMaxNumero(java.lang.Long, java.lang.Long)
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public List selectMaxNumero(Long tipo, Long empresa) throws Throwable {
			System.out.println("Ingresa al metodo selectMaxNumero de tipo: " + tipo + " en empresa: " + empresa);
			Query query = em.createQuery(" select   Max(b.numero) " +
										 " from     Asiento b " +
										 " where    b.empresa.codigo = :empresa " +
										 "          and b.tipoAsiento.codigo = :tipo");
			query.setParameter("empresa", empresa);		
			query.setParameter("tipo", tipo);
			return query.getResultList();
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectAsientoCierre(java.lang.Long, java.lang.Long, java.lang.Long)
		 */
		public Asiento selectAsientoCierre(Long mes, Long anio, Long tipo, Long empresa) throws Throwable {
			System.out.println("Ingresa al metodo selectAsientoCierre de mes: " + mes + " en anio: " + anio + " en empresa: " + empresa + ", tipo: " + tipo);
			Query query = em.createQuery(" select b " +
										 " from   Asiento b " +
										 " where  b.empresa.codigo = :empresa " +
										 "        and   b.tipoAsiento.codigo = :tipo " +
										 "        and   b.numeroMes = :mes " +
										 "        and   b.numeroAnio = :anio " +
										 "        and   b.estado != :inactivo");
			query.setParameter("empresa", empresa);		
			query.setParameter("tipo", tipo);
			query.setParameter("mes", mes);
			query.setParameter("anio", anio);
			query.setParameter("inactivo", Long.valueOf(EstadoAsiento.ANULADO));

			return (Asiento)query.getSingleResult();
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectByNumeroEmpresaTipo(java.lang.Long, java.lang.Long, java.lang.Long)
		 */
		public Asiento selectByNumeroEmpresaTipo(Long numero, Long empresa, Long tipo) throws Throwable {
			System.out.println("Ingresa al metodo selectByNumeroEmpresaTipo de empresa: " + empresa + " con numero: " + numero + " y tipo: " + tipo);
			Query query = em.createQuery(" select b " +
										 " from   Asiento b " +
					                     " where  b.empresa.codigo = :empresa " +
					                     "        and   b.tipoAsiento.codigo = :tipo " +
							             "        and   numero = :numero");
			query.setParameter("empresa", empresa);		
			query.setParameter("tipo", tipo);
			query.setParameter("numero", numero);

			return (Asiento)query.getSingleResult();
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectByMayorizacion(java.lang.Long)
		 */
		@SuppressWarnings("unchecked")
		public List<Asiento> selectByMayorizacion(Long idMayorizacion) throws Throwable {
			System.out.println("Ingresa al metodo selectByMayorizacion con mayorizacion: " + idMayorizacion);
			Query query = em.createQuery(" select b " +
										 " from   Asiento b" +
										 " where  b.mayorizacion.codigo = :idMayorizacion");
			query.setParameter("idMayorizacion", idMayorizacion);
			return  query.getResultList();
		}

		/* (non-Javadoc)
		 * @see com.compuseg.income.contabilidad.ejb.dao.AsientoDaoService#selectByConsultaCamposAsiento(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.String, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.util.Date, java.util.Date, java.util.Date, java.util.Date)
		 */
		@SuppressWarnings("unchecked")
		public List<Asiento> selectByConsultaCamposAsiento(Long idEmpresa,
				Long tipoAsiento, Long numero, Long estado, String nombreUsuario,
				Long rubroModuloClienteH, Long idReversion, Long numeroAnio,
				Long numeroMes, 
				Date fechaIngresoDesde, Date fechaIngresoHasta,
				Date fechaAsientoDesde, Date fechaAsientoHasta) throws Throwable {
			System.out.println("Ingresa al Metodo selectByConsultaCamposAsiento con idEmpresa:" + idEmpresa + 
					 ", tipoAsiento" + tipoAsiento + ", numero" + numero + ", estado" + estado + ", nombreUsuario" + nombreUsuario +
					 ", rubroModuloClienteH" + rubroModuloClienteH + ", idReversion" + idReversion + ", numeroAnio" + numeroAnio + 
					 ", numeroMes" + numeroMes +  
					 ", fechaIngresoDesde" + fechaIngresoDesde + ", fechaIngresoHasta" + fechaIngresoHasta +
					 ", fechaAsientoDesde" + fechaAsientoDesde + ", fechaAsientoHasta" + fechaAsientoHasta);
			
			Calendar desde = Calendar.getInstance();
			desde.setTime(fechaIngresoDesde);
			
			Calendar hasta = Calendar.getInstance();
			hasta.setTime(fechaIngresoHasta);
			
			Calendar desdeAsiento = Calendar.getInstance();
			desdeAsiento.setTime(fechaAsientoDesde);
			
			Calendar hastaAsiento = Calendar.getInstance();
			hastaAsiento.setTime(fechaAsientoHasta);
			
			//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
			String strQuery = " select b " +
							  " from    Asiento b" +
							  " where   b.empresa.codigo = :idEmpresa ";
			if(tipoAsiento != 0L){
				strQuery +=   " 		and  b.tipoAsiento.codigo = :tipoAsiento";
			}
			if(numero != 0L){
				strQuery +=   " 		and  b.numero = :numero";
			}
			if(estado != 0L){
				strQuery +=   "			and  b.estado = :estado";		
			}
			if(!"null".equals(nombreUsuario)){
				strQuery +=   "			and  b.nombreUsuario = :nombreUsuario";
			}
			if(rubroModuloClienteH != 0L){
				strQuery +=   "  		and  b.rubroModuloClienteH = :rubroModuloClienteH";		
			}
			if(idReversion != 0L){
				strQuery +=	  "			and  b.idReversion = :idReversion";
			}
			if(numeroAnio != 0L){
				strQuery +=   " 		and  b.numeroAnio = :numeroAnio";
			}
			if(numeroMes != 0L){
				strQuery +=   " 		and  b.numeroMes = :numeroMes";	
			}
			if(desde.get(Calendar.YEAR) != 1900){
				strQuery +=   " 		and trunc(b.fecheIngreso) between :fechaIngresoDesde and :fechaIngresoHasta";
			}
			if(desdeAsiento.get(Calendar.YEAR) != 1900){
				strQuery +=   " 		and trunc(b.fechaAsiento) between :fechaAsientoDesde and :fechaAsientoHasta";
			}
			
			Query query = em.createQuery(strQuery);		
			if(idEmpresa != 0L){
				query.setParameter("idEmpresa", idEmpresa);
			}
			if(tipoAsiento != 0L){
				query.setParameter("tipoAsiento", tipoAsiento);
			}
			if(numero != 0L){
				query.setParameter("numero", numero);
			}
			if(estado != 0L){
				query.setParameter("estado", estado);
			}
			if(!"null".equals(nombreUsuario)){
				query.setParameter("nombreUsuario", nombreUsuario);
			}
			if(rubroModuloClienteH != 0L){
				query.setParameter("rubroModuloClienteH", rubroModuloClienteH);
			}
			if(idReversion != 0L){
				query.setParameter("idReversion", idReversion);
			}
			if(numeroAnio != 0L){
				query.setParameter("numeroAnio", numeroAnio);
			}
			if(numeroMes != 0L){
				query.setParameter("numeroMes", numeroMes);
			}
			if(desde.get(Calendar.YEAR) != 1900){			
				query.setParameter("fechaIngresoDesde", fechaIngresoDesde);
				if(hasta.get(Calendar.YEAR) != 1900){				
					query.setParameter("fechaIngresoHasta", fechaIngresoHasta);
				}else{				
					query.setParameter("fechaIngresoHasta", fechaIngresoDesde);
				}
			}
			if(desdeAsiento.get(Calendar.YEAR) != 1900){			
				query.setParameter("fechaAsientoDesde", fechaAsientoDesde);
				if(hastaAsiento.get(Calendar.YEAR) != 1900){				
					query.setParameter("fechaAsientoHasta", fechaAsientoHasta);
				}else{				
					query.setParameter("fechaAsientoHasta", fechaAsientoDesde);
				}
			}
			return query.getResultList();
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<Asiento> selectByIdPeriodo(Long idPeriodo) throws Throwable {
			System.out.println("Ingresa al metodo selectByIdPeriodo con idPeriodo: " + idPeriodo);
			Query query = em.createQuery(" select b " +
										 " from   Asiento b" +
										 " where  b.periodo.codigo = :idPeriodo");
			query.setParameter("idPeriodo", idPeriodo);
			return  query.getResultList();
		}	
}
