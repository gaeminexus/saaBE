package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.DetalleCargaArchivoDaoService;
import com.saa.ejb.credito.service.DetalleCargaArchivoService;
import com.saa.model.credito.DetalleCargaArchivo;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetalleCargaArchivoServiceImpl implements DetalleCargaArchivoService {

    @EJB
    private DetalleCargaArchivoDaoService DetalleCargaArchivoDaoService;

    /**
     * Recupera un registro de DetalleCargaArchivo por su ID.
     */
    @Override
    public DetalleCargaArchivo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return DetalleCargaArchivoDaoService.selectById(id, NombreEntidadesCredito.DETALLE_CARGA_ARCHIVO);
    }

    /**
     * Elimina uno o varios registros de DetalleCargaArchivo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DetalleCargaArchivoService ... depurado");
        DetalleCargaArchivo detalle = new DetalleCargaArchivo();
        for (Long registro : id) {
            DetalleCargaArchivoDaoService.remove(detalle, registro);
        }
    }

    /**
     * Guarda una lista de registros de DetalleCargaArchivo.
     */
    @Override
    public void save(List<DetalleCargaArchivo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DetalleCargaArchivoService");
        for (DetalleCargaArchivo registro : lista) {
            DetalleCargaArchivoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de DetalleCargaArchivo.
     */
    @Override
    public List<DetalleCargaArchivo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DetalleCargaArchivoService");
        List<DetalleCargaArchivo> result = DetalleCargaArchivoDaoService.selectAll(NombreEntidadesCredito.DETALLE_CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DetalleCargaArchivo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de DetalleCargaArchivo.
     */
    @Override
    public DetalleCargaArchivo saveSingle(DetalleCargaArchivo detalle) throws Throwable {
        System.out.println("saveSingle - DetalleCargaArchivo");
        if (detalle.getCodigo() == null) {
            detalle.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
        }
        detalle = DetalleCargaArchivoDaoService.save(detalle, detalle.getCodigo());
        return detalle;
    }

    /**
     * Recupera registros de DetalleCargaArchivo segun criterios de búsqueda.
     */
    @Override
    public List<DetalleCargaArchivo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DetalleCargaArchivoService");
        List<DetalleCargaArchivo> result = DetalleCargaArchivoDaoService.selectByCriteria(datos, NombreEntidadesCredito.DETALLE_CARGA_ARCHIVO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio DetalleCargaArchivo no devolvio ningun registro");
        }
        return result;
    }
}
