package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.ExterService;
import com.saa.ejb.crd.service.ProvinciaService;
import com.saa.ejb.rpr.dao.NuevoParticipeG45DaoService;
import com.saa.ejb.rpr.dao.SaldoCuentaG42DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG45Service;
import com.saa.ejb.rpr.service.NuevoPrestamoG46Service;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Exter;
import com.saa.model.crd.Provincia;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NuevoParticipeG45;
import com.saa.model.rpr.NuevoPrestamoG46;
import com.saa.model.rpr.SaldoCuentaG42;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG45ServiceImpl implements GeneracionG45Service {

    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private NuevoPrestamoG46Service        g46Service;
    @EJB private SaldoCuentaG42DaoService       g42DaoService;
    @EJB private EntidadService                 entidadService;
    @EJB private ExterService                   exterService;
    @EJB private ProvinciaService               provinciaService;
    @EJB private NuevoParticipeG45DaoService    cg45DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G45");

        // -------------------------------------------------------
        // 1. Obtener el EJRD del G46 del mismo EJRC
        //    G45 usa exactamente los mismos registros que el G46.
        // -------------------------------------------------------
        Long codigoEjrc = detalle.getEjecucionReporte().getCodigo();
        DetalleEjecucionReporte ejrdG46 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G46");

        if (ejrdG46 == null) {
            System.out.println("G45 - No se encontro EJRD del G46. G45 vacio, OK.");
            return 0L;
        }

        List<NuevoPrestamoG46> registrosG46 = g46Service.selectByDetalle(ejrdG46.getCodigo());
        System.out.println("G45 - Registros de CG46: " + registrosG46.size());

        if (registrosG46.isEmpty()) {
            System.out.println("G45 - G46 sin datos. G45 vacio, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Obtener el EJRD del G42 del mismo EJRC para calcular patrimonio
        // -------------------------------------------------------
        DetalleEjecucionReporte ejrdG42 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G42");

        long contador = 0L;

        // -------------------------------------------------------
        // 3. Por cada registro del G46, generar un registro G45 identico
        //    con los datos fijos requeridos y patrimonio desde G42.
        // -------------------------------------------------------
        for (NuevoPrestamoG46 g46 : registrosG46) {
            String identificacion = g46.getIdentificacion();
            if (identificacion == null) continue;

            // --- Entidad: necesaria para patrimonio y tipo de participe ---
            Entidad entidad = null;
            try {
                entidad = entidadService.selectByNumeroIdentificacion(identificacion);
            } catch (Throwable ex) {
                System.out.println("G45 WARN - No se pudo obtener entidad para: " + identificacion);
            }

            // --- Patrimonio: suma saldoAportePatronal + saldoAportePersonal + rendimiento desde G42 ---
            Double patrimonio = 0.0;
            if (ejrdG42 != null && entidad != null) {
                try {
                    SaldoCuentaG42 g42 = g42DaoService.selectByEntidadYDetalle(entidad.getCodigo(), ejrdG42);
                    if (g42 != null) {
                        double saldoPat = g42.getSaldoAportePatronal() != null ? g42.getSaldoAportePatronal() : 0.0;
                        double saldoPer = g42.getSaldoAportePersonal() != null ? g42.getSaldoAportePersonal() : 0.0;
                        double rendim   = g42.getRendimiento()         != null ? g42.getRendimiento()         : 0.0;
                        patrimonio = saldoPat + saldoPer + rendim;
                    }
                } catch (Throwable ex) {
                    System.out.println("G45 WARN - No se encontro G42 para entidad: " + entidad.getCodigo());
                }
            }

            // --- Tipo de participe: J si idEstado == 30, caso contrario C ---
            String tipoParticipe = "C";
            if (entidad != null && entidad.getIdEstado() != null && entidad.getIdEstado() == 30L) {
                tipoParticipe = "J";
            }

            // --- Exter: genero, estadoCivil, fechaNacimiento, provincia ---
            String genero      = null;
            String estadoCivil = null;
            java.time.LocalDate fechaNac = null;
            String codigoAlternoProvincia = null;
            try {
                List<Exter> exters = exterService.selectByCedula(identificacion);
                if (exters != null && !exters.isEmpty()) {
                    Exter exter = exters.get(0);
                    if (exter.getGenero() != null && !exter.getGenero().isEmpty()) {
                        char primeraLetraGenero = Character.toUpperCase(exter.getGenero().charAt(0));
                        if (primeraLetraGenero == 'H') {
                            genero = "M";
                        } else if (primeraLetraGenero == 'M') {
                            genero = "F";
                        } else {
                            genero = String.valueOf(primeraLetraGenero);
                        }
                    }
                    // ESTADO CIVIL: siempre se envía "S"
                    estadoCivil = "S";
                    if (exter.getFechaNacimiento() != null) {
                        fechaNac = exter.getFechaNacimiento().toLocalDate();
                    }
                    // --- Provincia: buscar en tabla PRVN por nombre ---
                    if (exter.getProvincia() != null && !exter.getProvincia().isEmpty()) {
                        try {
                            Provincia prov = provinciaService.selectByNombre(exter.getProvincia());
                            if (prov != null) {
                                codigoAlternoProvincia = prov.getCodigoAlterno();
                            }
                        } catch (Throwable ex) {
                            System.out.println("G45 WARN - No se encontro Provincia por nombre: " + exter.getProvincia());
                        }
                    }
                }
            } catch (Throwable ex) {
                System.out.println("G45 WARN - No se encontro Exter para: " + identificacion);
            }

            // --- Construir y guardar CG45 ---
            NuevoParticipeG45 g45 = new NuevoParticipeG45();
            g45.setIdentificacion(identificacion);
            g45.setTipoIdentificacion(g46.getTipoIdentificacion());
            g45.setGenero(genero);
            g45.setEstadoCivil(estadoCivil);
            g45.setFechaNacimiento(fechaNac);
            g45.setTipoParticipe(tipoParticipe);
            g45.setActividadEconomica("009");
            g45.setPatrimonio(patrimonio);
            g45.setProvincia(codigoAlternoProvincia);
            g45.setCanton("01");
            g45.setParroquia("50");
            g45.setProfesion("O");
            g45.setCargasFamiliares(0L);
            g45.setOrigenIngresos("B");
            g45.setDetalleEjecucion(detalle);

            cg45DaoService.save(g45, null);
            System.out.println("G45 INSERT participe: " + identificacion + " patrimonio=" + patrimonio);
            contador++;
        }

        System.out.println("G45 generado con " + contador + " registros");
        return contador;
    }
}