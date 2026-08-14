package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.service.EventoPrestamoService;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación Service para EventoPrestamo (EVPR).
 *
 * @author Sistema SAA
 * @since 2026-08-13
 */
@Stateless
public class EventoPrestamoServiceImpl implements EventoPrestamoService {

    @EJB
    private EventoPrestamoDaoService eventoPrestamoDaoService;

    @Override
    public EventoPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById EventoPrestamo con id: " + id);
        return eventoPrestamoDaoService.selectById(id, NombreEntidadesCredito.EVENTO_PRESTAMO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EventoPrestamoService");
        EventoPrestamo evento = new EventoPrestamo();
        for (Long id : ids) {
            eventoPrestamoDaoService.remove(evento, id);
        }
    }

    @Override
    public void save(List<EventoPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EventoPrestamoService");
        for (EventoPrestamo registro : lista) {
            eventoPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<EventoPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EventoPrestamoService");
        List<EventoPrestamo> result = eventoPrestamoDaoService.selectAll(NombreEntidadesCredito.EVENTO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total EventoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public EventoPrestamo saveSingle(EventoPrestamo evento) throws Throwable {
        System.out.println("saveSingle - EventoPrestamo");
        evento = eventoPrestamoDaoService.save(evento, evento.getCodigo());
        return evento;
    }

    @Override
    public List<EventoPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EventoPrestamoService");
        List<EventoPrestamo> result = eventoPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.EVENTO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio EventoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<EventoPrestamo> listarPorPrestamo(Long codigoPrestamo) throws Throwable {
        System.out.println("Service: Listando eventos del préstamo: " + codigoPrestamo);
        return eventoPrestamoDaoService.selectByPrestamo(codigoPrestamo);
    }
}
