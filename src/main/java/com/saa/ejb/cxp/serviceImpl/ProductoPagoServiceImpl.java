/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.ejb.Mensaje;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.CuentaService;
import com.saa.ejb.cxp.dao.GrupoProductoPagoDaoService;
import com.saa.ejb.cxp.dao.ProductoPagoDaoService;
import com.saa.ejb.cxp.service.ProductoPagoService;
import com.saa.model.cxp.GrupoProductoPago;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.ProductoPago;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ProductoPagoService.
 *  Contiene los servicios relacionados con la entidad ProductoPago</p>
 */
@Stateless
public class ProductoPagoServiceImpl implements ProductoPagoService {
	
	@EJB
	private ProductoPagoDaoService productoPagoDaoService;

	@EJB
	private DetalleRubroService detalleRubroService;
	
	@EJB
	private CuentaService cuentaService;
	
	@EJB
	private GrupoProductoPagoDaoService grupoProductoPagoDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][], java.lang.Object[])
	 */
	public void save(List<ProductoPago> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de productoPago service");
		for (ProductoPago registro:lista) {			
			productoPagoDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de PlanCuenta service");
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
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
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll(java.lang.Object[])
	 */
	public List<ProductoPago> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ProductoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoPago> result = productoPagoDaoService.selectAll(NombreEntidadesPago.PRODUCTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda completa de productoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ProductoPagoService#selectById(java.lang.Long)
	 */
	public ProductoPago selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return productoPagoDaoService.selectById(id, NombreEntidadesPago.PRODUCTO_PAGO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ProductoPagoService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<ProductoPago> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProductoPago");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoPago> result = productoPagoDaoService.selectByCriteria(datos, NombreEntidadesPago.PRODUCTO_PAGO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda por criterio de productoPago no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#numeroRegistrosGrupo(java.lang.Long)
	 */
	public int numeroRegistrosGrupo(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo numeroRegistrosGrupo ProductoPagoService con idGrupo: " + idGrupo);
		//CREA SENTENCIA WHERE PARA VERIFICAR SI EXISTEN REGISTROS
		List<ProductoPago> result = productoPagoDaoService.selectByGrupo(idGrupo);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		return result.size();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#creaNodoArbolCero(java.lang.Long)
	 */
	public String creaNodoArbolCero(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo creaNodoArbolCero ProductoPagoService con idGrupo: " + idGrupo);
		//INICIALIZA VARIABLE DE RESULTADO
		String resultado = Mensaje.OK;
		int numeroRegistros = numeroRegistrosGrupo(idGrupo);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		if (numeroRegistros == 0) {
			//OBTIENE LA NATURALEZA DE CUENTA DE ACTIVOS PARA ESTA EMPRESA
			GrupoProductoPago grupoProductoPago = grupoProductoPagoDaoService.selectById(idGrupo, NombreEntidadesPago.GRUPO_PRODUCTO_PAGO);
			if (grupoProductoPago == null){
				resultado = "NO EXISTE GRUPO DE DE PRODUCTO POR LO QUE NO SE PODRA CREAR EL NODO";
			}else{
				//CREA EL OBJETO PARA PERSISTIRLO
				ProductoPago productoPago = new ProductoPago();
				productoPago.setNumero("0");
				productoPago.setEmpresa(grupoProductoPago.getEmpresa());
				productoPago.setNombre(grupoProductoPago.getNombre());
				productoPago.setGrupoProductoPago(grupoProductoPago);
				productoPago.setEstado(Long.valueOf(Estado.ACTIVO));
				productoPago.setFechaIngreso(LocalDateTime.now());
				productoPago.setNivel(0L);
				productoPago.setIdPadre(0L);
				productoPago.setTipoNivel(Long.valueOf(TipoCuentaContable.MOVIMIENTO));
				//PERSISTE EL OBJETO
				productoPagoDaoService.save(productoPago, productoPago.getCodigo());
				//RETORNA VARIABLE CON OK
				resultado = Mensaje.OK;
			}
			
		}else{
			System.out.println("EXISTEN REGISTROS EN EL GRUPO DE PRODUCTO POR LO QUE NO SE CREARÁ EL NODO PRINCIPAL");
			resultado = Mensaje.OK;
		}
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#saveProducto(java.lang.Object[][], java.lang.Object[], java.lang.Long)
	 */
	public String saveProducto(Object[][] object, Object[] campos, Long idGrupo)
			throws Throwable {
		System.out.println("Ingresa al metodo saveCuenta de productoPago service");
		//INSTANCIA NUEVA ENTIDAD
		ProductoPago productoPago = new ProductoPago();
		//INSTANCIA NUEVA ENTIDAD PARA PADRE
		ProductoPago productoPagoPadre = new ProductoPago();
		//INICIALIZA VARIABLE DE RESULTADO
		String resultado = Mensaje.OK;
		GrupoProductoPago grupoProductoPago = grupoProductoPagoDaoService.selectById(idGrupo, NombreEntidadesPago.GRUPO_PRODUCTO_PAGO);
		boolean tieneHijos = false;
		String numeroPadre = "";
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (int i = 0; i < object.length; i++) {			
			if (productoPago.getCodigo().equals(0L)) {
				//CREA NODO BASE
				resultado = creaNodoArbolCero(idGrupo);
				if (Mensaje.OK.equals(resultado)) {
					//VERIFICA SI SE TRATA DE UNA CUENTA DE PRIMER NIVEL
					Boolean primerNivel = cuentaService.verificaPrimerNivel(productoPago.getNumero());
					if (primerNivel) {
						//OBTIENE EL CODIGO DE LA CUENTA BASE
						productoPagoPadre = productoPagoDaoService.selectRaizByGrupo(idGrupo);
					}else{
						//OBTIENE EL NUMERO DEL PADRE
						numeroPadre = cuentaService.obtieneCuentaPadre(productoPago.getNumero());
						productoPagoPadre = productoPagoDaoService.selectByNumeroGrupo(numeroPadre, idGrupo);
					}
					if (productoPagoPadre == null) {
						resultado = "NO EXISTE EL NUMERO DE PRODUCTO PADRE" + numeroPadre;
						throw new IncomeException("NO EXISTE EL NUMERO DE PRODUCTO PADRE " + numeroPadre);
					}else{
						if(Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(productoPagoPadre.getTipoNivel())){
							if(tieneHijos){
								throw new IncomeException("NO SE PUEDE CREAR CUENTAS HIJOS LA CUENTA " + productoPagoPadre.getNumero() + " YA QUE TIENE ASIENTOS O PLANTILLAS ASOCIADOS");
							}	
						}
						// RECUPERA GRUPO AL QUE PERTENECE
						productoPago.setGrupoProductoPago(grupoProductoPago);
						// ASIGNA CODIGO DE PADRE
						productoPago.setIdPadre(productoPagoPadre.getCodigo());
						// ASIGNA LA EMPRESA
						productoPago.setEmpresa(grupoProductoPago.getEmpresa());
						// ASIGNA EL TIPO DE CUENTA COMO DE MOVIMIENTO
						productoPago.setTipoNivel(Long.valueOf(TipoCuentaContable.MOVIMIENTO));
						// ASIGNA EL NIVEL
						productoPago.setNivel(productoPagoPadre.getNivel() + 1);
						// ASIGNA ESTADO
						productoPago.setEstado(Long.valueOf(Estado.ACTIVO));
						// ASIGNA FECHA CREACION
						productoPago.setFechaIngreso(LocalDateTime.now());
						// ALMACENA EL REGISTRO
						productoPagoDaoService.save(productoPago, Long.valueOf("0"));					
						// MODIFICA PADRE COMO ACUMULACION
						productoPagoPadre.setTipoNivel(Long.valueOf(TipoCuentaContable.ACUMULACION));
						// ALMACENA EL REGISTRO
						productoPagoDaoService.save(productoPagoPadre, productoPagoPadre.getCodigo());	
					}					
				}			
			}else{
				//INSERTA O ACTUALIZA REGISTRO
				productoPagoDaoService.save(productoPago, productoPago.getCodigo());	
			}
		}
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#recuperaTipo(java.lang.Long)
	 */
	public Long recuperaTipo(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaTipo de ProductoPago con id: " + id);
		//INSTANCIA LA ENTIDAD
		ProductoPago productoPagos = new ProductoPago();
		ProductoPago productoPago = productoPagoDaoService.find(productoPagos, id);
		return productoPago.getTipoNivel();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#removeAcumulacion(java.lang.Long, boolean)
	 */
	public void removeAcumulacion(Long id, boolean actualiza) throws Throwable {
		System.out.println("Ingresa al metodo removeAcumulacion de ProductoPago con id: " + id + ", actualiza = " + actualiza);
		//INSTANCIA LA ENTIDAD
		ProductoPago productoPago = new ProductoPago();
		boolean tieneRegistros = false;
		List<ProductoPago> lista = recuperaProductosHijo(id);
		/*for (ProductoPago registro:lista){
			if (tieneRegistros) {
				break;
			}
		}*/
		if ((tieneRegistros) && (actualiza)) {
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (ProductoPago registro : lista){
				actualizaEstadoProducto(registro.getCodigo(), Estado.INACTIVO);
			}
			actualizaEstadoProducto(id, Estado.INACTIVO);
		}else{
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (ProductoPago registro : lista){
				productoPagoDaoService.remove(productoPago, registro.getCodigo());		
			}
			productoPagoDaoService.remove(productoPago, id);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#removeMovimiento(java.lang.Long)
	 */
	public void removeMovimiento(Long id) throws Throwable {
		System.out.println("Ingresa al metodo removeMovimiento de ProductoPago con id: " + id);

	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#recuperaIdPadre(java.lang.Long)
	 */
	public Long recuperaIdPadre(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaIdPadre de ProductoPago con id: " + id);
		ProductoPago productoPago = productoPagoDaoService.find(new ProductoPago(), id);
		return productoPago.getIdPadre();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#actualizaEstadoProducto(java.lang.Long, int)
	 */
	public void actualizaEstadoProducto(Long id, int tipo) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEstadoProducto de ProductoPago con id: " + id);
		ProductoPago productoPago = productoPagoDaoService.selectById(id, NombreEntidadesPago.PRODUCTO_PAGO);
		productoPago.setFechaAnulacion(LocalDateTime.now());
		productoPago.setEstado(Long.valueOf(tipo));
		productoPagoDaoService.save(productoPago, id);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#actualizaTipoProducto(java.lang.Long, int)
	 */
	public void actualizaTipoProducto(Long id, int tipo) throws Throwable {
		System.out.println("Ingresa al metodo actualizaTipoProducto de ProductoPago con id: " + id);
		ProductoPago productoPago = productoPagoDaoService.selectById(id, NombreEntidadesPago.PRODUCTO_PAGO);
		productoPago.setTipoNivel(Long.valueOf(tipo));
		productoPagoDaoService.save(productoPago, id);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#recuperaProductosHijo(java.lang.Long)
	 */
	public List<ProductoPago> recuperaProductosHijo(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaProductosHijo con id: " + id);		
		ProductoPago padre = productoPagoDaoService.selectById(id, NombreEntidadesPago.PRODUCTO_PAGO);
		return productoPagoDaoService.selectHijosByNumeroProducto(padre.getNumero(), padre.getGrupoProductoPago().getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#recuperaSiguienteHijo(java.lang.Long)
	 */
	public String recuperaSiguienteHijo(Long id) throws Throwable {
		System.out.println("Servicio recuperaSiguienteHijo de productoPago service");
		// INICIALIZA VARIABLES
		String producto = null;
		String productoPadre = null;
		String ultimoNumero = null;
		Long siguienteNumero = null;
		String productoFinal = null;
	
		// CREA SENTENCIA Y BUSCA ENTIDAD
		List<ProductoPago> productoPagoList = productoPagoDaoService.selectByIdPadre(id);
		if (!productoPagoList.isEmpty()) {			
			ProductoPago productoPago = buscaMayorNumeroProducto(productoPagoList);
			producto = productoPago.getNumero();
			productoPadre = cuentaService.obtieneCuentaPadre(producto);
			if ("0".equals(productoPadre)) {
				ultimoNumero = producto;													
			}else{
				ultimoNumero = producto.substring(productoPadre.length() + 1, producto.length());
			}			
			siguienteNumero = Long.valueOf(ultimoNumero) + 1;	
		}else{
			ProductoPago productoOriginal = productoPagoDaoService.selectById(id, NombreEntidadesPago.PRODUCTO_PAGO);
			productoPadre = productoOriginal.getNumero();
			siguienteNumero = Long.valueOf("01");	
		}		
		if ("0".equals(productoPadre)){
			productoFinal = String.valueOf(siguienteNumero);
		}else{
			productoFinal = productoPadre + '.' + siguienteNumero;	
		}
		
		return productoFinal;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoPagoService#buscaMayorNumeroProducto(java.util.List)
	 */
	public ProductoPago buscaMayorNumeroProducto(
			List<ProductoPago> listadoProducto) throws Throwable {
		System.out.println("Servicio buscaMayorNumeroProducto de ProductoPago ");
		String cuentaPadre = null;
		int ultimoNumero = 0;
		int mayor = 0;	
		ProductoPago productoPagoMayor = new ProductoPago();		
		for (ProductoPago registro : listadoProducto){
			cuentaPadre = cuentaService.obtieneCuentaPadre(registro.getNumero());
			if ("0".equals(cuentaPadre)) {
				ultimoNumero = Integer.valueOf(registro.getNumero().trim());													
			}else{
				ultimoNumero = Integer.valueOf(registro.getNumero().trim().substring(cuentaPadre.length() + 1, registro.getNumero().trim().length()));
			}
			if (ultimoNumero >= mayor){
				mayor = ultimoNumero;
				productoPagoMayor =  registro;
			}
		}
		return productoPagoMayor;
	}

	@Override
	public ProductoPago saveSingle(ProductoPago productoPago) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ProductoPago");
		productoPago = productoPagoDaoService.save(productoPago, productoPago.getCodigo());
		return productoPago;
	}
}