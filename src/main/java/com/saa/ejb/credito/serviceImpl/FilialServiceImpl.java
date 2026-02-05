package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.FilialDaoService;
import com.saa.ejb.credito.service.FilialService;
import com.saa.model.crd.Filial;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class FilialServiceImpl implements FilialService {

    @EJB
    private FilialDaoService filialDaoService;

    /**
     * Recupera un registro de Filial por su ID.
     */
    @Override
    public Filial selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return filialDaoService.selectById(id, NombreEntidadesCredito.FILIAL);
    }

    /**
     * Elimina uno o varios registros de Filial.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de FilialService ... depurado");
        Filial filial = new Filial();
        for (Long registro : id) {
            filialDaoService.remove(filial, registro);
        }
    }

    /**
     * Guarda una lista de registros de Filial.
     */
    @Override
    public void save(List<Filial> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de FilialService");
        for (Filial registro : lista) {
            filialDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Filial.
     */
    @Override
    public List<Filial> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll FilialService");
        List<Filial> result = filialDaoService.selectAll(NombreEntidadesCredito.FILIAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Filial no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Filial.
     */
    @Override
    public Filial saveSingle(Filial filial) throws Throwable {
        System.out.println("saveSingle - Filial");
        if(filial.getCodigo() == null){
        	filial.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        filial = filialDaoService.save(filial, filial.getCodigo());
        return filial;
    }

    /**
     * Recupera registros de Filial segun criterios de búsqueda.
     */
    @Override
    public List<Filial> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria FilialService");
        List<Filial> result = filialDaoService.selectByCriteria(datos, NombreEntidadesCredito.FILIAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Filial no devolvio ningun registro");
        }
        return result;
    }
}
