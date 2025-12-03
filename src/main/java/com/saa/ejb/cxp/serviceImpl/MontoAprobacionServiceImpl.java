/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.serviceImpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.saa.basico.ejb.CantidadService;
import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.AprobacionXMontoDaoService;
import com.saa.ejb.cxp.dao.MontoAprobacionDaoService;
import com.saa.ejb.cxp.dao.UsuarioXAprobacionDaoService;
import com.saa.ejb.cxp.service.MontoAprobacionService;
import com.saa.ejb.cxp.service.TempAprobacionXMontoService;
import com.saa.ejb.cxp.service.TempMontoAprobacionService;
import com.saa.ejb.cxp.service.TempUsuarioXAprobacionService;
import com.saa.model.cxp.AprobacionXMonto;
import com.saa.model.cxp.MontoAprobacion;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempAprobacionXMonto;
import com.saa.model.cxp.TempMontoAprobacion;
import com.saa.model.cxp.TempUsuarioXAprobacion;
import com.saa.model.cxp.UsuarioXAprobacion;
import com.saa.rubros.Estado;
import com.saa.rubros.FormatoFecha;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz MontoAprobacionService.
 *  Contiene los servicios relacionados con la entidad MontoAprobacion</p>
 */
@Stateless
public class MontoAprobacionServiceImpl implements MontoAprobacionService {
	
	@EJB
	private MontoAprobacionDaoService montoAprobacionDaoService;
	
	@EJB
	private TempMontoAprobacionService tempMontoAprobacionService;
	
	@EJB
	private TempAprobacionXMontoService tempAprobacionXMontoService;
	
	@EJB
	private TempUsuarioXAprobacionService tempUsuarioXAprobacionService;
	
	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;
	
	@EJB
	private AprobacionXMontoDaoService aprobacionXMontoDaoService;
	
	@EJB
	private UsuarioXAprobacionDaoService usuarioXAprobacionDaoService;
	
	@EJB
	private CantidadService cantidadService;
	
