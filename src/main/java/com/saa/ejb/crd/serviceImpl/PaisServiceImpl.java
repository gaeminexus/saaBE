package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.PaisDaoService;
import com.saa.ejb.crd.service.PaisService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Pais;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PaisServiceImpl implements PaisService {

    @EJB
    private PaisDaoService paisDaoService;

    /**
     * Recupera un registro de Pais por su ID.
     */
    @Override
    public Pais selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Pais con id: " + id);
        return paisDaoService.selectById(id, NombreEntidadesCredito.PAIS);
    }

    /**
     * Elimina uno o varios registros de Pais.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PaisService ... depurado");
        Pais pais = new Pais();
        for (Long registro : id) {
            paisDaoService.remove(pais, registro);
        }
    }

    /**
     * Guarda una lista de registros de Pais.
     */
    @Override
    public void save(List<Pais> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PaisService");
        for (Pais registro : lista) {
            paisDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Pais.
     */
    @Override
    public List<Pais> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PaisService");
        List<Pais> result = paisDaoService.selectAll(NombreEntidadesCredito.PAIS);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Pais no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Pais.
     */
    @Override
    public Pais saveSingle(Pais pais) throws Throwable {
        System.out.println("saveSingle - Pais");
        if(pais.getCodigo() == null){
        	pais.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        pais = paisDaoService.save(pais, pais.getCodigo());
        return pais;
    }

    /**
     * Recupera registros de Pais segun criterios de búsqueda.
     */
    @Override
    public List<Pais> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PaisService");
        List<Pais> result = paisDaoService.selectByCriteria(datos, NombreEntidadesCredito.PAIS);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Pais no devolvio ningun registro");
        }
        return result;
    }
}
