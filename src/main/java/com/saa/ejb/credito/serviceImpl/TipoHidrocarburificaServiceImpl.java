package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoHidrocarburificaDaoService;
import com.saa.ejb.credito.service.TipoHidrocarburificaService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoHidrocarburifica;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoHidrocarburificaServiceImpl implements TipoHidrocarburificaService {

    @EJB
    private TipoHidrocarburificaDaoService tipoHidrocarburificaDaoService;

    /**
     * Recupera un registro de TipoHidrocarburifica por su ID.
     */
    @Override
    public TipoHidrocarburifica selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return tipoHidrocarburificaDaoService.selectById(id, NombreEntidadesCredito.TIPO_HIDROCARBURIFICA);
    }

    /**
     * Elimina uno o varios registros de TipoHidrocarburifica.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoHidrocarburificaService ... depurado");
        TipoHidrocarburifica tipoHidrocarburifica = new TipoHidrocarburifica();
        for (Long registro : id) {
            tipoHidrocarburificaDaoService.remove(tipoHidrocarburifica, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoHidrocarburifica.
     */
    @Override
    public void save(List<TipoHidrocarburifica> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoHidrocarburificaService");
        for (TipoHidrocarburifica registro : lista) {
            tipoHidrocarburificaDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoHidrocarburifica.
     */
    @Override
    public List<TipoHidrocarburifica> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoHidrocarburificaService");
        List<TipoHidrocarburifica> result = tipoHidrocarburificaDaoService.selectAll(NombreEntidadesCredito.TIPO_HIDROCARBURIFICA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoHidrocarburifica no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoHidrocarburifica.
     */
    @Override
    public TipoHidrocarburifica saveSingle(TipoHidrocarburifica tipoHidrocarburifica) throws Throwable {
        System.out.println("saveSingle - TipoHidrocarburifica");
        if(tipoHidrocarburifica.getCodigo() == null){
        	tipoHidrocarburifica.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        tipoHidrocarburifica = tipoHidrocarburificaDaoService.save(tipoHidrocarburifica, tipoHidrocarburifica.getCodigo());
        return tipoHidrocarburifica;
    }

    /**
     * Recupera registros de TipoHidrocarburifica segun criterios de búsqueda.
     */
    @Override
    public List<TipoHidrocarburifica> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoHidrocarburificaService");
        List<TipoHidrocarburifica> result = tipoHidrocarburificaDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_HIDROCARBURIFICA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoHidrocarburifica no devolvio ningun registro");
        }
        return result;
    }
}
