package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ProductoDaoService;
import com.saa.ejb.credito.service.ProductoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Producto;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ProductoServiceImpl implements ProductoService {

    @EJB
    private ProductoDaoService productoDaoService;

    /**
     * Recupera un registro de Producto por su ID.
     */
    @Override
    public Producto selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return productoDaoService.selectById(id, NombreEntidadesCredito.PRODUCTO);
    }

    /**
     * Elimina uno o varios registros de Producto.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ProductoService ... depurado");
        Producto producto = new Producto();
        for (Long registro : id) {
            productoDaoService.remove(producto, registro);
        }
    }

    /**
     * Guarda una lista de registros de Producto.
     */
    @Override
    public void save(List<Producto> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ProductoService");
        for (Producto registro : lista) {
            productoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Producto.
     */
    @Override
    public List<Producto> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ProductoService");
        List<Producto> result = productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Producto no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Producto.
     */
    @Override
    public Producto saveSingle(Producto producto) throws Throwable {
        System.out.println("saveSingle - Producto");
        if(producto.getCodigo() == null){
        	producto.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        producto = productoDaoService.save(producto, producto.getCodigo());
        return producto;
    }

    /**
     * Recupera registros de Producto segun criterios de búsqueda.
     */
    @Override
    public List<Producto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ProductoService");
        List<Producto> result = productoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PRODUCTO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Producto no devolvio ningun registro");
        }
        return result;
    }
}
