package com.saa.ejb.contabilidad.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.ejb.FechaService;
import com.saa.basico.ejb.Mensaje;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DetalleAsientoDaoService;
import com.saa.ejb.contabilidad.dao.DetalleMayorizacionDaoService;
import com.saa.ejb.contabilidad.dao.DetallePlantillaDaoService;
import com.saa.ejb.contabilidad.dao.NaturalezaCuentaDaoService;
import com.saa.ejb.contabilidad.dao.PlanCuentaDaoService;
import com.saa.ejb.contabilidad.service.CuentaService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.contabilidad.service.DetalleMayorizacionService;
import com.saa.ejb.contabilidad.service.NaturalezaCuentaService;
import com.saa.ejb.contabilidad.service.PeriodoService;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.model.contabilidad.DetalleAsiento;
import com.saa.model.contabilidad.DetalleMayorizacion;
import com.saa.model.contabilidad.DetallePlantilla;
import com.saa.model.contabilidad.NaturalezaCuenta;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.Periodo;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.model.scp.DetalleRubro;
import com.saa.model.scp.Empresa;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * <p>Implementación de la interfaz PlanCuentaService.
 *  Contiene los servicios relacionados con la entidad PlanCuenta.</p>
 */
@Stateless
public class PlanCuentaServiceImpl implements PlanCuentaService{

	
	@EJB
	private PlanCuentaDaoService planCuentaDaoService;	
	
	@EJB
	private NaturalezaCuentaDaoService naturalezaCuentaDaoService;
	
	@EJB
	private NaturalezaCuentaService naturalezaCuentaService;
	
	@EJB
	private EmpresaDaoService empresaDaoService;	
	
	@EJB
	private CuentaService cuentaService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;	
	
	@EJB
	private PeriodoService periodoService;	
	
	@EJB
	private DetalleMayorizacionService detalleMayorizacionService;	
	
	@EJB
	private FechaService fechaService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	@EJB
	private DetalleAsientoDaoService detalleAsientoDaoService;
	
	@EJB
	private DetallePlantillaDaoService detallePlantillaDaoService;
	
