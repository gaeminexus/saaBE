package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.BioProfileDaoService;
import com.saa.ejb.credito.service.BioProfileService;
import com.saa.model.crd.BioProfile;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class BioProfileServiceImpl implements BioProfileService {

    @EJB
    private BioProfileDaoService bioProfileDaoService;

    /**
     * Recupera un registro de BioProfile por su ID.
     */
    @Override
    public BioProfile selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById BioProfile con id: " + id);
        return bioProfileDaoService.selectById(id, NombreEntidadesCredito.BIO_PROFILE);
    }

    /**
     * Elimina uno o varios registros de BioProfile.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de BioProfileService ... depurado");
        BioProfile bio = new BioProfile();
        for (Long registro : id) {
            bioProfileDaoService.remove(bio, registro);
        }
    }

    /**
     * Guarda una lista de registros de BioProfile.
     */
    @Override
    public void save(List<BioProfile> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de BioProfileService");
        for (BioProfile registro : lista) {
            bioProfileDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de BioProfile.
     */
    @Override
    public List<BioProfile> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll BioProfileService");
        List<BioProfile> result = bioProfileDaoService.selectAll(NombreEntidadesCredito.BIO_PROFILE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total BioProfile no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de BioProfile.
     */
    @Override
    public BioProfile saveSingle(BioProfile bio) throws Throwable {
        System.out.println("saveSingle - BioProfile");
        if (bio.getCodigo() == null) {
            bio.setEstado(Long.valueOf(Estado.ACTIVO)); // Activo
        }
        bio = bioProfileDaoService.save(bio, bio.getCodigo());
        return bio;
    }

    /**
     * Recupera registros de BioProfile según criterios de búsqueda.
     */
    @Override
    public List<BioProfile> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria BioProfileService");
        List<BioProfile> result = bioProfileDaoService.selectByCriteria(datos, NombreEntidadesCredito.BIO_PROFILE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio BioProfile no devolvio ningun registro");
        }
        return result;
    }
}
