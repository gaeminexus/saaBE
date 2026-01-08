/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.ejb.Mensaje;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.service.CuentaService;
import com.saa.ejb.cxc.dao.GrupoProductoCobroDaoService;
import com.saa.ejb.cxc.dao.ProductoCobroDaoService;
import com.saa.ejb.cxc.service.ProductoCobroService;
import com.saa.model.cxc.GrupoProductoCobro;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.ProductoCobro;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ProductoCobroService.
 *  Contiene los servicios relacionados con la entidad ProductoCobro</p>
 */
@Stateless
public class ProductoCobroServiceImpl implements ProductoCobroService {
	
	@EJB
	private ProductoCobroDaoService productoCobroDaoService;
	
	@EJB
	private DetalleRubroService detalleRubroService;
	
	@EJB
	private CuentaService cuentaService;
	
	@EJB
	private GrupoProductoCobroDaoService grupoProductoCobroDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.util.List)
	 */
	public void save(List<ProductoCobro> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de productoCobro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (ProductoCobro registro : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			productoCobroDaoService.save(registro, registro.getCodigo());
		}
	}

	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de productoCobro service");
		//INSTANCIA UNA ENTIDAD
		ProductoCobro productoCobro = new ProductoCobro();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			productoCobroDaoService.remove(productoCobro, registro);	
		}				
	}		

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<ProductoCobro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll ProductoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoCobro> result = productoCobroDaoService.selectAll(NombreEntidadesCobro.PRODUCTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ProductoCobro no devolvio ningun registro");
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ProductoCobroService#selectById(java.lang.Long)
	 */
	public ProductoCobro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return productoCobroDaoService.selectById(id, NombreEntidadesCobro.PRODUCTO_COBRO);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.Service.ProductoCobroService#selectByCriteria(java.util.List)
	 */
	public List<ProductoCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria ProductoCobroService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ProductoCobro> result = productoCobroDaoService.selectByCriteria(datos, NombreEntidadesCobro.PRODUCTO_COBRO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total ProductoCobro no devolvio ningun registro");
		}
		return result;
	}

	/**
	 * Guarda un solo registro de ProductoCobro.
	 */
	@Override
	public ProductoCobro saveSingle(ProductoCobro productoCobro) throws Throwable {
		System.out.println("saveSingle - ProductoCobroService");
		productoCobro = productoCobroDaoService.save(productoCobro, productoCobro.getCodigo());
		return productoCobro;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#numeroRegistrosGrupo(java.lang.Long)
	 */
	public int numeroRegistrosGrupo(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo numeroRegistrosGrupo ProductoCobroService con idGrupo: " + idGrupo);
		//CREA SENTENCIA WHERE PARA VERIFICAR SI EXISTEN REGISTROS
		List<ProductoCobro> result = productoCobroDaoService.selectByGrupo(idGrupo);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		return result.size();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#creaNodoArbolCero(java.lang.Long)
	 */
	public String creaNodoArbolCero(Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo creaNodoArbolCero ProductoCobroService con idGrupo: " + idGrupo);
		//INICIALIZA VARIABLE DE RESULTADO
		String resultado = Mensaje.OK;
		int numeroRegistros = numeroRegistrosGrupo(idGrupo);
		//VERIFICA SI SE RECUPERARON REGISTROS O NO
		if (numeroRegistros == 0) {
			//OBTIENE LA NATURALEZA DE CUENTA DE ACTIVOS PARA ESTA EMPRESA
			GrupoProductoCobro grupoProductoCobro = grupoProductoCobroDaoService.selectById(idGrupo, NombreEntidadesCobro.GRUPO_PRODUCTO_COBRO);
			if (grupoProductoCobro == null){
				resultado = "NO EXISTE GRUPO DE DE PRODUCTO POR LO QUE NO SE PODRA CREAR EL NODO";
			}else{
				//CREA EL OBJETO PARA PERSISTIRLO
				ProductoCobro productoCobro = new ProductoCobro();
				productoCobro.setNumero("0");
				productoCobro.setEmpresa(grupoProductoCobro.getEmpresa());
				productoCobro.setNombre(grupoProductoCobro.getNombre());
				productoCobro.setGrupoProductoCobro(grupoProductoCobro);
				productoCobro.setEstado(Long.valueOf(Estado.ACTIVO));
				productoCobro.setFechaIngreso(LocalDateTime.now());
				productoCobro.setNivel(0L);
				productoCobro.setIdPadre(0L);
				productoCobro.setTipoNivel(Long.valueOf(TipoCuentaContable.MOVIMIENTO));
				//PERSISTE EL OBJETO
				productoCobroDaoService.save(productoCobro, productoCobro.getCodigo());
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
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#saveProducto(com.saa.model.cxc.ProductoCobro, java.lang.Long)
	 */
	public String saveProducto(ProductoCobro productoCobro, Long idGrupo) throws Throwable {
		System.out.println("Ingresa al metodo saveProducto de productoCobro service");
		//INSTANCIA NUEVA ENTIDAD PARA PADRE
		ProductoCobro productoCobroPadre = new ProductoCobro();
		//INICIALIZA VARIABLE DE RESULTADO
		String resultado = Mensaje.OK;
		GrupoProductoCobro grupoProductoCobro = grupoProductoCobroDaoService.selectById(idGrupo, NombreEntidadesCobro.GRUPO_PRODUCTO_COBRO);
		boolean tieneHijos = false;
		String numeroPadre = "";
		
		if (productoCobro.getCodigo().equals(0L)) {
			//CREA NODO BASE
			resultado = creaNodoArbolCero(idGrupo);
			if (Mensaje.OK.equals(resultado)) {
				//VERIFICA SI SE TRATA DE UNA CUENTA DE PRIMER NIVEL
				Boolean primerNivel = cuentaService.verificaPrimerNivel(productoCobro.getNumero());
				if (primerNivel) {
					//OBTIENE EL CODIGO DE LA CUENTA BASE
					productoCobroPadre = productoCobroDaoService.selectRaizByGrupo(idGrupo);
				}else{
					//OBTIENE EL NUMERO DEL PADRE
					numeroPadre = cuentaService.obtieneCuentaPadre(productoCobro.getNumero());
					productoCobroPadre = productoCobroDaoService.selectByNumeroGrupo(numeroPadre, idGrupo);
				}
				if (productoCobroPadre == null) {
					resultado = "NO EXISTE EL NUMERO DE PRODUCTO PADRE" + numeroPadre;
					throw new IncomeException("NO EXISTE EL NUMERO DE PRODUCTO PADRE " + numeroPadre);
				}else{
					if(Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(productoCobroPadre.getTipoNivel())){
						tieneHijos = verificaHijos(productoCobroPadre.getCodigo());
						if(tieneHijos){
							throw new IncomeException("NO SE PUEDE CREAR CUENTAS HIJOS LA CUENTA " + productoCobroPadre.getNumero() + " YA QUE TIENE ASIENTOS O PLANTILLAS ASOCIADOS");
						}	
					}
					// RECUPERA GRUPO AL QUE PERTENECE
					productoCobro.setGrupoProductoCobro(grupoProductoCobro);
					// ASIGNA CODIGO DE PADRE
					productoCobro.setIdPadre(productoCobroPadre.getCodigo());
					// ASIGNA LA EMPRESA
					productoCobro.setEmpresa(grupoProductoCobro.getEmpresa());
					// ASIGNA EL TIPO DE CUENTA COMO DE MOVIMIENTO
					productoCobro.setTipoNivel(Long.valueOf(TipoCuentaContable.MOVIMIENTO));
					// ASIGNA EL NIVEL
					productoCobro.setNivel(productoCobroPadre.getNivel() + 1);
					// ASIGNA ESTADO
					productoCobro.setEstado(Long.valueOf(Estado.ACTIVO));
					// ASIGNA FECHA CREACION
					productoCobro.setFechaIngreso(LocalDateTime.now());
					// ALMACENA EL REGISTRO
					productoCobroDaoService.save(productoCobro, Long.valueOf("0"));					
					// MODIFICA PADRE COMO ACUMULACION
					productoCobroPadre.setTipoNivel(Long.valueOf(TipoCuentaContable.ACUMULACION));
					// ALMACENA EL REGISTRO
					productoCobroDaoService.save(productoCobroPadre, productoCobroPadre.getCodigo());	
				}					
			}			
		}else{
			//INSERTA O ACTUALIZA REGISTRO
			productoCobroDaoService.save(productoCobro, productoCobro.getCodigo());	
		}
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);
		return false;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#recuperaTipo(java.lang.Long)
	 */
	public Long recuperaTipo(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaTipo de ProductoCobro con id: " + id);
		//INSTANCIA LA ENTIDAD
		ProductoCobro productoCobros = new ProductoCobro();
		ProductoCobro productoCobro = productoCobroDaoService.find(productoCobros, id);
		return productoCobro.getTipoNivel();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#removeAcumulacion(java.lang.Long, boolean)
	 */
	public void removeAcumulacion(Long id, boolean actualiza) throws Throwable {
		System.out.println("Ingresa al metodo removeAcumulacion de ProductoCobro con id: " + id + ", actualiza = " + actualiza);
		//INSTANCIA LA ENTIDAD
		ProductoCobro productoCobro = new ProductoCobro();
		boolean tieneRegistros = false;
		List<ProductoCobro> listadoHijos = recuperaProductosHijo(id);
		for (ProductoCobro registro : listadoHijos){
			tieneRegistros = verificaHijos(registro.getCodigo());
			if (tieneRegistros) {
				break;
			}
		}
		if ((tieneRegistros) && (actualiza)) {
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (ProductoCobro registro : listadoHijos){
				actualizaEstadoProducto(registro.getCodigo(), Estado.INACTIVO);
			}
			actualizaEstadoProducto(id, Estado.INACTIVO);
		}else{
			// INACTIVA	LOS REGISTROS ASOCIADOS
			for (ProductoCobro registro : listadoHijos){
				productoCobroDaoService.remove(productoCobro, registro.getCodigo());		
			}
			productoCobroDaoService.remove(productoCobro, id);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#removeMovimiento(java.lang.Long)
	 */
	public void removeMovimiento(Long id) throws Throwable {
		System.out.println("Ingresa al metodo removeMovimiento de ProductoCobro con id: " + id);
		//INSTANCIA LA ENTIDAD
		ProductoCobro productoCobro = new ProductoCobro();
		boolean tieneHijos = false;
		int numeroRegistrosNivel = 0;
		Long idPadre = recuperaIdPadre(id);
		tieneHijos = verificaHijos(id);
		if(tieneHijos){
			actualizaEstadoProducto(id, Estado.INACTIVO);
		}else{
			productoCobroDaoService.remove(productoCobro, id);	
		}
		numeroRegistrosNivel = productoCobroDaoService.numeroRegActivosByIdPadre(idPadre);
		if (numeroRegistrosNivel == 0) {
			if(idPadre != null){
				actualizaTipoProducto(idPadre, TipoCuentaContable.MOVIMIENTO);	
			}else{
				System.out.println("TRANSACCION EXITOSA - EL REGISTRO NO CUENTA CON PRODUCTO PADRE POR LO QUE NO SE REALIZARA NINGUN CAMBIO EN LOS PRODUCTOS SUPERIORES");
				throw new EJBException("TRANSACCION EXITOSA - EL REGISTRO NO CUENTA CON PRODUCTO PADRE POR LO QUE NO SE REALIZARA NINGUN CAMBIO EN LOS PRODUCTOS SUPERIORES");
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#recuperaIdPadre(java.lang.Long)
	 */
	public Long recuperaIdPadre(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaIdPadre de ProductoCobro con id: " + id);
		ProductoCobro productoCobro = productoCobroDaoService.find(new ProductoCobro(), id);
		return productoCobro.getIdPadre();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#actualizaEstadoProducto(java.lang.Long, int)
	 */
	public void actualizaEstadoProducto(Long id, int tipo) throws Throwable {
		System.out.println("Ingresa al metodo actualizaEstadoProducto de ProductoCobro con id: " + id);
		ProductoCobro productoCobro = productoCobroDaoService.selectById(id, NombreEntidadesCobro.PRODUCTO_COBRO);
		productoCobro.setEstado(Long.valueOf(tipo));
		productoCobroDaoService.save(productoCobro, id);		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#actualizaTipoProducto(java.lang.Long, int)
	 */
	public void actualizaTipoProducto(Long id, int tipo) throws Throwable {
		System.out.println("Ingresa al metodo actualizaTipoProducto de ProductoCobro con id: " + id);
		ProductoCobro productoCobro = productoCobroDaoService.selectById(id, NombreEntidadesCobro.PRODUCTO_COBRO);
		productoCobro.setTipoNivel(Long.valueOf(tipo));
		productoCobroDaoService.save(productoCobro, id);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.service.ProductoCobroService#recuperaProductosHijo(java.lang.Long)
	 */
	public List<ProductoCobro> recuperaProductosHijo(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaProductosHijo con id: " + id);		
		ProductoCobro padre = productoCobroDaoService.selectById(id, NombreEntidadesCobro.PRODUCTO_COBRO);
		return productoCobroDaoService.selectHijosByNumeroProducto(padre.getNumero(), padre.getGrupoProductoCobro().getCodigo());
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxc.ejb.service.ProductoCobroService#recuperaSiguienteHijo(java.lang.Long)
	 */
	public String recuperaSiguienteHijo(Long id) throws Throwable {
		System.out.println("Servicio recuperaSiguienteHijo de ProductoCobro service");
		// INICIALIZA VARIABLES
		String producto = null;
		String productoPadre = null;
		String ultimoNumero = null;
		Long siguienteNumero = null;
		String productoFinal = null;
	
		// CREA SENTENCIA Y BUSCA ENTIDAD
		List<ProductoCobro> productoCobroList = productoCobroDaoService.selectByIdPadre(id);
		if (!productoCobroList.isEmpty()) {			
			ProductoCobro productoPago = buscaMayorNumeroProducto(productoCobroList);
			producto = productoPago.getNumero();
			productoPadre = cuentaService.obtieneCuentaPadre(producto);
			if ("0".equals(productoPadre)) {
				ultimoNumero = producto;													
			}else{
				ultimoNumero = producto.substring(productoPadre.length() + 1, producto.length());
			}			
			siguienteNumero = Long.valueOf(ultimoNumero) + 1;	
		}else{
			ProductoCobro productoOriginal = productoCobroDaoService.selectById(id, NombreEntidadesCobro.PRODUCTO_COBRO);
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
	 * @see com.compuseg.income.cxc.ejb.service.ProductoCobroService#buscaMayorNumeroProducto(java.util.List)
	 */
	public ProductoCobro buscaMayorNumeroProducto(List<ProductoCobro> listadoProducto) throws Throwable {
		System.out.println("Servicio buscaMayorNumeroProducto de ProductoCobro ");
		String cuentaPadre = null;
		int ultimoNumero = 0;
		int mayor = 0;	
		ProductoCobro productoCobroMayor = new ProductoCobro();		
		for (ProductoCobro registro : listadoProducto){
			cuentaPadre = cuentaService.obtieneCuentaPadre(registro.getNumero());
			if ("0".equals(cuentaPadre)) {
				ultimoNumero = Integer.valueOf(registro.getNumero().trim());													
			}else{
				ultimoNumero = Integer.valueOf(registro.getNumero().trim().substring(cuentaPadre.length() + 1, registro.getNumero().trim().length()));
			}
			if (ultimoNumero >= mayor){
				mayor = ultimoNumero;
				productoCobroMayor =  registro;
			}
		}
		return productoCobroMayor;
	}
}