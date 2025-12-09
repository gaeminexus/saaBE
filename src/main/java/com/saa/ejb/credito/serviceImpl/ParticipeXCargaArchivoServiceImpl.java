package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ParticipeXCargaArchivoDaoService;
import com.saa.ejb.credito.service.ParticipeXCargaArchivoService;
import com.saa.model.credito.ParticipeXCargaArchivo;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ParticipeXCargaArchivoServiceImpl implements ParticipeXCargaArchivoService {

    @EJB
    private ParticipeXCargaArchivoDaoService ParticipeXCargaArchivoDaoService;

    /**
     * Recupera un registro de ParticipeXCargaArchivo por su ID.
     */
    @Override
    public ParticipeXCargaArchivo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return ParticipeXCargaArchivoDaoService.selectById(id, NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
    }

    /**
     * Elimina uno o varios registros de ParticipeXCargaArchivo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ParticipeXCargaArchivoService ... depurado");
        ParticipeXCargaArchivo participe = new ParticipeXCargaArchivo();
        for (Long registro : id) {
            ParticipeXCargaArchivoDaoService.remove(participe, registro);
        }
    }

    /**
     * Guarda una lista de registros de ParticipeXCargaArchivo.
     */
    @Override
    public void save(List<ParticipeXCargaArchivo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ParticipeXCargaArchivoService");
        for (ParticipeXCargaArchivo registro : lista) {
            ParticipeXCargaArchivoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de ParticipeXCargaArchivo.
     */
    @Override
    public List<ParticipeXCargaArchivo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParticipeXCargaArchivoService");
        List<ParticipeXCargaArchivo> result = ParticipeXCargaArchivoDaoService.selectAll(NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ParticipeXCargaArchivo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de ParticipeXCargaArchivo.
     */
    @Override
    public ParticipeXCargaArchivo saveSingle(ParticipeXCargaArchivo participe) throws Throwable {
        // System.out.println("saveSingle - ParticipeXCargaArchivo");
        if (participe.getCodigo() == null) {
            participe.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
        }
        participe = ParticipeXCargaArchivoDaoService.save(participe, participe.getCodigo());
        return participe;
    }

    /**
     * Recupera registros de ParticipeXCargaArchivo segun criterios de búsqueda.
     */
    @Override
    public List<ParticipeXCargaArchivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        // System.out.println("Ingresa al metodo selectByCriteria ParticipeXCargaArchivoService");
        List<ParticipeXCargaArchivo> result = ParticipeXCargaArchivoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ParticipeXCargaArchivo no devolvio ningun registro");
        }
        return result;
    }
}
