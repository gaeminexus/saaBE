package com.saa.ejb.contabilidad.serviceImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.ejb.FechaService;
import com.saa.basico.ejb.Mensaje;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.CentroCostoDaoService;
import com.saa.ejb.contabilidad.dao.DetalleAsientoDaoService;
import com.saa.ejb.contabilidad.dao.DetalleMayorizacionCCDaoService;
import com.saa.ejb.contabilidad.service.CentroCostoService;
import com.saa.ejb.contabilidad.service.CuentaService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.ejb.contabilidad.service.PeriodoService;
import com.saa.model.contabilidad.CentroCosto;
import com.saa.model.contabilidad.DetalleAsiento;
import com.saa.model.contabilidad.DetalleMayorizacionCC;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.Periodo;
import com.saa.model.scp.Empresa;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

@Stateless
public class CentroCostoServiceImpl implements CentroCostoService {
	
	@EJB
	private CentroCostoDaoService centroCostoDaoService;
	
	@EJB
	private EmpresaDaoService empresaDaoService;
	
	@EJB
	private CuentaService cuentaService;
	
	@EJB
	private DetalleAsientoService detalleAsientoService;	
	
	@EJB
	private PeriodoService periodoService;	
	
	@EJB
	private DetalleMayorizacionCCDaoService detalleMayorizacionCCDaoService;	
	
	@EJB
	private FechaService fechaService;
	
