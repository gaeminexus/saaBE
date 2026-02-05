
/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.tesoreria.dao.MotivoPagoDaoService;
import com.saa.ejb.tesoreria.dao.PagoDaoService;
import com.saa.ejb.tesoreria.dao.TempMotivoPagoDaoService;
import com.saa.ejb.tesoreria.service.PagoService;
import com.saa.ejb.tesoreria.service.PersonaCuentaContableService;
import com.saa.ejb.tesoreria.service.TempPagoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.MotivoPago;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Pago;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.TempMotivoPago;
import com.saa.model.tsr.TempPago;
import com.saa.rubros.EstadoPago;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PagoService.
 *  Contiene los servicios relacionados con la entidad Pago.</p>
 */
@Stateless
public class PagoServiceImpl implements PagoService {
	
	@EJB
	private PagoDaoService pagoDaoService;
	
	@EJB	
	private TempPagoService tempPagoService;
	
	@EJB	
	private TempMotivoPagoDaoService tempMotivoPagoDaoService;	
	
	@EJB	
	private MotivoPagoDaoService motivoPagoDaoService;
	
	@EJB
	private AsientoService asientoService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;
	
	@EJB
	private PersonaCuentaContableService personaCuantaContableService;	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de Pago service ... depurado");
		//INSTANCIA LA ENTIDAD
		Pago pago = new Pago();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			pagoDaoService.remove(pago, registro);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<Pago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de Pago service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Pago pago : lista) {			
			pagoDaoService.save(pago, pago.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<Pago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PagoService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Pago> result = pagoDaoService.selectAll(NombreEntidadesTesoreria.PAGO);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Pago no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Pago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) Pago");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<Pago> result = pagoDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.PAGO
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de Pago no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.PagoService#selectById(java.lang.Long)
	 */
	public Pago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return pagoDaoService.selectById(id, NombreEntidadesTesoreria.PAGO);
	}

	public String[] crearPagoIngresado(Long idTempPago) throws Throwable {
		System.out.println("Ingresa al metodo crearPagoIngresados con id temporal de pago: " + idTempPago);
		//COPIA LA INFORMACION DE LAS TABLAS TEMPORALES A LAS REALES Y OBTIENE DATOS DEL COBRO
		Pago pago = copiarPagoTemporalAReal(idTempPago);
		//CREAR EL ASIENTO CONTABLE DEL COBRO
		Long[] datosAsiento = crearAsientoPago(pago);
		
		//ELIMINA LOS DATOS DE LAS TABLAS TEMPORALES MOTIVO PAGO
		
		tempPagoService.eliminarPagosTemporales(pago.getUsuario().getCodigo());
		String[] resultado = new String[4];
		resultado[0] = datosAsiento[0].toString();
		resultado[1] = pago.getCodigo().toString();
		resultado[2] = datosAsiento[1].toString();
		resultado[3] = "ID DE PAGO: " + pago.getCodigo() + "/  ASIENTO: " + datosAsiento[1];
		return resultado;
	}	
	

	public Pago copiarPagoTemporalAReal(Long idTempPago) throws Throwable {
		System.out.println("Ingresa al metodo copiarPagoTemporalAReal con id temporal de pago: " + idTempPago);
		Pago pago = new Pago();
		TempPago tempPago = tempPagoService.selectById(idTempPago);
		pago = savePagoReal(tempPago);
		return pago;
	}
	
	public Pago savePagoReal(TempPago tempPago) throws Throwable{
		System.out.println("Ingresa al metodo savePagoReal con id temporal de pago: " + tempPago.getCodigo());
		Double totalPago = 0.0D;
		
		Pago pago = new Pago();
		pago.setAsiento(tempPago.getAsiento());
		pago.setCheque(tempPago.getCheque());
		pago.setCodigo(0L);		
		pago.setDescripcion(tempPago.getDescripcion());
		pago.setEmpresa(tempPago.getEmpresa());
		pago.setFechaInactivo(tempPago.getFechaInactivo());
		pago.setFechaPago(tempPago.getFechaPago());
		pago.setIdTempPago(tempPago.getCodigo());
		pago.setNombreUsuario(tempPago.getNombreUsuario());
		pago.setNumeroAsiento(tempPago.getNumeroAsiento());
		pago.setNumeroId(tempPago.getNumeroId());
		pago.setPersona(tempPago.getPersona());
		pago.setProveedor(tempPago.getProveedor());
		pago.setRubroEstadoH(tempPago.getRubroEstadoH());
		pago.setRubroEstadoP(tempPago.getRubroEstadoP());
		pago.setRubroMotivoAnulacionH(tempPago.getRubroMotivoAnulacionH());
		pago.setRubroMotivoAnulacionP(tempPago.getRubroMotivoAnulacionP());
		pago.setTipoId(tempPago.getTipoId());
		pago.setTipoPago(tempPago.getTipoPago());
		pago.setUsuario(tempPago.getUsuario());
		pago.setValor(tempPago.getValor());
		
		pagoDaoService.save(pago, pago.getCodigo());		
		pago = pagoDaoService.recuperaIdPago(pago.getIdTempPago());	
		List<TempMotivoPago> tempMotivoPagos = tempMotivoPagoDaoService.selectByIdTempPago(tempPago.getCodigo());
		for (TempMotivoPago tempMotivoPago : tempMotivoPagos){			
			MotivoPago motivoPago = new MotivoPago();
			motivoPago.setCodigo(0L);
			motivoPago.setDescripcion(tempMotivoPago.getDescripcion());			
			motivoPago.setDetallePlantilla(tempMotivoPago.getDetallePlantilla());
			motivoPago.setPago(pago);
			motivoPago.setPlantilla(tempMotivoPago.getPlantilla());
			motivoPago.setValor(tempMotivoPago.getValor());
			
			totalPago += tempMotivoPago.getValor();			
			motivoPagoDaoService.save(motivoPago, motivoPago.getCodigo());						
		}	
		//Actualiza valor del Pago		
		pago.setValor(totalPago);				
		pagoDaoService.save(pago, pago.getCodigo());
		pago=pagoDaoService.selectById(pago.getCodigo(), NombreEntidadesTesoreria.PAGO);
	
		return pago;		
	}

	public List<Pago> recuperarPagoIdCheque(Long idCheque)throws Throwable {	
		return pagoDaoService.recuperaIdCheque(idCheque);
	}
	
	public Long[] crearAsientoPago(Pago pago) throws Throwable {
		System.out.println("Ingresa al metodo crearAsientoPago con id de pago: " + pago.getCodigo());
		Double sumDebe = 0.0D;
		Double sumHaber = 0.0D;		

		Long[] datosAsiento = asientoService.insertarCabeceraAsiento(pago.getEmpresa().getCodigo(),
			pago.getNombreUsuario(), 
			"PAGO Nro "+pago.getCodigo()+
			" A PROVEEDOR "+pago.getProveedor()+
			". DESCRIPCION: "+pago.getDescripcion(),
			TipoAsientos.EGRESOS);		
		
		Asiento asiento = asientoService.selectById(datosAsiento[0]);
		List<MotivoPago> detalle = motivoPagoDaoService.selectByIdPago(pago.getCodigo());		
		if(!detalle.isEmpty()){
			for (MotivoPago motivoPago : detalle) {		
				detalleAsientoService.insertarDetalleAsientoDebe(
				motivoPago.getDetallePlantilla().getPlanCuenta(),
				"MOTIVO DE PAGO: "+motivoPago.getDescripcion(),
				motivoPago.getValor(),
				asiento, null);
				sumDebe += motivoPago.getValor();
			}				
		}		

		PlanCuenta planCuentaHaber = new PlanCuenta();
		List<PersonaCuentaContable> planes = personaCuantaContableService.selectByPersonaTipoCuenta(pago.getEmpresa().getCodigo(),pago.getPersona().getCodigo(),
			RolPersona.PROVEEDOR, pago.getTipoPago());
		if(!planes.isEmpty()){
			planCuentaHaber=planes.get(0).getPlanCuenta();
		}else{
			String mensaje = "NO EXISTE DEFINICION CONTABLE DE PROVEEDOR ";
			throw new IncomeException(mensaje);			
		}		

		detalleAsientoService.insertarDetalleAsientoHaber(
				planCuentaHaber,
				"PAGO A PROVEEDOR: " + pago.getProveedor(),
				pago.getValor(),
				asiento, null);
		sumHaber = pago.getValor();	
		
		if(!sumDebe.equals(sumHaber)){
			String mensaje = "EL VALOR DEL DEBE [" + sumDebe + "] ES DIFERENTE AL DEL HABER ["+
			sumHaber + "], CODIGO DE ASIENTO "+asiento.getCodigo();	
			throw new IncomeException(mensaje);
		}		

		pago.setAsiento(asiento);
		pago.setNumeroAsiento(asiento.getNumero());		
		pagoDaoService.save(pago, pago.getCodigo());	

		return datosAsiento;
	}

	public void anularIngresoPago(Long idPago, int motivoAnular)
			throws Throwable {
		System.out.println("Ingresa al metodo anularIngresoPago con id de pago: " + idPago + " motivo: " + motivoAnular);
		//Obtener Asiento
		Pago pago = pagoDaoService.selectById(idPago, NombreEntidadesTesoreria.PAGO);
		Asiento asiento = pago.getAsiento();
		//Validar si puedo reversar pago
		if(validaAnularPago(pago, asiento)){
			//Actualiza estado del pago
			pago.setFechaInactivo(LocalDateTime.now());
			pago.setRubroEstadoH(Long.valueOf(String.valueOf(EstadoPago.ANULADO)));
			pagoDaoService.save(pago, pago.getCodigo());
			//Anula asiento contable			
			asientoService.anulaAsiento(asiento.getCodigo());
		}		
	}
	
	public boolean validaAnularPago(Pago pago, Asiento asiento) throws Throwable {
		boolean verifica = true;
		System.out.println("Ingresa al metodo validaAnularPago con id de pago: " + pago.getCodigo() 
			+ " asiento: " + asiento.getCodigo());
		if(pago.getRubroEstadoH()==(Long.valueOf(String.valueOf(EstadoPago.ANULADO)))){
			verifica = false;
			throw new IncomeException("NO SE PUEDE REVERSAR UN PAGO QUE ESTA ANULADO");
		}			
		if(pago.getRubroEstadoH()==(Long.valueOf(String.valueOf(EstadoPago.IMPRESO)))){
			verifica = false;
			throw new IncomeException("EL PAGO YA HA SIDO GENERADO EN UN CHEQUE");
		}
		return verifica;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.PagoService#updateEstadoIdChequeByIdCheque(java.lang.Long, int)
	 */
	public void updateEstadoIdChequeByIdCheque(Long idCheque, int estado)
		throws Throwable {
		System.out.println("Ingresa al metodo updateEstadoIdChequeByIdCheque con id de cheque: " + idCheque 
				+ " estado: " + estado);
		pagoDaoService.updateEstadoIdChequeByIdCheque(idCheque, estado);
		
	}

	@Override
	public Pago saveSingle(Pago pago) throws Throwable {
		System.out.println("saveSingle - Pago");
		pago = pagoDaoService.save(pago, pago.getCodigo());
		return pago;
	}

	
	
}