	/* (non-Javadoc)
	 * @see com.saa.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<MontoAprobacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de montoAprobacion service");
		for (MontoAprobacion registro:lista) {			
			montoAprobacionDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de montoAprobacion service");
		//INSTANCIA UNA ENTIDAD
		MontoAprobacion montoAprobacion = new MontoAprobacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			montoAprobacionDaoService.remove(montoAprobacion, registro);	
			}				

	}
	
	/* (non-Javadoc)
	 * @see com.saa.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<MontoAprobacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) MontoAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MontoAprobacion> result = montoAprobacionDaoService.selectAll(NombreEntidadesPago.MONTO_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de montoAprobacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.Service.MontoAprobacionService#selectById(java.lang.Long)
	 */
	public MontoAprobacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return montoAprobacionDaoService.selectById(id, NombreEntidadesPago.MONTO_APROBACION);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.Service.MontoAprobacionService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<MontoAprobacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) MontoAprobacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<MontoAprobacion> result = montoAprobacionDaoService.selectByCriteria(datos, NombreEntidadesPago.MONTO_APROBACION); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de montoAprobacion no devolvio ningun registro");
			}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.saa.cxp.ejb.service.MontoAprobacionService#copiaFromTemporales(java.lang.Long)
	 */
	public void copiaFromTemporales(Long idEmpresa) throws Throwable {
		System.out.println("Service copiaFromTemporales de MontoAprobacion con idEmpresa:" + idEmpresa);
		Date date = new Date();
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA, FormatoFecha.EJB_CON_HORA));
		MontoAprobacion montoAprobacion = new MontoAprobacion();
		MontoAprobacion montoIngresado = new MontoAprobacion();
		AprobacionXMonto aprobacionXMonto = new AprobacionXMonto();
		AprobacionXMonto aprobacionIngresada = new AprobacionXMonto();
		UsuarioXAprobacion usuarioXAprobacion = new UsuarioXAprobacion();
		List<TempMontoAprobacion> tempMontoAprobacions = tempMontoAprobacionService.selectByEmpresa(idEmpresa);
		List<TempAprobacionXMonto> tempAprobacionXMontos = null;
		List<TempUsuarioXAprobacion> tempUsuarioXAprobacions = null;
		//ELIMINA LA PARAMETRIZACION ANTERIOR
		eliminaAprobacionEmpresa(idEmpresa);
		//COPIA DE LOS REGISTROS TEMPORALES
		for(TempMontoAprobacion registros : tempMontoAprobacions){
			montoAprobacion.setCodigo(null);
			montoAprobacion.setFechaIngreso(df.parse(df.format(date)));
			montoAprobacion.setEmpresa(registros.getEmpresa());
			montoAprobacion.setUsuarioIngresa(registros.getUsuarioIngresa());
			montoAprobacion.setValorDesde(registros.getValorDesde());
			montoAprobacion.setValorHasta(registros.getValorHasta());
			montoIngresado = montoAprobacionDaoService.saveMontoAprobacion(montoAprobacion);
			tempAprobacionXMontos = tempAprobacionXMontoService.selectByTempMontoAprobacion(registros.getCodigo());
			for(TempAprobacionXMonto regTempAprobacion : tempAprobacionXMontos){
				aprobacionXMonto.setCodigo(null);
				aprobacionXMonto.setMontoAprobacion(montoIngresado);
				aprobacionXMonto.setNombreNivel(regTempAprobacion.getNombreNivel());
				aprobacionXMonto.setEstado(Long.valueOf(Estado.ACTIVO));
				aprobacionXMonto.setFechaIngreso(df.parse(df.format(date)));				
				aprobacionXMonto.setUsuarioIngresa(regTempAprobacion.getUsuarioIngresa());
				aprobacionXMonto.setOrdenAprobacion(regTempAprobacion.getOrdenAprobacion());
				aprobacionXMonto.setSeleccionaBanco(regTempAprobacion.getSeleccionaBanco());
				aprobacionIngresada = aprobacionXMontoDaoService.saveAprobacionXMonto(aprobacionXMonto);
				tempUsuarioXAprobacions = tempUsuarioXAprobacionService.selectByTempAprobacionXMonto(regTempAprobacion.getCodigo());
				for(TempUsuarioXAprobacion regTempUsuario : tempUsuarioXAprobacions){
					usuarioXAprobacion.setCodigo(null);
					usuarioXAprobacion.setAprobacionXMonto(aprobacionIngresada);
					usuarioXAprobacion.setUsuario(regTempUsuario.getUsuario());
					usuarioXAprobacion.setFechaIngreso(df.parse(df.format(date)));
					usuarioXAprobacionDaoService.save(usuarioXAprobacion, usuarioXAprobacion.getCodigo());
				}
			}
		}
		// ELIMINA LAS TABLAS TEMPORALES
		eliminaTemporalesAprobacionEmpresa(idEmpresa);
	}

	/* (non-Javadoc)
	 * @see com.saa.cxp.ejb.service.MontoAprobacionService#eliminaAprobacionEmpresa(java.lang.Long)
	 */
	public void eliminaAprobacionEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Service eliminaAprobacionEmpresa de MontoAprobacion con idEmpresa:" + idEmpresa);
		usuarioXAprobacionDaoService.deleteByEmpresa(idEmpresa);
		aprobacionXMontoDaoService.deleteByEmpresa(idEmpresa);
		montoAprobacionDaoService.deleteByEmpresa(idEmpresa);
	}

	/* (non-Javadoc)
	 * @see com.saa.cxp.ejb.service.MontoAprobacionService#eliminaTemporalesAprobacionEmpresa(java.lang.Long)
	 */
	public void eliminaTemporalesAprobacionEmpresa(Long idEmpresa)
			throws Throwable {
		System.out.println("Service eliminaTemporalesAprobacionEmpresa de MontoAprobacion con idEmpresa:" + idEmpresa);
		tempUsuarioXAprobacionService.deleteByEmpresa(idEmpresa);
		tempAprobacionXMontoService.deleteByEmpresa(idEmpresa);
		tempMontoAprobacionService.deleteByEmpresa(idEmpresa);
	}

	/* (non-Javadoc)
	 * @see com.saa.cxp.ejb.service.MontoAprobacionService#verificaTraslapeValores(java.lang.Long, java.lang.Double, java.lang.Double)
	 */
	@SuppressWarnings("rawtypes")
	public boolean verificaTraslapeValores(Long idEmpresa, Double valorDesde,
			Double valorHasta) throws Throwable {
		System.out.println("Ingresa al verificaTraslapeValores con idEmpresa: " + idEmpresa + 
				", valorDesde: " + valorDesde + ", valorHasta : " + valorHasta);
		boolean resultado = false;
		List valores = montoAprobacionDaoService.selectValoresByEmpresa(idEmpresa);
		resultado = cantidadService.verificaTraslape(valores, valorDesde, valorHasta);
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.saa.cxp.ejb.service.MontoAprobacionService#validaCosistenciaDatosTemp(java.lang.Long)
	 */

	/* (non-Javadoc)
	 * @see com.saa.cxp.ejb.service.MontoAprobacionService#copiaToTemporales(java.lang.Long)
	 */
	public void copiaToTemporales(Long idEmpresa) throws Throwable {
		System.out.println("Service copiaToTemporales de MontoAprobacion con idEmpresa:" + idEmpresa);
		Date date = new Date();
		DateFormat df = new SimpleDateFormat(
				detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA, FormatoFecha.EJB_CON_HORA));
		TempMontoAprobacion tempMontoAprobacion = new TempMontoAprobacion();
		TempMontoAprobacion tempMontoIngresado = new TempMontoAprobacion();
		TempAprobacionXMonto tempAprobacionXMonto = new TempAprobacionXMonto();
		TempAprobacionXMonto tempAprobacionIngresada = new TempAprobacionXMonto();
		TempUsuarioXAprobacion tempUsuarioXAprobacion = new TempUsuarioXAprobacion();
		List<MontoAprobacion> montoAprobacions = montoAprobacionDaoService.selectByEmpresa(idEmpresa);
		List<AprobacionXMonto> aprobacionXMontos = null;
		List<UsuarioXAprobacion> usuarioXAprobacions = null;
		//ELIMINA LA PARAMETRIZACION ANTERIOR
		eliminaTemporalesAprobacionEmpresa(idEmpresa);
		//COPIA DE LOS REGISTROS TEMPORALES
		for(MontoAprobacion registros : montoAprobacions){
			tempMontoAprobacion.setCodigo(null);
			tempMontoAprobacion.setFechaIngreso(df.parse(df.format(date)));
			tempMontoAprobacion.setEmpresa(registros.getEmpresa());
			tempMontoAprobacion.setUsuarioIngresa(registros.getUsuarioIngresa());
			tempMontoAprobacion.setValorDesde(registros.getValorDesde());
			tempMontoAprobacion.setValorHasta(registros.getValorHasta());
			tempMontoIngresado = tempMontoAprobacionService.saveTempMontoAprobacion(tempMontoAprobacion);
			aprobacionXMontos = aprobacionXMontoDaoService.selectByMontoAprobacion(registros.getCodigo());
			for(AprobacionXMonto regTempAprobacion : aprobacionXMontos){
				tempAprobacionXMonto.setCodigo(null);
				tempAprobacionXMonto.setTempMontoAprobacion(tempMontoIngresado);
				tempAprobacionXMonto.setNombreNivel(regTempAprobacion.getNombreNivel());
				tempAprobacionXMonto.setEstado(Long.valueOf(Estado.ACTIVO));
				tempAprobacionXMonto.setFechaIngreso(df.parse(df.format(date)));				
				tempAprobacionXMonto.setUsuarioIngresa(regTempAprobacion.getUsuarioIngresa());
				tempAprobacionXMonto.setOrdenAprobacion(regTempAprobacion.getOrdenAprobacion());
				tempAprobacionXMonto.setSeleccionaBanco(regTempAprobacion.getSeleccionaBanco());
				tempAprobacionIngresada = tempAprobacionXMontoService.saveTempAprobacionXMonto(tempAprobacionXMonto);
				usuarioXAprobacions = usuarioXAprobacionDaoService.selectByAprobacionXMonto(regTempAprobacion.getCodigo());
				for(UsuarioXAprobacion regTempUsuario : usuarioXAprobacions){
					tempUsuarioXAprobacion.setCodigo(null);
					tempUsuarioXAprobacion.setTempAprobacionXMonto(tempAprobacionIngresada);
					tempUsuarioXAprobacion.setUsuario(regTempUsuario.getUsuario());
					tempUsuarioXAprobacion.setFechaIngreso(df.parse(df.format(date)));
					tempUsuarioXAprobacionService.saveTempUsuarioXAprobacion(tempUsuarioXAprobacion);
				}
			}
		}
	}

	@Override
	public MontoAprobacion saveSingle(MontoAprobacion montoAprobacion) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) MontoAprobacion");
		montoAprobacion = montoAprobacionDaoService.save(montoAprobacion, montoAprobacion.getCodigo());
		return montoAprobacion;
	}


}