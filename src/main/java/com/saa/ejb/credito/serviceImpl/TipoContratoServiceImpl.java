package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoContratoDaoService;
import com.saa.ejb.credito.service.TipoContratoService;
import com.saa.model.credito.TipoContrato;
import com.saa.rubros.Estado;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoContratoServiceImpl implements TipoContratoService {

    @EJB
    private TipoContratoDaoService tipoContratoDaoService;

    /**
     * Recupera un registro de TipoContrato por su ID.
     */
    @Override
    public TipoContrato selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById TipoContrato con id: " + id);
        return tipoContratoDaoService.selectById(id, NombreEntidadesCredito.TIPO_CONTRATO);
    }

    /**
     * Elimina uno o varios registros de TipoContrato.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoContratoService ... depurado");
        TipoContrato tipoContrato = new TipoContrato();
        for (Long registro : id) {
            tipoContratoDaoService.remove(tipoContrato, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoContrato.
     */
    @Override
    public void save(List<TipoContrato> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoContratoService");
        for (TipoContrato registro : lista) {
            tipoContratoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoContrato.
     */
    @Override
    public List<TipoContrato> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoContratoService");
        List<TipoContrato> result = tipoContratoDaoService.selectAll(NombreEntidadesCredito.TIPO_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoContrato no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoContrato.
     */
    @Override
    public TipoContrato saveSingle(TipoContrato tipoContrato) throws Throwable {
        System.out.println("saveSingle - TipoContrato");
        if(tipoContrato.getCodigo() == null){
        	tipoContrato.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        tipoContrato = tipoContratoDaoService.save(tipoContrato, tipoContrato.getCodigo());
        return tipoContrato;
    }

    /**
     * Recupera registros de TipoContrato segun criterios de búsqueda.
     */
    @Override
    public List<TipoContrato> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoContratoService");
        List<TipoContrato> result = tipoContratoDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoContrato no devolvio ningun registro");
        }
        return result;
    }
}
