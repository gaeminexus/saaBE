package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.rpr.dao.NovacionG47DaoService;
import com.saa.ejb.rpr.service.GeneracionG47Service;
import com.saa.model.crd.Prestamo;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NovacionG47;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG47ServiceImpl implements GeneracionG47Service {

    private static final Long ESTADO_NOVACION = 9L;

    @EJB private PrestamoService       prestamoService;
    @EJB private NovacionG47DaoService cg47DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G47");

        // -------------------------------------------------------
        // 1. Obtener todos los préstamos con estadoPrestamo = 9
        // -------------------------------------------------------
        List<Prestamo> prestamos = prestamoService.selectByEstado(ESTADO_NOVACION);
        System.out.println("G47 - Prestamos con estado 9: " + prestamos.size());

        if (prestamos.isEmpty()) {
            System.out.println("G47 - Sin novaciones. G47 vacio, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Por cada préstamo → mapear a CG47 e INSERT
        // -------------------------------------------------------
        long contador = 0L;

        for (Prestamo prestamo : prestamos) {
            try {
                NovacionG47 g47 = new NovacionG47();

                // Identificación del partícipe
                g47.setTipoIdentificacion("C");
                if (prestamo.getEntidad() != null) {
                    g47.setIdentificacion(prestamo.getEntidad().getNumeroIdentificacion());
                }

                // Número de operación = idAsoprep
                if (prestamo.getIdAsoprep() != null) {
                    g47.setNumeroOperacion(String.valueOf(prestamo.getIdAsoprep()));
                }

                // Fecha de novación = fecha del préstamo
                if (prestamo.getFecha() != null) {
                    g47.setFechaNovacion(prestamo.getFecha().toLocalDate());
                }

                // numeroOperacionAnterior queda null (no hay campo fuente disponible)

                g47.setDetalleEjecucion(detalle);

                cg47DaoService.save(g47, null);
                System.out.println("G47 INSERT prestamo idAsoprep: " + prestamo.getIdAsoprep()
                    + " entidad: " + (prestamo.getEntidad() != null ? prestamo.getEntidad().getNumeroIdentificacion() : "N/A"));
                contador++;

            } catch (Throwable e) {
                System.out.println("G47 ERROR al insertar prestamo codigo " + prestamo.getCodigo() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("G47 generado con " + contador + " registros");
        return contador;
    }
}
