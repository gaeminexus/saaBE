package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionReportesService;
import com.saa.ejb.rpr.service.GeneracionUnReporteService;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.EjecucionReporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionReportesServiceImpl implements GeneracionReportesService {

    // Estados EJRC
    private static final Long EJRC_EN_PROCESO    = 1L;
    private static final Long EJRC_CON_NOVEDADES = 2L;
    private static final Long EJRC_COMPLETO      = 3L;

    // Estados EJRD
    private static final Long EJRD_OK            = 1L;
    private static final Long EJRD_CON_NOVEDADES = 2L;
    private static final Long EJRD_PENDIENTE     = 3L;

    // Tipo ejecución EJRC
    private static final Long TIPO_INICIAL       = 1L;
    private static final Long TIPO_CORRECCION    = 2L;

    // Tipos de reporte
    private static final String[] TIPOS_REPORTE = {
        "G40", "G41", "G42", "G43", "G44", "G46", "G45",
        "G47", "G48", "G49", "G50", "G51"
    };

    @EJB private EjecucionReporteService        ejrcService;
    @EJB private DetalleEjecucionReporteService ejrdService;
    // 2026-09-08: ejecuta CADA reporte en su propia transacción (REQUIRES_NEW) — ver el
    // javadoc de la interfaz para por qué esto ya no puede ser un método privado de esta
    // misma clase. Reemplaza a los doce @EJB de GeneracionGxxService que vivían acá.
    @EJB private GeneracionUnReporteService     unReporteService;

    @Override
    public EjecucionReporte ejecutarGeneracion(Long mes, Long anio, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo ejecutarGeneracion con mes: " + mes + ", anio: " + anio + ", usuario: " + usuario);

        EjecucionReporte ejrc     = null;
        List<DetalleEjecucionReporte> ejrdsAProcesar = new ArrayList<>();
        boolean esCorreccion = false;

        // -------------------------------------------------------
        // 1. Buscar si ya existe una ejecución para ese mes/año
        // -------------------------------------------------------
        List<EjecucionReporte> ejecucionesExistentes = null;
        try {
            ejecucionesExistentes = ejrcService.selectByMesAnio(mes, anio);
        } catch (Throwable e) {
            // No existe ejecución previa → se crea nueva
            ejecucionesExistentes = new ArrayList<>();
        }

        if (!ejecucionesExistentes.isEmpty()) {
            ejrc = ejecucionesExistentes.get(0);
            System.out.println("Ejecucion existente encontrada con id: " + ejrc.getCodigo() + ", estado: " + ejrc.getEstado());

            // ---------------------------------------------------
            // 2. Si ya está completa → informar al frontend
            // ---------------------------------------------------
            if (EJRC_COMPLETO.equals(ejrc.getEstado())) {
                System.out.println("Todos los reportes ya fueron generados para mes: " + mes + " anio: " + anio);
                throw new Exception("Los reportes G40-G51 ya fueron generados correctamente para " + mes + "/" + anio
                        + ". Ejecucion id: " + ejrc.getCodigo());
            }

            // ---------------------------------------------------
            // 3. Obtener solo los EJRD pendientes o con novedades
            // ---------------------------------------------------
            ejrdsAProcesar = ejrdService.selectPendientesYNovedadesByEjecucion(ejrc.getCodigo());
            esCorreccion = true;

            // Marcar EJRC como en proceso nuevamente
            ejrc.setEstado(EJRC_EN_PROCESO);
            ejrcService.saveSingle(ejrc);

        } else {
            // ---------------------------------------------------
            // 4. Crear nueva cabecera EJRC
            // ---------------------------------------------------
            ejrc = new EjecucionReporte();
            ejrc.setMes(mes);
            ejrc.setAnio(anio);
            ejrc.setUsuario(usuario);
            ejrc.setFechaGeneracion(LocalDate.now());
            ejrc.setTipoEjecucion(TIPO_INICIAL);
            ejrc.setEstado(EJRC_EN_PROCESO);
            ejrc.setObservaciones("Ejecucion inicial generada automaticamente");
            ejrc = ejrcService.saveSingle(ejrc);
            System.out.println("EJRC creado con id: " + ejrc.getCodigo());

            // ---------------------------------------------------
            // 5. Crear los 12 EJRD en estado Pendiente
            // ---------------------------------------------------
            for (String tipoReporte : TIPOS_REPORTE) {
                DetalleEjecucionReporte ejrd = new DetalleEjecucionReporte();
                ejrd.setEjecucionReporte(ejrc);
                ejrd.setTipoReporte(tipoReporte);
                ejrd.setEstado(EJRD_PENDIENTE);
                ejrd = ejrdService.saveSingle(ejrd);
                ejrdsAProcesar.add(ejrd);
                System.out.println("EJRD creado para " + tipoReporte + " con id: " + ejrd.getCodigo());
            }
        }

        // -------------------------------------------------------
        // 6. Ejecutar la lógica de generación por cada EJRD
        // -------------------------------------------------------
        List<String> reportesConNovedades = new ArrayList<>();
        for (DetalleEjecucionReporte ejrd : ejrdsAProcesar) {
            System.out.println("Procesando reporte: " + ejrd.getTipoReporte());

            // Si es corrección, actualizar tipo ejecución del EJRC
            if (esCorreccion) {
                ejrc.setTipoEjecucion(TIPO_CORRECCION);
            }

            try {
                // 2026-09-08: generarUno y marcarResultado corren cada uno en su PROPIA
                // transacción (REQUIRES_NEW, ver GeneracionUnReporteService) — si este reporte
                // falla, su rollback no arrastra a los demás ni a lo que este orquestador ya
                // grabó. Este método NO vuelve a llamar ejrdService.saveSingle por su cuenta en
                // ningún caso: todo el grabado del EJRD pasa por marcarResultado.
                long cantidadRegistros = unReporteService.generarUno(ejrd);
                unReporteService.marcarResultado(ejrd.getCodigo(), EJRD_OK, cantidadRegistros, null);
                System.out.println("Reporte " + ejrd.getTipoReporte() + " generado OK con " + cantidadRegistros + " registros");

            } catch (Throwable e) {
                // El log va PRIMERO, antes de cualquier intento de grabar: si marcarResultado
                // también fallara, el log ya tiene el dato — antes el orden era al revés
                // (grabar, después loguear) y el error real se perdía si el grabado explotaba
                // (STATUS_MARKED_ROLLBACK tapando la causa real). e.printStackTrace() porque
                // getMessage() solo, en un STATUS_MARKED_ROLLBACK, no dice nada.
                String mensajeError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                System.out.println("Reporte " + ejrd.getTipoReporte() + " fallo: " + mensajeError);
                e.printStackTrace();
                reportesConNovedades.add(ejrd.getTipoReporte() + ": " + mensajeError);

                unReporteService.marcarResultado(ejrd.getCodigo(), EJRD_CON_NOVEDADES, null, "ERROR: " + mensajeError);
            }
        }

        // -------------------------------------------------------
        // 7. Evaluar estado final del EJRC
        // -------------------------------------------------------
        // 2026-09-08: NO se re-consulta ejrdService.selectPendientesYNovedadesByEjecucion acá
        // a propósito. Este orquestador y ejrdService comparten el MISMO persistence context
        // transaction-scoped (los dos son @Stateless REQUIRED, misma transacción JTA) — los
        // EJRD de ejrdsAProcesar ya quedaron cacheados en ese contexto desde el paso 3/5, y
        // una consulta JPQL nueva sobre las MISMAS filas le devuelve al ORM las instancias YA
        // MANAGED (con su estado de ANTES del bucle), no lo que las transacciones REQUIRES_NEW
        // de arriba acaban de confirmar en la base — exactamente la trampa de "copias viejas
        // en el contexto de persistencia" que pisarían el resultado. reportesConNovedades ya
        // tiene, en memoria y sin ese riesgo, la lista completa y correcta de lo que falló en
        // ESTA corrida — es autoritativa: todo EJRD que no entró en ejrdsAProcesar ya estaba
        // OK antes de esta llamada y sigue igual.
        if (reportesConNovedades.isEmpty()) {
            ejrc.setEstado(EJRC_COMPLETO);
            ejrc.setObservaciones("Todos los reportes G40-G51 generados correctamente");
        } else {
            ejrc.setEstado(EJRC_CON_NOVEDADES);
            // Con nombre y mensaje — antes decía "N reporte(s) con novedades: ver detalle en
            // EJRD" y obligaba a ir a buscar a otra tabla para saber qué pasó. Truncado
            // defensivo (2026-09-08): con dos o tres reportes fallando el texto armado supera
            // fácil el límite de la columna y el saveSingle de más abajo revienta con
            // ORA-12899 — el mismo problema que este cambio vino a arreglar, un escalón más
            // allá. El prefijo con la cantidad va PRIMERO a propósito: es el dato que no se
            // puede perder, y al truncar desde el final siempre sobrevive.
            String observaciones = reportesConNovedades.size() + " reporte(s) con novedades: "
                    + String.join("; ", reportesConNovedades);
            ejrc.setObservaciones(truncar(observaciones, LIMITE_OBSERVACIONES_EJRC));
        }

        ejrc = ejrcService.saveSingle(ejrc);
        System.out.println("Ejecucion finalizada con estado: " + ejrc.getEstado());

        return ejrc;
    }

    /**
     * Límite de {@code EjecucionReporte.observaciones} — sale de {@code @Column(length = 500)}
     * en la entidad (columna {@code EJRCOBSR}). Si esa columna se agranda algún día, este
     * número también hay que tocarlo.
     */
    private static final int LIMITE_OBSERVACIONES_EJRC = 500;

    /**
     * Trunca {@code texto} a {@code limite} caracteres dejando el corte VISIBLE (termina en
     * "...", nunca a la mitad de una palabra sin avisar) — 2026-09-08, defensivo contra
     * ORA-12899 cuando varios reportes fallan a la vez y el mensaje concatenado supera la
     * columna. {@code null} o ya corto se devuelve tal cual.
     */
    private String truncar(String texto, int limite) {
        if (texto == null || texto.length() <= limite) {
            return texto;
        }
        return texto.substring(0, limite - 3) + "...";
    }
}
