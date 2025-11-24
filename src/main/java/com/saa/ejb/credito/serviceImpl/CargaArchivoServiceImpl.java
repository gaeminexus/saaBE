package com.saa.ejb.credito.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.CargaArchivoDaoService;
import com.saa.ejb.credito.service.CargaArchivoService;
import com.saa.model.credito.CargaArchivo;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CargaArchivoServiceImpl implements CargaArchivoService {

    @EJB
    private CargaArchivoDaoService CargaArchivoDaoService;

    /**
     * Recupera un registro de CargaArchivo por su ID.
     */
    @Override
    public CargaArchivo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return CargaArchivoDaoService.selectById(id, NombreEntidadesCredito.CARGA_ARCHIVO);
    }

    /**
     * Elimina uno o varios registros de CargaArchivo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CargaArchivoService ... depurado");
        CargaArchivo cargaArchivo = new CargaArchivo();
        for (Long registro : id) {
            CargaArchivoDaoService.remove(cargaArchivo, registro);
        }
    }

    /**
     * Guarda una lista de registros de CargaArchivo.
     */
    @Override
    public void save(List<CargaArchivo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CargaArchivoService");
        for (CargaArchivo registro : lista) {
            CargaArchivoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de CargaArchivo.
     */
    @Override
    public List<CargaArchivo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CargaArchivoService");
        List<CargaArchivo> result = CargaArchivoDaoService.selectAll(NombreEntidadesCredito.CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CargaArchivo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de CargaArchivo.
     */
    @Override
    public CargaArchivo saveSingle(CargaArchivo cargaArchivo) throws Throwable {
        System.out.println("saveSingle - CargaArchivo");
        if (cargaArchivo.getCodigo() == null) {
        	cargaArchivo.setFechaCarga(LocalDateTime.now());
            cargaArchivo.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
        }
        cargaArchivo = CargaArchivoDaoService.save(cargaArchivo, cargaArchivo.getCodigo());
        return cargaArchivo;
    }

    /**
     * Recupera registros de CargaArchivo segun criterios de búsqueda.
     */
    @Override
    public List<CargaArchivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CargaArchivoService");
        List<CargaArchivo> result = CargaArchivoDaoService.selectByCriteria(datos, NombreEntidadesCredito.CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CargaArchivo no devolvio ningun registro");
        }
        return result;
    }
}
