package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.ejb.rpr.dao.GarantiaRealG51DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG51Service;
import com.saa.ejb.rpr.service.NuevoPrestamoG46Service;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.GarantiaRealG51;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG51ServiceImpl implements GeneracionG51Service {

    @EJB private NuevoPrestamoG46Service        g46Service;
    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private GarantiaRealG51DaoService      cg51DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G51");

        // -------------------------------------------------------
        // Obtener los registros del G46 del mismo EJRC
        // -------------------------------------------------------
        DetalleEjecucionReporte ejrdG46 = ejrdService.selectByEjecucionYTipo(
            detalle.getEjecucionReporte().getCodigo(), "G46"
        );

        if (ejrdG46 == null) {
            System.out.println("G51 - No hay EJRD G46 en este EJRC. G51 vacío, OK.");
            return 0L;
        }

        List<NuevoPrestamoG46> registrosG46 = g46Service.selectByDetalle(ejrdG46.getCodigo());
        System.out.println("G51 - Registros G46 encontrados: " + registrosG46.size());

        if (registrosG46.isEmpty()) {
            System.out.println("G51 - G46 vacío. G51 vacío, OK.");
            return 0L;
        }

        long contador = 0L;

        for (NuevoPrestamoG46 g46 : registrosG46) {
            try {
                String numOp = g46.getNumeroOperacion();

                // TIPO DE GARANTÍA: por ahora siempre A21.
                // TODO: cuando el nombre del producto empiece con H → cambiar a otro código.
                String tipoGarantia = "A21";

                GarantiaRealG51 g51 = new GarantiaRealG51();
                g51.setTipoIdentificacion(g46.getTipoIdentificacion());
                g51.setIdentificacion(g46.getIdentificacion());
                g51.setNumeroOperacion(numOp);
                g51.setNumeroGarantia(numOp);              // igual al número de operación
                g51.setTipoGarantia(tipoGarantia);
                g51.setDescripcionGarantia("Cuenta Individual");
                g51.setValorAvaluo(0.0);
                g51.setFechaAvaluo(null);
                g51.setNumeroRegistroGarantia(null);
                g51.setFechaContabilizacion(null);
                g51.setPorcentajeCubre(100.0);
                g51.setEstadoRegistro("N");
                g51.setDetalleEjecucion(detalle);

                cg51DaoService.save(g51, null);
                System.out.println("G51 INSERT op: " + numOp + " tipoGarantia: " + tipoGarantia);
                contador++;

            } catch (Throwable e) {
                System.out.println("G51 ERROR g46.codigo=" + g46.getCodigo() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("G51 generado con " + contador + " registros");
        return contador;
    }
}
