package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.MotivoPrestamoDaoService;
import com.saa.ejb.credito.service.MotivoPrestamoService;
import com.saa.model.credito.MotivoPrestamo;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class MotivoPrestamoServiceImpl implements MotivoPrestamoService {

    @EJB
    private MotivoPrestamoDaoService motivoPrestamoDaoService;

    /**
     * Recupera un registro de MotivoPrestamo por su ID.
     */
    @Override
    public MotivoPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById MotivoPrestamo con id: " + id);
        return motivoPrestamoDaoService.selectById(id, NombreEntidadesCredito.MOTIVO_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de MotivoPrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de MotivoPrestamoService ... depurado");
        MotivoPrestamo motivo = new MotivoPrestamo();
        for (Long registro : id) {
            motivoPrestamoDaoService.remove(motivo, registro);
        }
    }

    /**
     * Guarda una lista de registros de MotivoPrestamo.
     */
    @Override
    public void save(List<MotivoPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de MotivoPrestamoService");
        for (MotivoPrestamo registro : lista) {
            motivoPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de MotivoPrestamo.
     */
    @Override
    public List<MotivoPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll MotivoPrestamoService");
        List<MotivoPrestamo> result = motivoPrestamoDaoService.selectAll(NombreEntidadesCredito.MOTIVO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total MotivoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de MotivoPrestamo.
     */
    @Override
    public MotivoPrestamo saveSingle(MotivoPrestamo motivo) throws Throwable {
        System.out.println("saveSingle - MotivoPrestamo");
        if(motivo.getCodigo() == null){
        	motivo.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        motivo = motivoPrestamoDaoService.save(motivo, motivo.getCodigo());
        return motivo;
    }

    /**
     * Recupera registros de MotivoPrestamo segun criterios de búsqueda.
     */
    @Override
    public List<MotivoPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria MotivoPrestamoService");
        List<MotivoPrestamo> result =
                motivoPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.MOTIVO_PRESTAMO);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio MotivoPrestamo no devolvio ningun registro");
        }
        return result;
    }
}