	@EJB
	private DetalleAsientoDaoService detalleAsientoDaoService;

	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de CentroCosto service ");
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
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.List<CentroCosto>)
	 */
	public void save(List<CentroCosto> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de centroCosto service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (CentroCosto centroCosto : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			centroCostoDaoService.save(centroCosto, centroCosto.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<CentroCosto> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll CentroCostoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CentroCosto> result = centroCostoDaoService.selectAll(NombreEntidadesContabilidad.CENTRO_COSTO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			throw new IncomeException("Busqueda total CentroCosto no devolvio ningun registro");
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<CentroCosto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CentroCosto");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CentroCosto> result = centroCostoDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.CENTRO_COSTO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de centroCosto no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#numeroRegistrosEmpresa(java.lang.Long)
	 */
	public int numeroRegistrosEmpresa(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo numeroRegistrosEmpresa CentroCosto con empresa: " + empresa);
		//CREA SENTENCIA WHERE PARA VERIFICAR SI EXISTEN REGISTROS
		List<CentroCosto> result = centroCostoDaoService.selectByEmpresa(empresa);		
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		return result.size();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#creaNodoArbolCero(java.lang.Long)
	 */
	public String creaNodoArbolCero(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo creaNodoArbolCero CentroCosto con empresa: " + empresa);
		//INICIALIZA VARIABLE DE RESULTADO
		String resultado = Mensaje.OK;
		int numeroRegistros = numeroRegistrosEmpresa(empresa);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		if (numeroRegistros == 0) {
			//CREA EL OBJETO PARA PERSISTIRLO
			CentroCosto centroCosto = new CentroCosto();
			centroCosto.setNombre("CENTRO COSTO");
			centroCosto.setNumero("0");
			centroCosto.setTipo(Long.valueOf(TipoCuentaContable.ACUMULACION));
			centroCosto.setNivel(0L);
			centroCosto.setIdPadre(0L);
			centroCosto.setEstado(Long.valueOf(Estado.ACTIVO));
			centroCosto.setEmpresa(empresaDaoService.find(new Empresa(), empresa));
			//PERSISTE EL OBJETO
			centroCostoDaoService.save(centroCosto, centroCosto.getCodigo());
			//RETORNA VARIABLE CON OK
			resultado = Mensaje.OK;			
		}else{
			resultado = Mensaje.OK;
		}
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#saveCuenta(java.lang.List<CentroCosto>, java.lang.Object[])
	 */
	public String saveCuenta(List<CentroCosto> objects, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo saveCuenta de centroCosto service");
		//INSTANCIA UNA ENTIDAD
		Empresa empresas = new Empresa();
		//INSTANCIA NUEVA ENTIDAD PARA PADRE
		CentroCosto centroCostoPadre = new CentroCosto();
		//INICIALIZA VARIABLE DE RESULTADO
		String resultado = Mensaje.OK;
		boolean tieneHijos = false;
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (CentroCosto centroCosto : objects) {
			//INSTANCIA NUEVA ENTIDAD
			if (centroCosto.getCodigo() == 0L) {
				//CREA NODO BASE
				resultado = creaNodoArbolCero(empresa);
				if (Mensaje.OK.equals(resultado)){
					//VERIFICA SI SE TRATA DE UNA CUENTA DE PRIMER NIVEL
					Boolean primerNivel = cuentaService.verificaPrimerNivel(centroCosto.getNumero());
					if (primerNivel) {
						//OBTIENE EL CODIGO DE LA CUENTA BASE
						centroCostoPadre = centroCostoDaoService.selectRaizByEmpresa(empresa);
					}else{
						//OBTIENE LA CUENTA PADRE
						centroCostoPadre = centroCostoDaoService.selectByCuentaEmpresa(cuentaService.obtieneCuentaPadre(centroCosto.getNumero()),empresa);						
					}
					if (centroCostoPadre == null) {
						resultado = "NO EXISTE CUENTA PADRE PARA EL CENTRO DE COSTO " + centroCosto.getNumero();
						throw new EJBException("NO EXISTE CUENTA PADRE PARA EL CENTRO DE COSTO " + centroCosto.getNumero());
					}else{
						if(Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(centroCostoPadre.getTipo())){
							tieneHijos = verificaHijosSinMayorizacin(centroCostoPadre.getCodigo());
							if(tieneHijos){
								throw new EJBException("NO SE PUEDE CREAR CUENTAS HIJOS LA CUENTA " + centroCostoPadre.getNumero() + " YA QUE TIENE REGISTROS ASOCIADOS");
							}	
						}
						// ASIGNA CODIGO DE PADRE
						centroCosto.setIdPadre(centroCostoPadre.getCodigo());
						// ASIGNA EMPRESA
						centroCosto.setEmpresa((Empresa)empresaDaoService.find(empresas, empresa));
						// ASIGNA EL TIPO DE CUENTA COMO DE MOVIMIENTO
						centroCosto.setTipo(Long.valueOf(TipoCuentaContable.MOVIMIENTO));
						// ASIGNA EL NIVEL
						centroCosto.setNivel(centroCostoPadre.getNivel() + 1);
						// ASIGNA ESTADO
						centroCosto.setEstado(Long.valueOf(Estado.ACTIVO));
						// ALMACENA EL REGISTRO
						centroCostoDaoService.save(centroCosto, centroCosto.getCodigo());					
						// MODIFICA PADRE COMO ACUMULACION
						centroCostoPadre.setTipo(Long.valueOf(TipoCuentaContable.ACUMULACION));
						// ALMACENA EL REGISTRO
						centroCostoDaoService.save(centroCostoPadre, centroCostoPadre.getCodigo());	
					}													
				}
			}else{
				//INSERTA O ACTUALIZA REGISTRO
				centroCostoDaoService.save(centroCosto, centroCosto.getCodigo());	
			}
		}
		
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#recuperaTipo(java.lang.Long)
	 */
	public Long recuperaTipo(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaTipo de CentroCosto con id: " + id);
		CentroCosto centroCosto = centroCostoDaoService.find(new CentroCosto(), id);
		return centroCosto.getTipo();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#removeMovimiento(java.lang.Long)
	 */
	public void removeMovimiento(Long id) throws Throwable {
		System.out.println("Ingresa al metodo removeMovimiento de CentroCosto con id: " + id);		
		boolean tieneHijos = false;
		int numeroRegistrosNivel = 0;
		Long idPadre = recuperaIdPadre(id);
		tieneHijos = verificaHijosSinMayorizacin(id);
		if(tieneHijos){
			actualizaEstadoCentro(id, Estado.INACTIVO);
		}else{
			centroCostoDaoService.remove(new CentroCosto(), id);	
		}	
		numeroRegistrosNivel = centroCostoDaoService.numeroRegActivosByIdPadre(idPadre);
		if (numeroRegistrosNivel == 1) {
			if(idPadre != null){
				actualizaTipoCuenta(idPadre, TipoCuentaContable.MOVIMIENTO);	
			}else{
				System.out.println("TRANSACCION EXITOSA - EL REGISTRO NO CUENTA CON CUENTA PADRE POR LO QUE NO SE REALIZARA NINGUN CAMBIO EN LAS CUENTAS SUPERIORES");
				throw new IncomeException("TRANSACCION EXITOSA - EL REGISTRO NO CUENTA CON CUENTA PADRE POR LO QUE NO SE REALIZARA NINGUN CAMBIO EN LAS CUENTAS SUPERIORES");
			}
		}					
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#removeAcumulacion(java.lang.Long)
	 */
	public void removeAcumulacion(Long id, boolean actualiza) throws Throwable {
		System.out.println("Ingresa al metodo removeAcumulacion de CentroCosto con id: " + id);
		//INSTANCIA LA ENTIDAD
		CentroCosto centroCosto = new CentroCosto();
		boolean tieneRegistros = false;
		List<CentroCosto> listadoHijos = recuperaCuentasHijo(id);
		for (CentroCosto registro : listadoHijos){
			tieneRegistros = verificaHijosSinMayorizacin(registro.getCodigo());
			if (tieneRegistros) {
				break;
			}
		}
		if ((tieneRegistros) && (actualiza)) {
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (CentroCosto registro : listadoHijos){
				actualizaEstadoCentro(registro.getCodigo(), Estado.INACTIVO);
			}
			actualizaEstadoCentro(id, Estado.INACTIVO);
		}else{
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (CentroCosto registro : listadoHijos){
				centroCostoDaoService.remove(centroCosto, registro.getCodigo());		
			}
			centroCostoDaoService.remove(centroCosto, id);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#recuperaSiguienteHijo(java.lang.Long)
	 */
	public String recuperaSiguienteHijo(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaSiguienteHijo de centroCosto service");
		String cuenta = null;
		String cuentaPadre = null;
		String ultimoNumero = null;
		Long siguienteNumero = null;
		String cuentaFinal = null;
		
		// CREA SENTENCIA Y BUSCA ENTIDAD		
		List<CentroCosto> centroCostoList = centroCostoDaoService.selectByIdPadre(id);			
		if (!centroCostoList.isEmpty()) {
			CentroCosto centroCosto = buscaMayorCuenta(centroCostoList);
			// RECUPERA CUENTA
			cuenta = centroCosto.getNumero();
			cuentaPadre = cuentaService.obtieneCuentaPadre(cuenta);
			if ("0".equals(cuentaPadre)) {
				ultimoNumero = cuenta;													
			}else{
				ultimoNumero = cuenta.substring(cuentaPadre.length() + 1, cuenta.length());
			}	
			siguienteNumero = Long.valueOf(ultimoNumero) + 1;
		}else{				
			CentroCosto centroCostoOriginal = centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO);
			cuentaPadre = centroCostoOriginal.getNumero();
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
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#recuperaIdPadre(java.lang.Long)
	 */
	public Long recuperaIdPadre(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaTipo de CentroCosto con id: " + id);
		CentroCosto centroCosto = centroCostoDaoService.find(new CentroCosto(), id);
		return centroCosto.getIdPadre();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#actualizaTipoCuenta(java.lang.Long, int)
	 */
	public void actualizaTipoCuenta(Long id, int tipo) throws Throwable {
		System.out.println("Ingresa al metodo actualizaTipoCuenta de CentroCosto con id: " + id);
		CentroCosto centroCosto = centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO);
		centroCosto.setTipo(Long.valueOf(tipo));
		centroCostoDaoService.save(centroCosto, id);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);
		boolean flag = false;
		Long tipo = recuperaTipo(id);
		List<CentroCosto> todos = new ArrayList<CentroCosto>();
		if (Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(tipo)){
			todos.add(centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO));
		}else{
			todos = recuperaCuentasHijo(id);			
		}
		return flag;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#copiaCentroEmpresa(java.lang.Long, java.lang.Long)
	 */
	public String copiaCentroEmpresa(Long empresaOrigen, Long empresaDestino) throws Throwable {
		System.out.println("Ingresa al metodo copiaCentroEmpresa con empresaOrigen: " + empresaOrigen + " y empresaDestino: " + empresaDestino);
		String resultado = Mensaje.OK;
		String cuentaPadre = null;
		CentroCosto objetoPadre = new CentroCosto();
		Empresa empresa = new Empresa();
		// VALIDA QUE SE PUEDA REALIZA LA COPIA
		boolean permiteCopia = validaCopiaCentros(empresaDestino);
		if (permiteCopia) {
			List<CentroCosto> listadoPlan = centroCostoDaoService.selectByEmpresa(empresaDestino);
			if (!listadoPlan.isEmpty()) {
				// ELIMINA LOS REGISTROS
				centroCostoDaoService.deleteByEmpresa(empresaDestino);
			}
			listadoPlan = centroCostoDaoService.selectActivosByEmpresa(empresaOrigen);
			for (CentroCosto registro : listadoPlan) {
				registro.setEmpresa((Empresa)empresaDaoService.find(empresa, empresaDestino));
				registro.setCodigo(0L);
				if("0".equals(registro.getNumero())){
					registro.setIdPadre(0L);
				}else{
					cuentaPadre = cuentaService.obtieneCuentaPadre(registro.getNumero());
					objetoPadre = centroCostoDaoService.selectByCuentaEmpresa(cuentaPadre, empresaDestino);
					registro.setIdPadre(objetoPadre.getCodigo());					
				}
				registro.setNumero(registro.getNumero().trim());
				centroCostoDaoService.save(registro, registro.getCodigo());
			}
		}else{
			System.out.println("NO SE PUEDE REALIZAR LA COPIA PORQUE YA EXISTEN REGISTROS ASIGNADOS");
			resultado = "NO SE PUEDE REALIZAR LA COPIA PORQUE YA EXISTEN REGISTROS ASIGNADOS";
		}
		
		return resultado;
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#validaCopiaCentros(java.lang.Long)
	 */
	public boolean validaCopiaCentros(Long empresaDestino) throws Throwable {
		System.out.println("Ingresa al metodo validaCopiaCentro con empresaDestino: " + empresaDestino);
		boolean permiteCopia = true;
		boolean existenHijos = false;
		List<CentroCosto> listadoCentroCosto = centroCostoDaoService.selectByEmpresaSinRaiz(empresaDestino);
		// VALIDA SI EXISTEN REGISTROS DE PLAN PARA LA EMPRESA
		if (listadoCentroCosto.isEmpty()){
			permiteCopia = true;
		}else{
			// VALIDA SI EXISTEN REGISTROS RELACIONADOS AL PLAN
			for (CentroCosto registro : listadoCentroCosto) {
				existenHijos = verificaHijosHistMayorizacin(registro.getCodigo());
				if (existenHijos) {
					permiteCopia = false;
					break;
				}				
			}
		}

		return permiteCopia;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#recuperaCuentasHijo(java.lang.Long)
	 */
	public List<CentroCosto> recuperaCuentasHijo(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaCuentasHijo con id: " + id);
		CentroCosto padre = centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO);
		return centroCostoDaoService.selectHijosByNumeroCuenta(padre.getNumero(), padre.getEmpresa().getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#actualizaEstadoCentro(java.lang.Long, int)
	 */
	public void actualizaEstadoCentro(Long id, int tipo) throws Throwable {
		System.out.println("Ingresa al metodo actualizaTipoCuenta de CentroCosto con id: " + id);
		CentroCosto centroCosto = centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO);
		centroCosto.setEstado(Long.valueOf(tipo));
		centroCostoDaoService.save(centroCosto, id);		
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#buscaMayorCuenta(java.util.List)
	 */
	public CentroCosto buscaMayorCuenta(List<CentroCosto> listadoCuenta) throws Throwable {
		System.out.println("Ingresa al metodo buscaMayorCuenta de CentroCosto ");
		String cuentaPadre = null;
		int ultimoNumero = 0;
		int mayor = 0;	
		CentroCosto planMayor = new CentroCosto();		
		for (CentroCosto registro : listadoCuenta){
			cuentaPadre = cuentaService.obtieneCuentaPadre(registro.getNumero());
			if ("0".equals(cuentaPadre)) {
				ultimoNumero = Integer.valueOf(registro.getNumero().trim());													
			}else{
				ultimoNumero = Integer.valueOf(registro.getNumero().trim().substring(cuentaPadre.length() + 1, registro.getNumero().trim().length()));
			}
			if (ultimoNumero >= mayor){
				mayor = ultimoNumero;
				planMayor =  registro;
			}
		}
		return planMayor;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#remove(java.lang.Long, boolean)
	 */
	public void remove(Long id, boolean actualiza) throws Throwable {
		System.out.println("Ingresa al metodo remove[boolean] de CentroCosto service ");
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
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#selectById(java.lang.Long)
	 */
	public CentroCosto selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#selectMovimientosByEmpresa(java.lang.Long)
	 */
	public List<CentroCosto> selectMovimientosByEmpresa(Long empresa) throws Throwable {
		System.out.println("Ingresa al método selectMovimientosByEmpresa con empresa: " + empresa);
		List<CentroCosto> resultado = centroCostoDaoService.selectMovimientosByEmpresa(empresa);
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#selectByEmpresaCuentaFechaCentro(java.lang.Long, java.util.Date, java.util.Date, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public List<CentroCosto> selectByEmpresaCuentaFechaCentro(Long empresa,
			Date fechaInicio, Date fechaFin, String cuentaInicio,
			String cuentaFin, String centroInicio, String centroFin)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin + ", centroInicio: " +
				  centroInicio + ", centroFin: " + centroFin);
		return centroCostoDaoService.selectByEmpresaCuentaFechaCentro(empresa, 
				fechaInicio, fechaFin, cuentaInicio, cuentaFin, centroInicio, centroFin);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#saldoCuentaAnteriorFechaEmpresa(java.lang.Long, java.lang.Long, java.util.Date)
	 */
	public Double saldoCentroFechaEmpresa(Long idEmpresa,
			Long idCentro, Date fechaInicio) throws Throwable {
		System.out.println("Ingresa al Metodo saldoCentroFechaEmpresa con idEmpresa: " + idEmpresa + ", idCentro: " + idCentro +
				 ", fecha: " + fechaInicio);
		
		Double saldoAnteriorCuenta = 0D;
		Date diaInicio = new Date();
		DetalleMayorizacionCC detalleMayorizacionCCAnterior = null;
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
			detalleMayorizacionCCAnterior = detalleMayorizacionCCDaoService.selectByMayorizacionAndCC(periodoAnteriorMayorizado.getIdMayorizacion(), idCentro);
			if(detalleMayorizacionCCAnterior == null){
				saldoAnteriorCuenta = 0D;
			}else{
				saldoAnteriorCuenta = detalleMayorizacionCCAnterior.getSaldoActual();	
			}			
			diaInicio = fechaService.sumaRestaDias(periodoAnteriorMayorizado.getUltimoDia(), 1);
		}	
		saldoAnteriorCuenta += detalleAsientoService.recuperaSaldoCentroEmpresaFechas(idEmpresa, idCentro, diaInicio, fechaInicio);
								
		return saldoAnteriorCuenta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#verificaHijosSinMayorizacin(java.lang.Long)
	 */
	public boolean verificaHijosSinMayorizacin(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijosSinMayorizacin con id: " + id);
		boolean flag = false;
		Long tipo = recuperaTipo(id);
		List<CentroCosto> todos = new ArrayList<CentroCosto>();
		if (Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(tipo)){
			todos.add(centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO));
		}else{
			todos = recuperaCuentasHijo(id);			
		}
		return flag;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#verificaHijosHistMayorizacin(java.lang.Long)
	 */
	public boolean verificaHijosHistMayorizacin(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijosHistMayorizacin con id: " + id);
		boolean flag = false;
		Long tipo = recuperaTipo(id);
		List<CentroCosto> todos = new ArrayList<CentroCosto>();
		if (Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(tipo)){
			todos.add(centroCostoDaoService.selectById(id, NombreEntidadesContabilidad.CENTRO_COSTO));
		}else{
			todos = recuperaCuentasHijo(id);			
		}
		return flag;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.CentroCostoService#selectByEmpresaSinRaiz(java.lang.Long)
	 */
	public List<CentroCosto> selectByEmpresaSinRaiz(Long idEmpresa)
			throws Throwable {
		System.out.println("Ingresa al método selectByEmpresaSinRaiz con empresa: " + idEmpresa);
		return  centroCostoDaoService.selectByEmpresaSinRaiz(idEmpresa);
	}

	@Override
	public CentroCosto saveSingle(CentroCosto centroCosto) throws Throwable {
		System.out.println("saveSingle - CentroCostoService");
		centroCosto = centroCostoDaoService.save(centroCosto, centroCosto.getCodigo());
		return centroCosto;
	}

	@Override
	public Long validaExistenAsientos(Long idCentroCosto) throws Throwable {
		System.out.println("Ingresa al metodo validaByNaturalezaCuenta de naturalezaCuenta con naturaleza: " + idCentroCosto);
		List<DetalleAsiento> listado = detalleAsientoDaoService.selectByIdCentroCosto(idCentroCosto);
		return Long.valueOf(listado.size());
	}

}
