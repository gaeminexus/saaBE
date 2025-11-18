package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ContratoDaoService;
import com.saa.ejb.credito.service.ContratoService;
import com.saa.model.credito.Contrato;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ContratoServiceImpl implements ContratoService {

    @EJB
    private ContratoDaoService contratoDaoService;

    /**
     * Recupera un registro de Contrato por su ID.
     */
    @Override
    public Contrato selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Contrato con id: " + id);
        return contratoDaoService.selectById(id, NombreEntidadesCredito.CONTRATO);
    }

    /**
     * Elimina uno o varios registros de Contrato.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ContratoService ... depurado");
        Contrato contrato = new Contrato();
        for (Long registro : id) {
            contratoDaoService.remove(contrato, registro);
        }
    }

    /**
     * Guarda una lista de registros de Contrato.
     */
    @Override
    public void save(List<Contrato> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ContratoService");
        for (Contrato registro : lista) {
            contratoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Contrato.
     */
    @Override
    public List<Contrato> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ContratoService");
        List<Contrato> result = contratoDaoService.selectAll(NombreEntidadesCredito.CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Contrato no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Contrato.
     */
    @Override
    public Contrato saveSingle(Contrato contrato) throws Throwable {
        System.out.println("saveSingle - Contrato");
        contrato = contratoDaoService.save(contrato, contrato.getCodigo());
        return contrato;
    }

    /**
     * Recupera registros de Contrato segun criterios de búsqueda.
     */
    @Override
    public List<Contrato> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ContratoService");
        List<Contrato> result = contratoDaoService.selectByCriteria(datos, NombreEntidadesCredito.CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Contrato no devolvio ningun registro");
        }
        return result;
    }
}