	@EJB
	private DetalleMayorizacionDaoService detalleMayorizacionDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Servicio remove[] de PlanCuenta service");
		for (Long registro : id) {
			Long tipo = recuperaTipo(registro);
			// CUENTA MOVIMIENTO
			if (Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(tipo)){
				removeMovimiento(registro);				
			}
			// CUENTA ACUMULACION
			if (Long.valueOf(TipoCuentaContable.ACUMULACION).equals(tipo)){
				removeAcumulacion(registro, false);
			}				
		}		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<PlanCuenta>)
	 */
	public void save(List<PlanCuenta> lista) throws Throwable {
		System.out.println("Servicio save de planCuenta service");
		for (PlanCuenta registro : lista) {
			planCuentaDaoService.save(registro, registro.getCodigo());
        }
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<PlanCuenta> selectAll() throws Throwable {
		System.out.println("Servicio selectAll PlanCuentaService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PlanCuenta> result = planCuentaDaoService.selectAll(NombreEntidadesContabilidad.PLAN_CUENTA); 
		if (result.isEmpty()) {
            throw new IncomeException("Busqueda total PlanCuenta no devolvio ningun registro");
        }
        return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<PlanCuenta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Servicio (selectByCriteria) PlanCuenta");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PlanCuenta> result = planCuentaDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.PLAN_CUENTA); 
		if (result.isEmpty()) {
            throw new IncomeException("Busqueda total PlanCuenta no devolvio ningun registro");
        }
        return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#numeroRegistrosEmpresa(java.lang.Long)
	 */
	public int numeroRegistrosEmpresa(Long empresa) throws Throwable {
		System.out.println("Servicio numeroRegistrosEmpresa PlanCuentaService con empresa: " + empresa);
		//CREA SENTENCIA WHERE PARA VERIFICAR SI EXISTEN REGISTROS
		List<PlanCuenta> result = planCuentaDaoService.selectByEmpresa(empresa);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		return result.size();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#creaNodoArbolCero(java.lang.Long)
	 */
	public String creaNodoArbolCero(Long empresa) throws Throwable {
		System.out.println("Servicio creaNodoArbolCero PlanCuentaService con empresa: " + empresa);
		//INICIALIZA VARIABLE DE RESULTADO
		String resultado = Mensaje.OK;
		int numeroRegistros = numeroRegistrosEmpresa(empresa);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		if (numeroRegistros == 0) {
			//OBTIENE LA NATURALEZA DE CUENTA DE ACTIVOS PARA ESTA EMPRESA
			NaturalezaCuenta naturalezaCuenta = naturalezaCuentaDaoService.selectByNumeroEmpresa(Long.valueOf(GRUPO_ACTIVO), empresa);
			if (naturalezaCuenta == null){
				resultado = "NO EXISTE GRUPO DE CUENTA ACTIVO POR LO QUE NO SE PODRA CREAR EL NODO";
			}else{
				//CREA EL OBJETO PARA PERSISTIRLO
				PlanCuenta planCuenta = new PlanCuenta();
				planCuenta.setCuentaContable("0");
				planCuenta.setNaturalezaCuenta(naturalezaCuenta);
				planCuenta.setNombre("PLAN DE CUENTAS");
				planCuenta.setTipo(Long.valueOf(TipoCuentaContable.ACUMULACION));
				planCuenta.setNivel(0L);
				planCuenta.setIdPadre(0L);
				planCuenta.setEstado(Long.valueOf(Estado.ACTIVO));
				planCuenta.setEmpresa((Empresa)empresaDaoService.find(new Empresa(), empresa));
				//PERSISTE EL OBJETO
				planCuentaDaoService.save(planCuenta, planCuenta.getCodigo());
				//RETORNA VARIABLE CON OK
				resultado = Mensaje.OK;
			}
			
		}else{
			System.out.println("EXISTEN REGISTROS EN EL PLAN DE CUENTA POR LO QUE NO SE CREARÁ EL NODO PRINCIPAL");
			resultado = Mensaje.OK;
		}
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#saveCuenta(java.lang.List<PlanCuenta>, java.lang.Object[])
	 */
	public String saveCuenta(List<PlanCuenta> object, Long empresa) throws Throwable {
		System.out.println("Servicio saveCuenta de planCuenta service");
		//INSTANCIA NUEVA ENTIDAD PARA PADRE
		PlanCuenta planCuentaPadre = new PlanCuenta();
		String resultado = Mensaje.OK;
		Empresa empresaEntity = new Empresa();
		boolean tieneHijos = false;
		String cuentaPadre = "";
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (PlanCuenta planCuenta : object) {			
			if (planCuenta.getCodigo().equals(0L)) {
				//CREA NODO BASE
				resultado = creaNodoArbolCero(empresa);
				if (Mensaje.OK.equals(resultado)) {

					//VERIFICA SI SE TRATA DE UNA CUENTA DE PRIMER NIVEL
					Boolean primerNivel = cuentaService.verificaPrimerNivel(planCuenta.getCuentaContable());
					if (primerNivel) {
						//OBTIENE EL CODIGO DE LA CUENTA BASE
						planCuentaPadre = planCuentaDaoService.selectRaizByEmpresa(empresa);
					}else{
						//OBTIENE LA CUENTA PADRE
						cuentaPadre = cuentaService.obtieneCuentaPadre(planCuenta.getCuentaContable());
						planCuentaPadre = planCuentaDaoService.selectByCuentaEmpresa(cuentaPadre, empresa);
					}
					if (planCuentaPadre == null) {
						resultado = "NO EXISTE LA CUENTA PADRE" + cuentaPadre;
						throw new IncomeException("NO EXISTE LA CUENTA PADRE " + cuentaPadre);
					}else{
						if(Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(planCuentaPadre.getTipo())){
							tieneHijos = verificaHijosSinMayorizacin(planCuentaPadre.getCodigo());
							if(tieneHijos){
								throw new IncomeException("NO SE PUEDE CREAR CUENTAS HIJOS LA CUENTA " + planCuentaPadre.getCuentaContable() + " YA QUE TIENE ASIENTOS O PLANTILLAS ASOCIADOS");
							}	
						}
						// RECUPERA NATURALEZA CUENTA Y ASIGNA
						planCuenta.setNaturalezaCuenta(naturalezaCuentaService.recuperaNaturalezaByCuenta(planCuenta.getCuentaContable(), empresa));
						// ASIGNA CODIGO DE PADRE
						planCuenta.setIdPadre(planCuentaPadre.getCodigo());
						// ASIGNA LA EMPRESA
						planCuenta.setEmpresa((Empresa)empresaDaoService.find(empresaEntity, empresa));
						// ASIGNA EL TIPO DE CUENTA COMO DE MOVIMIENTO
						planCuenta.setTipo(Long.valueOf(TipoCuentaContable.MOVIMIENTO));
						// ASIGNA EL NIVEL
						planCuenta.setNivel(planCuentaPadre.getNivel() + 1);
						// ASIGNA ESTADO
						planCuenta.setEstado(Long.valueOf(Estado.ACTIVO));
						// ASIGNA FECHA CREACION
						planCuenta.setFechaUpdate(LocalDate.now());
						// ALMACENA EL REGISTRO
						planCuentaDaoService.save(planCuenta, Long.valueOf("0"));					
						// MODIFICA PADRE COMO ACUMULACION
						planCuentaPadre.setTipo(Long.valueOf(TipoCuentaContable.ACUMULACION));
						// ALMACENA EL REGISTRO
						planCuentaDaoService.save(planCuentaPadre, planCuentaPadre.getCodigo());	
					}					
				}			
			}else{
				//INSERTA O ACTUALIZA REGISTRO
				planCuenta.setFechaUpdate(LocalDate.now());
				planCuentaDaoService.save(planCuenta, planCuenta.getCodigo());	
			}
		}
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#recuperaTipo(java.lang.Long)
	 */
	public Long recuperaTipo(Long id) throws Throwable {
		System.out.println("Servicio recuperaTipo de PlanCuenta con id: " + id);
		//INSTANCIA LA ENTIDAD
		PlanCuenta planCuentas = new PlanCuenta();
		PlanCuenta planCuenta = planCuentaDaoService.find(planCuentas, id);
		return planCuenta.getTipo();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#removeMovimiento(java.lang.Long)
	 */
	public void removeMovimiento(Long id) throws Throwable {
		System.out.println("Servicio removeMovimiento de PlanCuenta con id: " + id);
		//INSTANCIA LA ENTIDAD
		PlanCuenta planCuenta = new PlanCuenta();
		boolean tieneHijos = false;
		int numeroRegistrosNivel = 0;
		Long idPadre = recuperaIdPadre(id);
		tieneHijos = verificaHijosSinHistMayorizacin(id);
		if(tieneHijos){
			actualizaEstadoCuenta(id, Estado.INACTIVO);
		}else{
			planCuentaDaoService.remove(planCuenta, id);	
		}
		numeroRegistrosNivel = planCuentaDaoService.numeroRegActivosByIdPadre(idPadre);
		if (numeroRegistrosNivel == 0) {
			if(idPadre != null){
				actualizaTipoCuenta(idPadre, TipoCuentaContable.MOVIMIENTO);	
			}else{
				System.out.println("TRANSACCION EXITOSA - EL REGISTRO NO CUENTA CON CUENTA PADRE POR LO QUE NO SE REALIZARA NINGUN CAMBIO EN LAS CUENTAS SUPERIORES");
				throw new EJBException("TRANSACCION EXITOSA - EL REGISTRO NO CUENTA CON CUENTA PADRE POR LO QUE NO SE REALIZARA NINGUN CAMBIO EN LAS CUENTAS SUPERIORES");
			}
		}
						
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#removeAcumulacion(java.lang.Long)
	 */
	public void removeAcumulacion(Long id, boolean actualiza) throws Throwable {
		System.out.println("Servicio removeAcumulacion de PlanCuenta con id: " + id + ", actualiza = " + actualiza);
		//INSTANCIA LA ENTIDAD
		PlanCuenta planCuenta = new PlanCuenta();
		boolean tieneRegistros = false;
		List<PlanCuenta> listadoHijos = recuperaCuentasHijo(id);
		for (PlanCuenta registro : listadoHijos){
			tieneRegistros = verificaHijosSinHistMayorizacin(registro.getCodigo());
			if (tieneRegistros) {
				break;
			}
		}
		if ((tieneRegistros) && (actualiza)) {
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (PlanCuenta registro : listadoHijos){
				actualizaEstadoCuenta(registro.getCodigo(), Estado.INACTIVO);
			}
			actualizaEstadoCuenta(id, Estado.INACTIVO);
		}else{
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (PlanCuenta registro : listadoHijos){
				planCuentaDaoService.remove(planCuenta, registro.getCodigo());		
			}
			planCuentaDaoService.remove(planCuenta, id);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#recuperaSiguienteHijo(java.lang.Long)
	 */
	public String recuperaSiguienteHijo(Long id) throws Throwable {
		System.out.println("Servicio recuperaSiguienteHijo de planCuenta service");
		// INICIALIZA VARIABLES
		String cuenta = null;
		String cuentaPadre = null;
		String ultimoNumero = null;
		Long siguienteNumero = null;
		String cuentaFinal = null;
	
		// CREA SENTENCIA Y BUSCA ENTIDAD
		List<PlanCuenta> planCuentaList = planCuentaDaoService.selectByIdPadre(id);
		if (!planCuentaList.isEmpty()) {			
			PlanCuenta planCuenta = buscaMayorCuenta(planCuentaList);
			cuenta = planCuenta.getCuentaContable();
			cuentaPadre = cuentaService.obtieneCuentaPadre(cuenta);
			if ("0".equals(cuentaPadre)) {
				ultimoNumero = cuenta;													
			}else{
				ultimoNumero = cuenta.substring(cuentaPadre.length() + 1, cuenta.length());
			}			
			siguienteNumero = Long.valueOf(ultimoNumero) + 1;	
		}else{
			PlanCuenta planCuentaOriginal = planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA);
			cuentaPadre = planCuentaOriginal.getCuentaContable();
			siguienteNumero = Long.valueOf("01");	
		}		
		if ("0".equals(cuentaPadre)){
			cuentaFinal = String.valueOf(siguienteNumero);
		}else{
			cuentaFinal = cuentaPadre + '.' + siguienteNumero;	
		}
		
		return cuentaFinal;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#recuperaIdPadre(java.lang.Long)
	 */
	public Long recuperaIdPadre(Long id) throws Throwable {
		System.out.println("Servicio recuperaIdPadre de PlanCuenta con id: " + id);
		PlanCuenta planCuenta = planCuentaDaoService.find(new PlanCuenta(), id);
		return planCuenta.getIdPadre();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#actualizaTipoCuenta(java.lang.Long, int)
	 */
	public void actualizaTipoCuenta(Long id, int tipo) throws Throwable {
		System.out.println("Servicio actualizaTipoCuenta de PlanCuenta con id: " + id);
		PlanCuenta planCuenta = planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA);
		planCuenta.setTipo(Long.valueOf(tipo));
		planCuentaDaoService.save(planCuenta, id);
	}	
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#copiaPlanEmpresa(java.lang.Long, java.lang.Long)
	 */
	public String copiaPlanEmpresa(Long empresaOrigen, Long empresaDestino)
			throws Throwable {
		System.out.println("Servicio copiaPlanEmpresa con empresaOrigen: " + empresaOrigen + " y empresaDestino: " + empresaDestino);
		//INSTANCIA LA ENTIDAD 
		Empresa empresa = new Empresa();
		String resultado = Mensaje.OK;
		NaturalezaCuenta naturalezaCuenta = new NaturalezaCuenta();
		String cuentaPadre = null;
		PlanCuenta objetoPadre = new PlanCuenta();
		
		// VALIDA QUE SE PUEDA REALIZA LA COPIA
		boolean permiteCopia = validaCopiaPlan(empresaDestino);

		if (permiteCopia) {
			List<PlanCuenta> listadoPlan = planCuentaDaoService.selectByEmpresa(empresaDestino);
			if (!listadoPlan.isEmpty()) {
				// ELIMINA LOS REGISTROS
				planCuentaDaoService.deleteByEmpresa(empresaDestino);
			}
			List<NaturalezaCuenta> naturalezaOrigen = naturalezaCuentaDaoService.selectByEmpresa(empresaDestino);
			if (!naturalezaOrigen.isEmpty()) {
				// ELIMINA LOS REGISTROS
				naturalezaCuentaDaoService.deleteByEmpresa(empresaDestino);
			}
			List<NaturalezaCuenta> listadoNaturaleza = naturalezaCuentaDaoService.selectActivosByEmpresa(empresaOrigen);
			for (NaturalezaCuenta registro : listadoNaturaleza) {
				NaturalezaCuenta nuevo = registro;
				nuevo.setEmpresa((Empresa)empresaDaoService.find(empresa, empresaDestino));
				nuevo.setCodigo(0L);
				naturalezaCuentaDaoService.save(nuevo, 0L);
			}
			listadoPlan = planCuentaDaoService.selectActivasByEmpresa(empresaOrigen);
			for (PlanCuenta registro : listadoPlan) {
				naturalezaCuenta = naturalezaCuentaService.recuperaNaturalezaByCuenta(registro.getCuentaContable(), empresaDestino);
				registro.setNaturalezaCuenta(naturalezaCuenta);
				registro.setEmpresa((Empresa)empresaDaoService.find(empresa, empresaDestino));
				registro.setCodigo(0L);
				if("0".equals(registro.getCuentaContable())){
					registro.setIdPadre(0L);
				}else{
					cuentaPadre = cuentaService.obtieneCuentaPadre(registro.getCuentaContable());
					objetoPadre = planCuentaDaoService.selectByCuentaEmpresa(cuentaPadre, empresaDestino);
					registro.setIdPadre(objetoPadre.getCodigo());					
				}
				planCuentaDaoService.save(registro, registro.getCodigo());					
			}
		}else{
			System.out.println("NO SE PUEDE REALIZAR LA COPIA PORQUE YA EXISTEN REGISTROS ASIGNADOS");
			resultado = "NO SE PUEDE REALIZAR LA COPIA PORQUE YA EXISTEN REGISTROS ASIGNADOS";
		}
		
		return resultado;
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#validaCopiaPlan(java.lang.Long)
	 */
	public boolean validaCopiaPlan(Long empresaDestino) throws Throwable {
		System.out.println("Servicio validaCopiaPlan con empresaDestino: " + empresaDestino);
		boolean permiteCopia = true;
		boolean existenHijos = false;
		List<PlanCuenta> listadoPlanCuenta = planCuentaDaoService.selectByEmpresaSinRaiz(empresaDestino);
		// VALIDA SI EXISTEN REGISTROS DE PLAN PARA LA EMPRESA
		if (listadoPlanCuenta.isEmpty()){
			permiteCopia = true;
		}else{
			// VALIDA SI EXISTEN REGISTROS RELACIONADOS AL PLAN
			for (PlanCuenta registro : listadoPlanCuenta) {
				existenHijos = verificaHijosSinHistMayorizacin(registro.getCodigo());
				if (existenHijos) {
					permiteCopia = false;
					break;
				}				
			}
		}
		return permiteCopia;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#recuperaCuentasHijo(java.lang.Long)
	 */
	public List<PlanCuenta> recuperaCuentasHijo(Long id) throws Throwable {
		System.out.println("Servicio recuperaCuentasHijo con id: " + id);		
		PlanCuenta padre = planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA);
		return planCuentaDaoService.selectHijosByNumeroCuenta(padre.getCuentaContable(), padre.getEmpresa().getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#actualizaEstadoCuenta(java.lang.Long, int)
	 */
	public void actualizaEstadoCuenta(Long id, int tipo) throws Throwable {
		System.out.println("Servicio actualizaEstadoCuenta de PlanCuenta con id: " + id);
		PlanCuenta planCuenta = planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA);
		planCuenta.setFechaInactivo(LocalDate.now());
		planCuenta.setEstado(Long.valueOf(tipo));
		planCuentaDaoService.save(planCuenta, id);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#buscaMayorCuenta(java.util.List)
	 */
	public PlanCuenta buscaMayorCuenta(List<PlanCuenta> listadoCuenta) throws Throwable {
		System.out.println("Servicio buscaMayorCuenta de PlanCuenta ");
		String cuentaPadre = null;
		int ultimoNumero = 0;
		int mayor = 0;	
		PlanCuenta planMayor = new PlanCuenta();		
		for (PlanCuenta registro : listadoCuenta){
			cuentaPadre = cuentaService.obtieneCuentaPadre(registro.getCuentaContable());
			if ("0".equals(cuentaPadre)) {
				ultimoNumero = Integer.valueOf(registro.getCuentaContable().trim());													
			}else{
				ultimoNumero = Integer.valueOf(registro.getCuentaContable().trim().substring(cuentaPadre.length() + 1, registro.getCuentaContable().trim().length()));
			}
			if (ultimoNumero >= mayor){
				mayor = ultimoNumero;
				planMayor =  registro;
			}
		}
		return planMayor;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#remove(java.lang.Long, boolean)
	 */
	public void remove(Long id, boolean actualiza) throws Throwable {
		System.out.println("Servicio remove boolean de PlanCuenta service ");
		Long tipo = recuperaTipo(id);
		// CUENTA MOVIMIENTO
		if (Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(tipo)){
			removeMovimiento(id);				
		}
		// CUENTA ACUMULACION
		if (Long.valueOf(TipoCuentaContable.ACUMULACION).equals(tipo)){
			removeAcumulacion(id, actualiza);
		}	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#validaExisteCuentasNaturaleza(java.lang.Long)
	 */
	public String validaExisteCentroCosto(Long empresa) throws Throwable {
		System.out.println("Servicio validaExisteCuentasNaturaleza de PlanCuenta con empresa: " + empresa);
		String resultado = Mensaje.OK;
		List<DetalleRubro> listado = detalleRubroService.selectByCodigoAlternoRubro(Rubros.GRUPOS_CUENTAS_BASICAS);
		for (DetalleRubro detalleRubro : listado) {
			if (detalleRubro.getCodigoAlterno() != 0) {
				List<PlanCuenta> planCuentas = planCuentaDaoService.selectByEmpresaNaturaleza(empresa, Long.valueOf(detalleRubro.getCodigoAlterno()));
				if(planCuentas.isEmpty()){
					resultado = "NO EXISTEN CUENTAS PARA LA NATURALEZA " + detalleRubro.getCodigoAlterno();
					break;
				}
			}
		}
		return resultado;
	}

	public PlanCuenta selectById(Long id) throws Throwable {
		System.out.println("Servicio selectById con id: " + id);		
		return planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#selectByEmpresaManejaCC(java.lang.Long)
	 */
	public List<PlanCuenta> selectByEmpresaManejaCC(Long empresa) throws Throwable {
		System.out.println("Servicio selectByEmpresaManejaCC con empresa: " + empresa);
		return planCuentaDaoService.selectByEmpresaManejaCC(empresa);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#recuperaMaximaCuenta(java.lang.Long, java.lang.String)
	 */
	public String recuperaMaximaCuenta(Long empresa, String cuentaInicio, String siguienteNaturaleza) throws Throwable {
		System.out.println("Servicio recuperaMaximaCuenta con empresa : " + empresa);				
		return planCuentaDaoService.selectMaximaByCuenta(empresa, cuentaInicio, siguienteNaturaleza);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#selectByCuentaEmpresa(java.lang.String, java.lang.Long)
	 */
	public PlanCuenta selectByCuentaEmpresa(String cuenta, Long empresa) throws Throwable {
		System.out.println("Servicio selectByCuentaEmpresa con empresa : " + empresa + ", Cuenta " + cuenta);
		return planCuentaDaoService.selectByCuentaEmpresa(cuenta, empresa);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#selectMovimientoByEmpresaCuentaFecha(java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String)
	 */
	public List<PlanCuenta> selectMovimientoByEmpresaCuentaFecha(Long empresa,
			Date fechaInicio, Date fechaFin, String cuentaInicio,
			String cuentaFin) throws Throwable {
		System.out.println("Servicio selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio " + fechaInicio);
		return planCuentaDaoService.selectMovimientoByEmpresaCuentaFecha(empresa, fechaInicio, fechaFin, cuentaInicio, cuentaFin);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#selectByEmpresaCuentaFechaCentro(java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public List<PlanCuenta> selectByEmpresaCuentaFechaCentro(Long empresa,
			Date fechaInicio, Date fechaFin, String cuentaInicio,
			String cuentaFin, String centroInicio, String centroFin)
			throws Throwable {
		System.out.println("Servicio selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin + ", centroInicio: " +
				  centroInicio + ", centroFin: " + centroFin);
		return planCuentaDaoService.selectByEmpresaCuentaFechaCentro
		       			(empresa, fechaInicio, fechaFin, 
		    		   	 cuentaInicio, cuentaFin, centroInicio, centroFin);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#saldoCuentaFechaEmpresa(java.lang.Long, java.lang.Long, java.util.Date)
	 */
	public Double saldoCuentaFechaEmpresa(Long idEmpresa, Long idCuenta,
			Date fechaInicio) throws Throwable {
		System.out.println("Servicio saldoCuentaFechaEmpresa con idEmpresa: " + idEmpresa + ", idCuenta: " + idCuenta +
				 ", fecha: " + fechaInicio);
		
		Double saldoAnteriorCuenta = 0D;
		Date diaInicio = new Date();
		DetalleMayorizacion detalleMayorizacionAnterior = null;
		Periodo periodoAnteriorMayorizado = new Periodo();
		Periodo periodoInicial = new Periodo();
		
		periodoAnteriorMayorizado = periodoService.obtieneMaximoPeriodoFechaEstado(idEmpresa, EstadoPeriodos.MAYORIZADO, fechaInicio);
		
		if(periodoAnteriorMayorizado == null){
			periodoInicial = periodoService.obtieneMinimoPeriodoFechaEstado(idEmpresa, EstadoPeriodos.RAIZ, fechaInicio);								
			if(periodoInicial == null){
				diaInicio = fechaInicio;
			}else{
				diaInicio = periodoInicial.getPrimerDia();
			}
		}else{
			detalleMayorizacionAnterior = detalleMayorizacionService.selectByCuentaMayorizacion(idCuenta, periodoAnteriorMayorizado.getIdMayorizacion());
			if(detalleMayorizacionAnterior == null){
				saldoAnteriorCuenta = 0D;
			}else{
				saldoAnteriorCuenta = detalleMayorizacionAnterior.getSaldoActual();	
			}			
			diaInicio = fechaService.sumaRestaDias(periodoAnteriorMayorizado.getUltimoDia(), 1);
		}	
		saldoAnteriorCuenta += detalleAsientoService.recuperaSaldoCuentaEmpresaFechas(idEmpresa,
					idCuenta, diaInicio, fechaInicio);
								
		return saldoAnteriorCuenta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#verificaHijosSinMayorizacin(java.lang.Long)
	 */
	public boolean verificaHijosSinMayorizacin(Long id) throws Throwable {
		System.out.println("Servicio verificaHijosSinMayorizacin con id: " + id);
		boolean flag = false;
		List<PlanCuenta> todos = new ArrayList<PlanCuenta>();
		Long tipo = recuperaTipo(id);
		if (Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(tipo)){
			todos.add(planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA));
		}else{
			todos = recuperaCuentasHijo(id);	
		}		
		for(PlanCuenta i : todos){			
			List<DetalleAsiento> detallesAsientos = detalleAsientoDaoService.selectByIdPlanCuenta(i.getCodigo());
			if (!detallesAsientos.isEmpty()) {
				flag = true;
				break;
			}			
			List<DetalleMayorizacion> detallesMayorizacion = detalleMayorizacionDaoService.selectByIdPlanCuenta(i.getCodigo());
			if (!detallesMayorizacion.isEmpty()) {
				flag = true;
				break;
			}
			List<DetallePlantilla> detallesPlantilla = detallePlantillaDaoService.selectByIdPlanCuenta(i.getCodigo());
			if (!detallesPlantilla.isEmpty()) {
				flag = true;
				break;
			}	
		}
		return flag;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#verificaHijosSinHistMayorizacin(java.lang.Long)
	 */
	public boolean verificaHijosSinHistMayorizacin(Long id) throws Throwable {
		System.out.println("Servicio verificaHijosSinHistMayorizacin con id: " + id);
		boolean flag = false;
		List<PlanCuenta> todos = new ArrayList<PlanCuenta>();
		Long tipo = recuperaTipo(id);
		if (Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(tipo)){
			todos.add(planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA));
		}else{
			todos = recuperaCuentasHijo(id);	
		}		
		for(PlanCuenta i : todos){	
			List<DetalleAsiento> detallesAsientos = detalleAsientoDaoService.selectByIdPlanCuenta(i.getCodigo());
			if (!detallesAsientos.isEmpty()) {
				flag = true;
				break;
			}			
			List<DetalleMayorizacion> detallesMayorizacion = detalleMayorizacionDaoService.selectByIdPlanCuenta(i.getCodigo());
			if (!detallesMayorizacion.isEmpty()) {
				flag = true;
				break;
			}
			List<DetallePlantilla> detallesPlantilla = detallePlantillaDaoService.selectByIdPlanCuenta(i.getCodigo());
			if (!detallesPlantilla.isEmpty()) {
				flag = true;
				break;
			}			
		}
		return flag;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#selectByRangoEmpresaEstado(java.lang.Long, java.lang.String, java.lang.String)
	 */
	public List<PlanCuenta> selectByRangoEmpresaEstado(Long empresa,
			String cuentaInicio, String cuentaHasta) throws Throwable {
		System.out.println("Servicio selectByRangoEmpresaEstado con empresa : " + empresa + ", cuentaInicio" + cuentaInicio + 
				 ", cuentaHasta: " + cuentaHasta);
		return planCuentaDaoService.selectByRangoEmpresaEstado(empresa, cuentaInicio, cuentaHasta);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#selectMovimientoByRangoEmpresaEstado(java.lang.Long, java.lang.String, java.lang.String)
	 */
	public List<PlanCuenta> selectMovimientoByRangoEmpresaEstado(Long empresa,
			String cuentaInicio, String cuentaHasta) throws Throwable {
		System.out.println("Servicio selectMovimientoByRangoEmpresaEstado con empresa : " 
				 + empresa + ", cuentaInicio" + cuentaInicio + 
				 ", cuentaHasta: " + cuentaHasta);
		return planCuentaDaoService.selectMovimientoByRangoEmpresaEstado(empresa, cuentaInicio, cuentaHasta);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#recuperaCuentasByRango(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public List<PlanCuenta> recuperaCuentasByRango(Long empresa, String cuentaInicio, String cuentaFin) throws Throwable {
		System.out.println("Servicio recuperaCuentasByRango con empresa:" + empresa + ", cuentaInicio" + cuentaInicio + ", cuentaFin" + cuentaFin);
		List<PlanCuenta> rangos = planCuentaDaoService.selectCuentasByRango(empresa, cuentaInicio, cuentaFin);
		return rangos;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlanCuentaService#servicioRango(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public boolean servicioRango(Long empresa, String cuentaInicio, String cuentaFin, String cuentaComparar) throws Throwable {
		System.out.println("Servicio servicioRango con empresa: " + empresa + ", cuentaInicio" + cuentaInicio + ", cuentaFin" + cuentaFin + ", cuentaComparar" + cuentaComparar);
		boolean flag = true;
		String cuenta = null;
		if(cuentaFin == null){
			cuenta = planCuentaDaoService.selectMaxCuentaByNumeroCuenta(empresa, cuentaInicio+'%');
		}else {
			cuenta = cuentaFin;
		}
		List<PlanCuenta> cuentas = planCuentaDaoService.selectCuentasByRango(empresa, cuentaInicio, cuenta);
		for(PlanCuenta planCuenta :cuentas){
			if(planCuenta.getCuentaContable().equals(cuentaComparar)){			
			   flag = false;								
			   throw new IncomeException("NO PUEDE INGRESAR UNA CUENTA QUE EXISTA EN UN RANGO");			
				}
		}
		return flag;
	}

	@Override
	public PlanCuenta saveSingle(PlanCuenta planCuenta) throws Throwable {
		System.out.println("saveSingle - planCuenta");
        if(planCuenta.getCodigo() == null){
        	planCuenta.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        planCuenta = planCuentaDaoService.save(planCuenta, planCuenta.getCodigo());
        return planCuenta;
	}

	@Override
	public String validaDetallePlantilla(Long idPlanCuenta) throws Throwable {
		System.out.println("verificaAsientoEnPeriodo: " + idPlanCuenta);
	    List<DetallePlantilla> detallePlantilla = detallePlantillaDaoService.selectByIdPlanCuenta(idPlanCuenta);
	    String resultado = "OK";
	    if (!detallePlantilla.isEmpty()) {
	    	resultado = "No se puede eliminar el Plan de cuenta porque tiene plantilla asociada.";
	    }
	    return resultado;
	}

	@Override
	public String validaAsientoEnCuenta(Long idPlanCuenta) throws Throwable {
		System.out.println("verificaAsientoEnPeriodo: " + idPlanCuenta);
	    List<DetalleAsiento> detalleAsientos = detalleAsientoDaoService.selectByIdPlanCuenta(idPlanCuenta);
	    String resultado = "OK";
	    if (!detalleAsientos.isEmpty()) {
	    	resultado = "No se puede eliminar el Plan de cuenta porque tiene asientos asociados.";
	    }
	    return resultado;
	}

}