package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoGeneroDaoService;
import com.saa.ejb.credito.service.TipoGeneroService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoGenero;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoGeneroServiceImpl implements TipoGeneroService {

    @EJB
    private TipoGeneroDaoService tipoGeneroDaoService;

    /**
     * Recupera un registro de TipoGenero por su ID.
     */
    @Override
    public TipoGenero selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoGenero con id: " + id);
        return tipoGeneroDaoService.selectById(id, NombreEntidadesCredito.TIPO_GENERO);
    }

    /**
     * Elimina uno o varios registros de TipoGenero.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoGeneroService ... depurado");
        TipoGenero tipoGenero = new TipoGenero();
        for (Long registro : id) {
            tipoGeneroDaoService.remove(tipoGenero, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoGenero.
     */
    @Override
    public void save(List<TipoGenero> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoGeneroService");
        for (TipoGenero registro : lista) {
            tipoGeneroDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoGenero.
     */
    @Override
    public List<TipoGenero> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoGeneroService");
        List<TipoGenero> result = tipoGeneroDaoService.selectAll(NombreEntidadesCredito.TIPO_GENERO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoGenero no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoGenero.
     */
    @Override
    public TipoGenero saveSingle(TipoGenero tipoGenero) throws Throwable {
        System.out.println("saveSingle - TipoGenero");
        if(tipoGenero.getCodigo() == null){
        	tipoGenero.setIdEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        tipoGenero = tipoGeneroDaoService.save(tipoGenero, tipoGenero.getCodigo());
        return tipoGenero;
    }

    /**
     * Recupera registros de TipoGenero según criterios de búsqueda.
     */
    @Override
    public List<TipoGenero> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoGeneroService");
        List<TipoGenero> result = tipoGeneroDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_GENERO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoGenero no devolvio ningun registro");
        }
        return result;
    }
}
