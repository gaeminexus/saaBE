package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoIdentificacionDaoService;
import com.saa.ejb.credito.service.TipoIdentificacionService;
import com.saa.model.credito.TipoIdentificacion;
import com.saa.rubros.Estado;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoIdentificacionServiceImpl implements TipoIdentificacionService {

    @EJB
    private TipoIdentificacionDaoService tipoIdentificacionDaoService;

    /**
     * Recupera un registro de TipoIdentificacion por su ID.
     */
    @Override
    public TipoIdentificacion selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return tipoIdentificacionDaoService.selectById(id, NombreEntidadesCredito.TIPO_IDENTIFICACION);
    }

    /**
     * Elimina uno o varios registros de TipoIdentificacion.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoIdentificacionService ... depurado");
        TipoIdentificacion tipoIdentificacion = new TipoIdentificacion();
        for (Long registro : id) {
            tipoIdentificacionDaoService.remove(tipoIdentificacion, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoIdentificacion.
     */
    @Override
    public void save(List<TipoIdentificacion> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoIdentificacionService");
        for (TipoIdentificacion registro : lista) {
            tipoIdentificacionDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoIdentificacion.
     */
    @Override
    public List<TipoIdentificacion> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoIdentificacionService");
        List<TipoIdentificacion> result = tipoIdentificacionDaoService.selectAll(NombreEntidadesCredito.TIPO_IDENTIFICACION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoIdentificacion no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoIdentificacion.
     */
    @Override
    public TipoIdentificacion saveSingle(TipoIdentificacion tipoIdentificacion) throws Throwable {
        System.out.println("saveSingle - TipoIdentificacion");
        if(tipoIdentificacion.getCodigo() == null){
        	tipoIdentificacion.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        tipoIdentificacion = tipoIdentificacionDaoService.save(tipoIdentificacion, tipoIdentificacion.getCodigo());
        return tipoIdentificacion;
    }

    /**
     * Recupera registros de TipoIdentificacion segun criterios de búsqueda.
     */
    @Override
    public List<TipoIdentificacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoIdentificacionService");
        List<TipoIdentificacion> result = tipoIdentificacionDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_IDENTIFICACION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoIdentificacion no devolvio ningun registro");
        }
        return result;
    }
}
