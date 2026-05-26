package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.DireccionService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.ExterService;
import com.saa.ejb.crd.service.PerfilEconomicoService;
import com.saa.ejb.crd.service.ParticipeService;
import com.saa.ejb.rpr.dao.NuevoParticipeG45DaoService;
import com.saa.ejb.rpr.dao.ParticipeActivoG41DaoService;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG45Service;
import com.saa.model.crd.Direccion;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Exter;
import com.saa.model.crd.PerfilEconomico;
import com.saa.model.crd.Participe;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NuevoParticipeG45;
import com.saa.model.rpr.ParticipeActivoG41;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG45ServiceImpl implements GeneracionG45Service {

    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private ParticipeActivoG41DaoService   cg41DaoService;
    @EJB private EntidadService                 entidadService;
    @EJB private ExterService                   exterService;
    @EJB private DireccionService               direccionService;
    @EJB private PerfilEconomicoService         perfilEconomicoService;
    @EJB private ParticipeService               participeService;
    @EJB private NuevoParticipeG45DaoService    cg45DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G45");

        // -------------------------------------------------------
        // 1. Obtener el EJRD del G41 del mismo EJRC
        //    G41 ya cambió idEstado 1→10, así que no podemos filtrar por idEstado=1.
        //    Leemos directamente los registros que G41 insertó en CG41.
        // -------------------------------------------------------
        Long codigoEjrc = detalle.getEjecucionReporte().getCodigo();
        DetalleEjecucionReporte ejrdG41 = ejrdService.selectByEjecucionYTipo(codigoEjrc, "G41");

        if (ejrdG41 == null) {
            System.out.println("G45 - No se encontro EJRD del G41. G45 vacio, OK.");
            return 0L;
        }

        List<ParticipeActivoG41> registrosG41 = cg41DaoService.selectByDetalle(ejrdG41.getCodigo());
        System.out.println("G45 - Registros de CG41 (nuevos participes): " + registrosG41.size());

        if (registrosG41.isEmpty()) {
            System.out.println("G45 - Sin nuevos participes. G45 vacio, OK.");
            return 0L;
        }

        // -------------------------------------------------------
        // 2. Por cada nuevo partícipe del G41 → enriquecer y hacer INSERT en CG45
        // -------------------------------------------------------
        long contador = 0L;

        for (ParticipeActivoG41 g41 : registrosG41) {
            String identificacion = g41.getIdentificacion();
            if (identificacion == null) continue;

            // Buscar Entidad por número de identificación (query directa en BD)
            Entidad entidad = null;
            try {
                entidad = entidadService.selectByNumeroIdentificacion(identificacion);
            } catch (Throwable ex) {
                System.out.println("G45 WARN - No se pudo obtener entidad para: " + identificacion);
            }

            Long codigoEntidad = entidad != null ? entidad.getCodigo() : null;

            // --- Exter: genero, estadoCivil, profesion, provincia, canton, fechaNacimiento ---
            String genero         = null;
            String estadoCivil    = null;
            String profesion      = null;
            String provincia      = null;
            String canton         = null;
            LocalDate fechaNac    = null;
            try {
                List<Exter> exters = exterService.selectByCedula(identificacion);
                if (exters != null && !exters.isEmpty()) {
                    Exter exter = exters.get(0);
                    genero      = exter.getGenero();
                    estadoCivil = exter.getEstadoCivil();
                    profesion   = exter.getProfesion();
                    provincia   = exter.getProvincia();
                    canton      = exter.getCanton();
                    if (exter.getFechaNacimiento() != null) {
                        fechaNac = exter.getFechaNacimiento().toLocalDate();
                    }
                }
            } catch (Throwable ex) {
                System.out.println("G45 WARN - No se encontro Exter para: " + identificacion);
            }

            // --- Direccion: parroquia.nombre ---
            String parroquia = null;
            if (codigoEntidad != null) {
                try {
                    List<Direccion> dirs = direccionService.selectByParent(codigoEntidad);
                    if (dirs != null && !dirs.isEmpty() && dirs.get(0).getParroquia() != null) {
                        parroquia = dirs.get(0).getParroquia().getNombre();
                    }
                } catch (Throwable ex) {
                    System.out.println("G45 WARN - No se encontro Direccion para entidad: " + codigoEntidad);
                }
            }

            // --- PerfilEconomico: patrimonio, origenIngresos ---
            Double patrimonio     = null;
            String origenIngresos = null;
            if (codigoEntidad != null) {
                try {
                    List<PerfilEconomico> perfiles = perfilEconomicoService.selectByEntidad(codigoEntidad);
                    if (perfiles != null && !perfiles.isEmpty()) {
                        PerfilEconomico perfil = perfiles.get(0);
                        patrimonio    = perfil.getPatrimonioNeto();
                        origenIngresos = perfil.getOrigenOtrosIngresos();
                    }
                } catch (Throwable ex) {
                    System.out.println("G45 WARN - No se encontro PerfilEconomico para entidad: " + codigoEntidad);
                }
            }

            // --- Participe: tipoParticipe, actividadEconomica ---
            String tipoParticipe       = null;
            String actividadEconomica  = null;
            if (codigoEntidad != null) {
                try {
                    List<Participe> participes = participeService.selectByEntidad(codigoEntidad);
                    if (participes != null && !participes.isEmpty()) {
                        Participe participe = participes.get(0);
                        if (participe.getTipoParticipante() != null) {
                            tipoParticipe = participe.getTipoParticipante().getNombre();
                        }
                        actividadEconomica = participe.getIngresoAdicionalActividad();
                    }
                } catch (Throwable ex) {
                    System.out.println("G45 WARN - No se encontro Participe para entidad: " + codigoEntidad);
                }
            }

            // --- Cargasfamiliares desde Entidad ---
            Long cargasFamiliares = entidad != null ? entidad.getCargasFamiliares() : null;

            // --- Construir y guardar CG45 ---
            NuevoParticipeG45 g45 = new NuevoParticipeG45();
            g45.setIdentificacion(identificacion);
            g45.setTipoIdentificacion(g41.getTipoIdentificacion());
            g45.setGenero(genero);
            g45.setEstadoCivil(estadoCivil);
            g45.setFechaNacimiento(fechaNac);
            g45.setProfesion(profesion);
            g45.setProvincia(provincia);
            g45.setCanton(canton);
            g45.setParroquia(parroquia);
            g45.setPatrimonio(patrimonio);
            g45.setOrigenIngresos(origenIngresos);
            g45.setTipoParticipe(tipoParticipe);
            g45.setActividadEconomica(actividadEconomica);
            g45.setCargasFamiliares(cargasFamiliares);
            g45.setDetalleEjecucion(detalle);

            cg45DaoService.save(g45, null);
            System.out.println("G45 INSERT nuevo participe: " + identificacion);
            contador++;
        }

        System.out.println("G45 generado con " + contador + " registros");
        return contador;
    }
}
