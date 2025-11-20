package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoCesantiaDaoService;
import com.saa.ejb.credito.service.TipoCesantiaService;
import com.saa.model.credito.TipoCesantia;
import com.saa.rubros.Estado;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoCesantiaServiceImpl implements TipoCesantiaService {

    @EJB
    private TipoCesantiaDaoService tipoCesantiaDaoService;

    /**
     * Recupera un registro de TipoCesantia por su ID.
     */
    @Override
    public TipoCesantia selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoCesantia con id: " + id);
        return tipoCesantiaDaoService.selectById(id, NombreEntidadesCredito.TIPO_CESANTIA);
    }

    /**
     * Elimina uno o varios registros de TipoCesantia.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoCesantiaService ... depurado");
        TipoCesantia tipoCesantia = new TipoCesantia();
        for (Long registro : id) {
            tipoCesantiaDaoService.remove(tipoCesantia, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoCesantia.
     */
    @Override
    public void save(List<TipoCesantia> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoCesantiaService");
        for (TipoCesantia registro : lista) {
            tipoCesantiaDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoCesantia.
     */
    @Override
    public List<TipoCesantia> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoCesantiaService");
        List<TipoCesantia> result = tipoCesantiaDaoService.selectAll(NombreEntidadesCredito.TIPO_CESANTIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoCesantia no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoCesantia.
     */
    @Override
    public TipoCesantia saveSingle(TipoCesantia tipoCesantia) throws Throwable {
        System.out.println("saveSingle - TipoCesantia");
        if(tipoCesantia.getCodigo() == null){
        	tipoCesantia.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        tipoCesantia = tipoCesantiaDaoService.save(tipoCesantia, tipoCesantia.getCodigo());
        return tipoCesantia;
    }

    /**
     * Recupera registros de TipoCesantia segun criterios de búsqueda.
     */
    @Override
    public List<TipoCesantia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoCesantiaService");
        List<TipoCesantia> result = tipoCesantiaDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_CESANTIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoCesantia no devolvio ningun registro");
        }
        return result;
    }
}
