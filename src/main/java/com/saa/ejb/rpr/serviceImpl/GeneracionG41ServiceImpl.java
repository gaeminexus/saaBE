package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.ExterService;
import com.saa.ejb.crd.service.ParticipeService;
import com.saa.ejb.rpr.dao.ParticipeActivoG41DaoService;
import com.saa.ejb.rpr.service.GeneracionG41Service;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Exter;
import com.saa.model.crd.Participe;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.ParticipeActivoG41;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG41ServiceImpl implements GeneracionG41Service {

    private static final Long ESTADO_NUEVO            = 1L;
    private static final Long ESTADO_PROCESADO        = 10L;

    @EJB private EntidadService             entidadService;
    @EJB private ExterService               exterService;
    @EJB private ParticipeService           participeService;
    @EJB private ParticipeActivoG41DaoService cg41DaoService;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G41");

        // -------------------------------------------------------
        // 1. Buscar entidades con idEstado = 1
        // -------------------------------------------------------
        List<Entidad> entidades = entidadService.selectByIdEstado(ESTADO_NUEVO);
        System.out.println("Entidades con idEstado=1 encontradas: " + entidades.size());

        // -------------------------------------------------------
        // 2. Si no hay ninguna → G41 vacío, retornar 0
        // -------------------------------------------------------
        if (entidades.isEmpty()) {
            System.out.println("No hay entidades nuevas → G41 queda vacio y OK");
            return 0L;
        }

        // -------------------------------------------------------
        // 3. Por cada entidad → mapear a CG41
        // -------------------------------------------------------
        long contador = 0L;

        for (Entidad entidad : entidades) {
            System.out.println("Procesando entidad id: " + entidad.getCodigo()
                    + " cedula: " + entidad.getNumeroIdentificacion());

            // Buscar Participe asociado a esta entidad
            List<Participe> participes = participeService.selectByEntidad(entidad.getCodigo());

            // Buscar Exter por cedula para obtener datos personales
            Exter exter = null;
            List<Exter> exterList = exterService.selectByCedula(entidad.getNumeroIdentificacion());
            if (!exterList.isEmpty()) {
                exter = exterList.get(0);
            }

            // Armar el registro CG41
            ParticipeActivoG41 cg41 = new ParticipeActivoG41();
            cg41.setDetalleEjecucion(detalle);

            // Tipo de identificación — siempre "C"
            cg41.setTipoIdentificacion("C");

            // Identificación
            cg41.setIdentificacion(entidad.getNumeroIdentificacion());

            // Datos de Exter (genero, estado civil, fecha nacimiento)
            if (exter != null) {
                cg41.setGenero(exter.getGenero());
                cg41.setEstadoCivil(exter.getEstadoCivil());
                if (exter.getFechaNacimiento() != null) {
                    cg41.setFechaNacimiento(exter.getFechaNacimiento().toLocalDate());
                }
            }

            // Datos de Participe (fecha ingreso fondo)
            if (!participes.isEmpty()) {
                Participe participe = participes.get(0);
                if (participe.getFechaIngresoFondo() != null) {
                    cg41.setFechaIngreso(participe.getFechaIngresoFondo().toLocalDate());
                }
            }

            // Valores fijos requeridos por el reporte
            cg41.setEstadoParticipe("A");
            cg41.setTipoSistema("I");
            cg41.setBaseCalculoAportacion("S");
            cg41.setTipoRelacionLaboral("N");
            cg41.setEstadoRegistro("A");

            // Persistir en CG41
            cg41DaoService.save(cg41, null);
            System.out.println("Registro G41 insertado para cedula: " + entidad.getNumeroIdentificacion());

            // -------------------------------------------------------
            // 4. Actualizar idEstado de la entidad a 10 (Procesado)
            // -------------------------------------------------------
            entidad.setIdEstado(ESTADO_PROCESADO);
            entidadService.saveSingle(entidad);

            contador++;
        }

        System.out.println("G41 generado con " + contador + " registros");
        return contador;
    }
}
