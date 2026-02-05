package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoAporteDaoService;
import com.saa.ejb.credito.service.TipoAporteService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoAporte;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoAporteServiceImpl implements TipoAporteService {

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    /**
     * Recupera un registro de TipoAporte por su ID.
     */
    @Override
    public TipoAporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoAporte con id: " + id);
        return tipoAporteDaoService.selectById(id, NombreEntidadesCredito.TIPO_APORTE);
    }

    /**
     * Elimina uno o varios registros de TipoAporte.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoAporteService ... depurado");
        TipoAporte tipoAporte = new TipoAporte();
        for (Long registro : id) {
            tipoAporteDaoService.remove(tipoAporte, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoAporte.
     */
    @Override
    public void save(List<TipoAporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoAporteService");
        for (TipoAporte registro : lista) {
            tipoAporteDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoAporte.
     */
    @Override
    public List<TipoAporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoAporteService");
        List<TipoAporte> result = tipoAporteDaoService.selectAll(NombreEntidadesCredito.TIPO_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoAporte no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoAporte.
     */
    @Override
    public TipoAporte saveSingle(TipoAporte tipoAporte) throws Throwable {
        System.out.println("saveSingle - TipoAporte");
        if(tipoAporte.getCodigo() == null){
        	tipoAporte.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        tipoAporte = tipoAporteDaoService.save(tipoAporte, tipoAporte.getCodigo());
        return tipoAporte;
    }

    /**
     * Recupera registros de TipoAporte segun criterios.
     */
    @Override
    public List<TipoAporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoAporteService");
        List<TipoAporte> result = tipoAporteDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoAporte no devolvio ningun registro");
        }
        return result;
    }
}
