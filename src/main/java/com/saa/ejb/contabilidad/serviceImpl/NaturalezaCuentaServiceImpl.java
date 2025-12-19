package com.saa.ejb.contabilidad.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.ejb.Mensaje;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.NaturalezaCuentaDaoService;
import com.saa.ejb.contabilidad.dao.PlanCuentaDaoService;
import com.saa.ejb.contabilidad.service.CentroCostoService;
import com.saa.ejb.contabilidad.service.NaturalezaCuentaService;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.model.contabilidad.CentroCosto;
import com.saa.model.contabilidad.NaturalezaCuenta;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.model.scp.DetalleRubro;
import com.saa.rubros.Estado;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NaturalezaCuentaServiceImpl implements NaturalezaCuentaService{
	
	@EJB
	private NaturalezaCuentaDaoService naturalezaCuentaDaoService;
	
	@EJB
	private EmpresaDaoService empresaDaoService;
	
	@EJB
	private CentroCostoService centroCostoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	@EJB
	private PlanCuentaDaoService planCuentaDaoService;
	
	@EJB
	private PlanCuentaService planCuentaService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de NaturalezaCuenta service ");
		//INSTANCIA LA ENTIDAD
		NaturalezaCuenta naturalezaCuenta = new NaturalezaCuenta();
		for (Long registro : id) {			
				naturalezaCuentaDaoService.remove(naturalezaCuenta, registro);	
			}				
			
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<NaturalezaCuenta> lista)throws Throwable {
		System.out.println("Ingresa al metodo save de naturalezaCuenta service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (NaturalezaCuenta registro: lista) {			
			//CONVIERTE REGISTRO DE OBJETO A ENTIDAD
			naturalezaCuentaDaoService.save(registro, registro.getCodigo());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<NaturalezaCuenta> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll NaturalezaCuentaService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<NaturalezaCuenta> result = naturalezaCuentaDaoService.selectAll(NombreEntidadesContabilidad.NATURALEZA_CUENTA); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total NaturalezaCuenta no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.NaturalezaCuentaService#recuperaIdNaturaleza(java.lang.String, java.lang.Long)
	 */
	public NaturalezaCuenta recuperaNaturalezaByCuenta(String cuenta, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo recuperaNaturalezaByCuenta con cuenta " + cuenta + " en empresa " + empresa);
		String cuentaBusqueda = cuenta;
		if ("0".equals(cuenta)){
			cuentaBusqueda = "1";
		}
		int posicion = cuentaBusqueda.indexOf('.');
		if (posicion < 1){
			posicion = cuentaBusqueda.length();
		}		
		//OBTIENE EL CODIGO DE LA CUENTA BASE
		List<NaturalezaCuenta> result1 = naturalezaCuentaDaoService.selectByNumeroEmpresaSinExce(cuentaBusqueda.substring(0, posicion), empresa);		
		NaturalezaCuenta naturalezaCuenta = new NaturalezaCuenta();
		if (result1.size() == 0){
			throw new Exception("NO EXISTE NATURALEZA PARA LA CUENTA " + cuenta);
		}else{
			naturalezaCuenta = result1.get(0);
		}
		return naturalezaCuenta;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<NaturalezaCuenta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) NaturalezaCuenta");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<NaturalezaCuenta> result = naturalezaCuentaDaoService.selectByCriteria(datos, NombreEntidadesContabilidad.NATURALEZA_CUENTA); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda por criterio de naturalezaCuenta no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.NaturalezaCuentaService#validaExisteNaturalezasBasicas(java.lang.Long)
	 */
	public String validaExisteNaturalezasBasicas(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo validaExisteNaturalezasBasicas de naturalezaCuenta con empresa: " + empresa);
		String resultado = Mensaje.OK;
		List<DetalleRubro> listado = detalleRubroService.selectByCodigoAlternoRubro(Rubros.GRUPOS_CUENTAS_BASICAS);
		for (DetalleRubro detalleRubro : listado) {
			if (detalleRubro.getCodigoAlterno() != 0) {
				NaturalezaCuenta naturalezaCuenta = naturalezaCuentaDaoService.selectByNumeroEmpresa(Long.valueOf(detalleRubro.getCodigoAlterno()), empresa);
				if (naturalezaCuenta.getManejaCentroCosto().equals(Long.valueOf(Estado.ACTIVO))) {
					// VERIFICA QUE EXISTAN CENTROS DE COSTO
					List<CentroCosto> centroCostos = centroCostoService.selectMovimientosByEmpresa(empresa);
					if (centroCostos.isEmpty()) {
						resultado = "NO EXISTEN CENTROS DE COSTO Y LA NATURALEZA " + naturalezaCuenta.getNombre() + " LOS REQUIERE";
						break;
					}
				}
			}
		}
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.NaturalezaCuentaService#selectById(java.lang.Long)
	 */
	public NaturalezaCuenta selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return naturalezaCuentaDaoService.selectById(id, NombreEntidadesContabilidad.NATURALEZA_CUENTA);
	}

	@Override
	public NaturalezaCuenta saveSingle(NaturalezaCuenta naturalezaCuenta) throws Throwable {
		System.out.println("saveSingle - NaturalezaCuenta");
		boolean esNuevo = false;
		if (naturalezaCuenta.getCodigo() == null) {
			esNuevo = true;
			naturalezaCuenta = naturalezaCuentaDaoService.save(naturalezaCuenta, naturalezaCuenta.getCodigo());
		}
		Long nivelPlanCuenta = 1L;
		// Actualiza plan Cuenta de nivel 1
		List<PlanCuenta> planCuentas = planCuentaDaoService.selectByNivelPlanCuenta(naturalezaCuenta.getCodigo(), nivelPlanCuenta);
		PlanCuenta planCuenta = new PlanCuenta();
		if (planCuentas.isEmpty()) {
			planCuenta.setCodigo(null);
			planCuenta.setNaturalezaCuenta(naturalezaCuenta);
			planCuenta.setCuentaContable(naturalezaCuenta.getNumero().toString());
			planCuenta.setNombre(naturalezaCuenta.getNombre());
			planCuenta.setTipo(Long.valueOf(TipoCuentaContable.ACUMULACION));
			planCuenta.setNivel(nivelPlanCuenta);
			System.out.println("Nivel Base: " + naturalezaCuenta.getEmpresa().getCodigo());
			planCuenta.setIdPadre(planCuentaDaoService.selectRaizByEmpresa(naturalezaCuenta.getEmpresa().getCodigo()).getCodigo());
			planCuenta.setEstado(Long.valueOf(Estado.ACTIVO));
			planCuenta.setEmpresa(naturalezaCuenta.getEmpresa());
			planCuenta.setFechaUpdate(LocalDate.now());
			planCuentaDaoService.save(planCuenta, null);
		} else{
			planCuenta = planCuentas.get(0);
			planCuenta.setNombre(naturalezaCuenta.getNombre());
			planCuentaDaoService.save(planCuenta, planCuenta.getCodigo());
		}
		if(!esNuevo) {
			//ACTUALIZA NATURALEZA CUENTA
			naturalezaCuenta = naturalezaCuentaDaoService.save(naturalezaCuenta, naturalezaCuenta.getCodigo());
		}
		return naturalezaCuenta; 
	}

	@Override
	public Long validaTieneCuentas(Long idNaturaleza) throws Throwable {
		System.out.println("Ingresa al metodo validaByNaturalezaCuenta de naturalezaCuenta con naturaleza: " + idNaturaleza);
		List<PlanCuenta> listado = planCuentaDaoService.selectByIdNaturalezaCuenta(idNaturaleza);
		return Long.valueOf(listado.size());
	}

	@Override
	public String eliminaNaturalezaCuenta(Long idNaturaleza) throws Throwable {
		System.out.println("eliminaNaturalezaCuenta naturaleza: " + idNaturaleza);
		String mensaje = Mensaje.OK;
		List<PlanCuenta> listado = planCuentaDaoService.selectByIdNaturalezaCuenta(idNaturaleza);
		if (!listado.isEmpty()) {
			for (PlanCuenta planCuenta : listado) {
				mensaje = planCuentaService.validaAsientoEnCuenta(planCuenta.getCodigo());
				if (mensaje.equals(Mensaje.OK)) {
					mensaje = planCuentaService.validaDetallePlantilla(planCuenta.getCodigo());
					if (!mensaje.equals(Mensaje.OK)) {
						break;
					}
				} else {
					break;
				}
			}
			if (mensaje.equals(Mensaje.OK)) {
				for (PlanCuenta planCuenta : listado) {
					planCuentaDaoService.remove(new PlanCuenta(), planCuenta.getCodigo());
				}
				//ELIMINA LA NATURALEZA DE CUENTA
				naturalezaCuentaDaoService.remove(new NaturalezaCuenta(), idNaturaleza);
			}
		} else {
			//ELIMINA LA NATURALEZA DE CUENTA
			naturalezaCuentaDaoService.remove(new NaturalezaCuenta(), idNaturaleza);
		}
		return mensaje;
	}

	@Override
	public String inactivaNaturalezaCuenta(Long idNaturaleza) throws Throwable {
		String mensaje = Mensaje.OK;
		List<PlanCuenta> listado = planCuentaDaoService.selectByIdNaturalezaCuenta(idNaturaleza);
		if (!listado.isEmpty()) {
			for (PlanCuenta planCuenta : listado) {
				planCuenta.setEstado(Long.valueOf(Estado.INACTIVO));
				planCuentaDaoService.save(planCuenta, planCuenta.getCodigo());
			}
		}
		NaturalezaCuenta naturalezaCuenta = naturalezaCuentaDaoService.selectById(idNaturaleza, NombreEntidadesContabilidad.NATURALEZA_CUENTA);
		naturalezaCuenta.setEstado(Long.valueOf(Estado.INACTIVO));
		naturalezaCuentaDaoService.save(naturalezaCuenta, naturalezaCuenta.getCodigo());
		return mensaje;
	}
	
}
