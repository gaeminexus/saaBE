package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import com.saa.ejb.crd.service.ExterService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.ProvinciaService;
import com.saa.ejb.rpr.dao.NuevoPrestamoG46DaoService;
import com.saa.ejb.rpr.service.GeneracionG46Service;
import com.saa.model.crd.Exter;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Provincia;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG46ServiceImpl implements GeneracionG46Service {

    @EJB private PrestamoService            prestamoService;
    @EJB private ExterService               exterService;
    @EJB private ProvinciaService           provinciaService;
    @EJB private NuevoPrestamoG46DaoService cg46DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G46");

        // -------------------------------------------------------
        // 0. Calcular rango: primer y último día del mes de ejecución
        // -------------------------------------------------------
        long mes  = detalle.getEjecucionReporte().getMes();
        long anio = detalle.getEjecucionReporte().getAnio();
        int ultimoDia = YearMonth.of((int) anio, (int) mes).lengthOfMonth();
        LocalDateTime fechaInicio = LocalDateTime.of((int) anio, (int) mes, 1, 0, 0, 0);
        LocalDateTime fechaFin    = LocalDateTime.of((int) anio, (int) mes, ultimoDia, 23, 59, 59);
        System.out.println("G46 - rango: " + fechaInicio + " a " + fechaFin);

        // -------------------------------------------------------
        // 1. Obtener préstamos cuya fecha esté dentro del mes de ejecución
        // -------------------------------------------------------
        List<Prestamo> prestamos = prestamoService.selectByRangoFechas(fechaInicio, fechaFin);
        System.out.println("G46 - Prestamos encontrados: " + prestamos.size());

        if (prestamos.isEmpty()) {
            System.out.println("G46 - Sin prestamos en el mes. G46 vacio, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Por cada préstamo → mapear a CG46 e INSERT
        // -------------------------------------------------------
        long contador = 0L;

        for (Prestamo prestamo : prestamos) {
            // FILTRO: Solo préstamos con idAsoprep válido
            if (prestamo.getIdAsoprep() == null || prestamo.getIdAsoprep().toString().trim().isEmpty()) {
                System.out.println("G46 - SKIP prestamo codigo " + prestamo.getCodigo() + " (idAsoprep nulo o vacío)");
                continue;
            }

            NuevoPrestamoG46 g46 = new NuevoPrestamoG46();

            // Identificación del partícipe
            g46.setTipoIdentificacion("C");
            if (prestamo.getEntidad() != null) {
                g46.setIdentificacion(prestamo.getEntidad().getNumeroIdentificacion());
            }

            // Número de operación = idAsoprep
            if (prestamo.getIdAsoprep() != null) {
                g46.setNumeroOperacion(String.valueOf(prestamo.getIdAsoprep()));
            }

            // Tipo de crédito = primera letra del nombre del producto
            if (prestamo.getProducto() != null && prestamo.getProducto().getNombre() != null
                    && !prestamo.getProducto().getNombre().isEmpty()) {
                char inicialProducto = Character.toUpperCase(prestamo.getProducto().getNombre().charAt(0));
                g46.setTipoCredito(String.valueOf(inicialProducto == 'E' ? 'Q' : inicialProducto));
            }

            // Valores fijos
            g46.setEstadoOperacion("N");
            g46.setSituacionOperacion("N");
            g46.setPeriodicidadPago("ME");
            g46.setFrecuenciaRevision("365");
            g46.setGarantias("GA");

            // Destino geográfico — provincia desde EXTR → PRVN, canton y parroquia fijos
            String identificacionEntidad = prestamo.getEntidad() != null
                    ? prestamo.getEntidad().getNumeroIdentificacion() : null;
            String codigoAlternoProvincia = null;
            if (identificacionEntidad != null) {
                try {
                    List<Exter> exters = exterService.selectByCedula(identificacionEntidad);
                    if (exters != null && !exters.isEmpty() && exters.get(0).getProvincia() != null
                            && !exters.get(0).getProvincia().isEmpty()) {
                        Provincia prov = provinciaService.selectByNombre(exters.get(0).getProvincia());
                        if (prov != null) {
                            codigoAlternoProvincia = prov.getCodigoAlterno();
                        }
                    }
                } catch (Throwable ex) {
                    System.out.println("G46 WARN - No se pudo obtener provincia para: " + identificacionEntidad);
                }
            }
            g46.setDestinoProvincia(codigoAlternoProvincia);
            g46.setDestinoCanton("01");
            g46.setDestinoParroquia("50");

            // Fechas de concesión y vencimiento
            if (prestamo.getFecha() != null) {
                g46.setFechaConcesion(prestamo.getFecha().toLocalDate());
            }
            if (prestamo.getFechaFin() != null) {
                g46.setFechaVencimiento(prestamo.getFechaFin().toLocalDate());
            }

            // Valores monetarios
            g46.setValorOperacion(prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0);
            g46.setTasaInteresNominal(prestamo.getInteresNominal() != null ? prestamo.getInteresNominal() : 0.0);

            g46.setDetalleEjecucion(detalle);

            cg46DaoService.save(g46, null);
            System.out.println("G46 INSERT prestamo idAsoprep: " + prestamo.getIdAsoprep());
            contador++;
        }

        System.out.println("G46 generado con " + contador + " registros");
        return contador;
    }
}
