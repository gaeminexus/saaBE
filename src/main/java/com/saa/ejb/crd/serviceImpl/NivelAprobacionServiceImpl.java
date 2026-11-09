package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.ejb.UsuarioDaoService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AprobacionPrestamoDaoService;
import com.saa.ejb.crd.dao.NivelAprobacionPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.ReglaAprobacionPrestamoDaoService;
import com.saa.ejb.crd.dao.UsuarioNivelAprobacionDaoService;
import com.saa.ejb.crd.service.NivelAprobacionService;
import com.saa.ejb.crd.service.dto.EstadoAprobacionPrestamo;
import com.saa.ejb.crd.service.dto.ReglaAprobacionCompleta;
import com.saa.ejb.crd.service.dto.ResultadoNivelAprobacion;
import com.saa.ejb.crd.service.dto.SimulacionReglaAprobacion;
import com.saa.model.crd.AprobacionPrestamo;
import com.saa.model.crd.NivelAprobacionPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.ReglaAprobacionPrestamo;
import com.saa.model.crd.UsuarioNivelAprobacion;
import com.saa.model.scp.Usuario;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de {@link NivelAprobacionService}. La resolución de la regla y la carga de sus niveles NO lanzan por una
 * regla de configuración: devuelven el motivo (una IncomeException que cruza un proxy EJB marca la transacción como
 * rollback aunque se atrape). Solo las operaciones que escriben (registrarAprobacion / registrarRechazo) lanzan, y lo hacen
 * antes de grabar nada.
 */
@Stateless
public class NivelAprobacionServiceImpl implements NivelAprobacionService {

    private static final long DECISION_APROBADO = 1L;
    private static final long DECISION_NEGADO = 2L;

    @EJB
    private ReglaAprobacionPrestamoDaoService reglaDao;

    @EJB
    private NivelAprobacionPrestamoDaoService nivelDao;

    @EJB
    private UsuarioNivelAprobacionDaoService usuarioNivelDao;

    @EJB
    private AprobacionPrestamoDaoService aprobacionDao;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private UsuarioDaoService usuarioDaoService;

    @EJB
    private com.saa.ejb.crd.service.SustitucionPrestamoService sustitucionPrestamoService;

    @EJB
    private com.saa.ejb.crd.service.CuentaBancariaParticipeService cuentaBancariaParticipeService;

    /** Resultado interno de resolver la regla: la regla, «no hay reglas activas» (transición) o el motivo del fallo. */
    private static final class Resolucion {
        private ReglaAprobacionPrestamo regla;
        private boolean sinReglasActivas;
        private String error;
    }

    /** Niveles de una regla ya cargados y validados, o el motivo. */
    private static final class Niveles {
        private ReglaAprobacionCompleta completa;
        private String error;
    }

    // ------------------------------------------------------------------ consulta

    @Override
    public ReglaAprobacionCompleta reglaCompleta(Long idRegla) throws Throwable {
        System.out.println("NivelAprobacionService.reglaCompleta - regla: " + idRegla);
        ReglaAprobacionPrestamo regla = idRegla != null ? reglaDao.find(new ReglaAprobacionPrestamo(), idRegla) : null;
        if (regla == null) {
            throw new IncomeException("NO_ENCONTRADO: no existe la regla " + idRegla);
        }
        return armar(regla);
    }

    @Override
    public SimulacionReglaAprobacion simular(Long idTipoPrestamo, Double monto, Long plazo, Long operacion)
            throws Throwable {
        System.out.println("NivelAprobacionService.simular - tipo: " + idTipoPrestamo + " - monto: " + monto
            + " - plazo: " + plazo + " - operación: " + operacion);
        SimulacionReglaAprobacion respuesta = new SimulacionReglaAprobacion();
        Long operacionEfectiva = operacion != null ? operacion : Long.valueOf(OPERACION_NUEVO);
        respuesta.setOperacion(operacionEfectiva);
        if (operacionEfectiva < OPERACION_NUEVO || operacionEfectiva > OPERACION_REESTRUCTURACION) {
            respuesta.setMensaje("La operación es 1 (nuevo), 2 (novación) o 3 (reestructuración)");
            return respuesta;
        }
        Resolucion resolucion = resolverPorDatos(idTipoPrestamo, monto, plazo, operacionEfectiva);
        respuesta.setSinReglasActivas(resolucion.sinReglasActivas);
        if (resolucion.sinReglasActivas) {
            respuesta.setMensaje("No hay ninguna regla de aprobación activa: el préstamo se aprueba en un solo paso, como hoy.");
            return respuesta;
        }
        if (resolucion.error != null) {
            respuesta.setMensaje(resolucion.error);
            return respuesta;
        }
        Niveles niveles = cargarNiveles(resolucion.regla);
        respuesta.setRegla(niveles.completa);
        respuesta.setMensaje(niveles.error);
        return respuesta;
    }

    @Override
    public EstadoAprobacionPrestamo estadoAprobacion(Long idPrestamo, Long idUsuario) throws Throwable {
        System.out.println("NivelAprobacionService.estadoAprobacion - préstamo: " + idPrestamo + " - usuario: " + idUsuario);
        Prestamo prestamo = idPrestamo != null ? prestamoDaoService.find(new Prestamo(), idPrestamo) : null;
        if (prestamo == null) {
            throw new IncomeException("PRESTAMO_NO_ENCONTRADO: no existe el préstamo " + idPrestamo);
        }
        EstadoAprobacionPrestamo estado = new EstadoAprobacionPrestamo();
        // Aviso previo: la orden de desembolso (préstamo nuevo y novación; no la reestructuración) necesita cuenta de destino.
        if (prestamo.getEntidad() != null && !sustitucionPrestamoService.esReestructuracion(prestamo)) {
            estado.setMotivoSinCuentaDestino(
                cuentaBancariaParticipeService.motivoSinCuentaActivaUnica(prestamo.getEntidad().getCodigo()));
        }
        Resolucion resolucion = resolver(prestamo);
        if (resolucion.sinReglasActivas) {
            estado.setSinNiveles(true);
            estado.setMensaje("No hay reglas de aprobación activas: se aprueba en un solo paso.");
            return estado;
        }
        if (resolucion.error != null) {
            estado.setMensaje(resolucion.error);
            return estado;
        }
        Niveles niveles = cargarNiveles(resolucion.regla);
        EstadoAprobacionPrestamo.ReglaResumen resumen = new EstadoAprobacionPrestamo.ReglaResumen();
        resumen.setId(resolucion.regla.getCodigo());
        resumen.setNombre(resolucion.regla.getNombre());
        estado.setRegla(resumen);
        if (niveles.error != null) {
            estado.setMensaje(niveles.error);
            return estado;
        }
        List<AprobacionPrestamo> vigentes = aprobacionDao.selectVigentesByPrestamo(prestamo.getCodigo());
        boolean yaAprobo = false;
        for (ReglaAprobacionCompleta.Nivel nivel : niveles.completa.getNiveles()) {
            EstadoAprobacionPrestamo.NivelEstado fila = new EstadoAprobacionPrestamo.NivelEstado();
            fila.setIdNivel(nivel.getCodigo());
            fila.setOrden(nivel.getOrden());
            fila.setNombre(nivel.getNombre());
            for (ReglaAprobacionCompleta.UsuarioNivel usuario : nivel.getUsuarios()) {
                fila.getUsuarios().add(usuario.getNombre());
            }
            AprobacionPrestamo decision = decisionDe(vigentes, nivel.getOrden());
            if (decision != null) {
                fila.setAprobadoPor(decision.getUsuario());
                fila.setFecha(decision.getFecha());
                fila.setDecision(decision.getDecision());
            }
            estado.getNiveles().add(fila);
        }
        estado.setTotalNiveles(estado.getNiveles().size());
        ReglaAprobacionCompleta.Nivel pendiente = nivelPendiente(niveles.completa, vigentes);
        if (pendiente != null) {
            estado.setNivelPendiente(pendiente.getOrden());
            Long idEstado = prestamo.getIdEstado();
            boolean aprobable = idEstado != null && idEstado == EstadoPrestamo.GENERADO;
            boolean esDelNivel = false;
            for (ReglaAprobacionCompleta.UsuarioNivel usuario : pendiente.getUsuarios()) {
                if (usuario.getIdUsuario().equals(idUsuario)) {
                    esDelNivel = true;
                }
            }
            for (AprobacionPrestamo aprobacion : vigentes) {
                if (aprobacion.getDecision() != null && aprobacion.getDecision() == DECISION_APROBADO
                        && aprobacion.getIdUsuario().equals(idUsuario)) {
                    yaAprobo = true;
                }
            }
            estado.setPuedeAprobar(aprobable && esDelNivel && !yaAprobo);
        }
        return estado;
    }

    // ------------------------------------------------------------------ escritura

    @Override
    public ResultadoNivelAprobacion registrarAprobacion(Prestamo prestamo, Long idUsuario, String usuario,
            String observacion) throws Throwable {
        System.out.println("NivelAprobacionService.registrarAprobacion - préstamo: "
            + (prestamo != null ? prestamo.getCodigo() : null) + " - usuario: " + usuario);
        ResultadoNivelAprobacion resultado = new ResultadoNivelAprobacion();
        Resolucion resolucion = resolver(prestamo);
        if (resolucion.sinReglasActivas) {
            resultado.setConNiveles(false);
            return resultado;
        }
        if (resolucion.error != null) {
            throw new IncomeException(resolucion.error);
        }
        Niveles niveles = cargarNiveles(resolucion.regla);
        if (niveles.error != null) {
            throw new IncomeException(niveles.error);
        }
        List<AprobacionPrestamo> vigentes = aprobacionDao.selectVigentesByPrestamo(prestamo.getCodigo());
        ReglaAprobacionCompleta.Nivel pendiente = nivelPendiente(niveles.completa, vigentes);
        if (pendiente == null) {
            throw new IncomeException("Todos los niveles de la regla '" + resolucion.regla.getNombre()
                + "' ya aprobaron el préstamo " + prestamo.getCodigo());
        }
        // El usuario tiene que ser de ESE nivel.
        boolean esDelNivel = false;
        StringBuilder permitidos = new StringBuilder();
        for (ReglaAprobacionCompleta.UsuarioNivel habilitado : pendiente.getUsuarios()) {
            if (habilitado.getIdUsuario().equals(idUsuario)) {
                esDelNivel = true;
            }
            if (permitidos.length() > 0) {
                permitidos.append(", ");
            }
            permitidos.append(habilitado.getNombre());
        }
        if (!esDelNivel) {
            throw new IncomeException("El nivel " + pendiente.getOrden() + " (" + pendiente.getNombre()
                + ") lo aprueba: " + permitidos);
        }
        // Un mismo usuario no aprueba dos niveles del mismo préstamo.
        for (AprobacionPrestamo previa : vigentes) {
            if (previa.getDecision() != null && previa.getDecision() == DECISION_APROBADO
                    && previa.getIdUsuario().equals(idUsuario)) {
                throw new IncomeException("El usuario " + usuario + " ya aprobó el nivel " + previa.getOrden()
                    + " (" + previa.getNombreNivel() + ") de este préstamo; no puede aprobar dos niveles");
            }
        }
        grabar(prestamo, resolucion.regla, pendiente, idUsuario, usuario, DECISION_APROBADO, observacion);

        int total = niveles.completa.getNiveles().size();
        ReglaAprobacionCompleta.Nivel ultimo = niveles.completa.getNiveles().get(total - 1);
        boolean esUltimo = ultimo.getOrden().equals(pendiente.getOrden());
        resultado.setConNiveles(true);
        resultado.setEsUltimo(esUltimo);
        resultado.setOrdenAprobado(pendiente.getOrden());
        resultado.setNombreNivel(pendiente.getNombre());
        resultado.setTotalNiveles(total);
        resultado.setIdRegla(resolucion.regla.getCodigo());
        resultado.setUsuario(usuario);
        if (!esUltimo) {
            for (ReglaAprobacionCompleta.Nivel nivel : niveles.completa.getNiveles()) {
                if (nivel.getOrden() > pendiente.getOrden()) {
                    resultado.setSiguienteNivel(nivel.getNombre());
                    break;
                }
            }
        }
        return resultado;
    }

    @Override
    public void registrarRechazo(Prestamo prestamo, Long idUsuario, String usuario, String observacion) throws Throwable {
        System.out.println("NivelAprobacionService.registrarRechazo - préstamo: "
            + (prestamo != null ? prestamo.getCodigo() : null) + " - usuario: " + usuario);
        Resolucion resolucion = resolver(prestamo);
        if (resolucion.sinReglasActivas || resolucion.error != null || resolucion.regla == null) {
            return; // sin niveles aplicables: se rechaza como hoy
        }
        Niveles niveles = cargarNiveles(resolucion.regla);
        if (niveles.error != null) {
            return;
        }
        boolean esDeAlgunNivel = false;
        for (ReglaAprobacionCompleta.Nivel nivel : niveles.completa.getNiveles()) {
            for (ReglaAprobacionCompleta.UsuarioNivel habilitado : nivel.getUsuarios()) {
                if (habilitado.getIdUsuario().equals(idUsuario)) {
                    esDeAlgunNivel = true;
                }
            }
        }
        if (!esDeAlgunNivel) {
            throw new IncomeException("El usuario " + usuario + " no pertenece a ningún nivel de aprobación de la regla '"
                + resolucion.regla.getNombre() + "'; no puede negar este préstamo");
        }
        List<AprobacionPrestamo> vigentes = aprobacionDao.selectVigentesByPrestamo(prestamo.getCodigo());
        ReglaAprobacionCompleta.Nivel nivel = nivelPendiente(niveles.completa, vigentes);
        if (nivel == null) {
            nivel = niveles.completa.getNiveles().get(niveles.completa.getNiveles().size() - 1);
        }
        grabar(prestamo, resolucion.regla, nivel, idUsuario, usuario, DECISION_NEGADO, observacion);
    }

    @Override
    public int invalidarAprobaciones(Long idPrestamo, String motivo) throws Throwable {
        System.out.println("NivelAprobacionService.invalidarAprobaciones - préstamo: " + idPrestamo + " - motivo: " + motivo);
        int invalidadas = 0;
        for (AprobacionPrestamo aprobacion : aprobacionDao.selectVigentesByPrestamo(idPrestamo)) {
            aprobacion.setEstado(Long.valueOf(Estado.INACTIVO));
            aprobacion.setUsuarioModificacion(motivo);
            aprobacion.setFechaModificacion(LocalDateTime.now());
            aprobacionDao.save(aprobacion, aprobacion.getCodigo());
            invalidadas++;
        }
        return invalidadas;
    }

    // ------------------------------------------------------------------ helpers

    private void grabar(Prestamo prestamo, ReglaAprobacionPrestamo regla, ReglaAprobacionCompleta.Nivel nivel,
            Long idUsuario, String usuario, long decision, String observacion) throws Throwable {
        AprobacionPrestamo aprobacion = new AprobacionPrestamo();
        aprobacion.setIdPrestamo(prestamo.getCodigo());
        aprobacion.setIdRegla(regla.getCodigo());
        aprobacion.setIdNivel(nivel.getCodigo());
        aprobacion.setOrden(nivel.getOrden());
        aprobacion.setNombreNivel(nivel.getNombre());
        aprobacion.setIdUsuario(idUsuario);
        aprobacion.setUsuario(usuario);
        aprobacion.setDecision(decision);
        aprobacion.setFecha(LocalDateTime.now());
        aprobacion.setObservacion(observacion != null && observacion.length() > 1000
            ? observacion.substring(0, 1000) : observacion);
        aprobacion.setEstado(Long.valueOf(Estado.ACTIVO));
        aprobacionDao.save(aprobacion, null);
    }

    /** La regla del préstamo: la de sus aprobaciones vigentes (no se re-resuelve) o la que dicen sus datos. */
    private Resolucion resolver(Prestamo prestamo) throws Throwable {
        List<AprobacionPrestamo> vigentes = aprobacionDao.selectVigentesByPrestamo(prestamo.getCodigo());
        if (!vigentes.isEmpty()) {
            Resolucion resolucion = new Resolucion();
            resolucion.regla = reglaDao.find(new ReglaAprobacionPrestamo(), vigentes.get(0).getIdRegla());
            if (resolucion.regla == null) {
                resolucion.error = "La regla " + vigentes.get(0).getIdRegla() + " de las aprobaciones del préstamo ya no existe";
            }
            return resolucion;
        }
        Long idTipo = prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
            ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
        Long plazo = prestamo.getPlazo() != null ? Long.valueOf(prestamo.getPlazo().longValue()) : null;
        return resolverPorDatos(idTipo, prestamo.getMontoSolicitado(), plazo, operacionDe(prestamo));
    }

    /** Operaciones de RGAP.RGAPTPOP. */
    static final int OPERACION_NUEVO = 1;
    static final int OPERACION_NOVACION = 2;
    static final int OPERACION_REESTRUCTURACION = 3;

    /**
     * Operación del préstamo: sin original → NUEVO; con original → REESTRUCTURACIÓN o NOVACIÓN con la MISMA lógica que
     * la sustitución real ({@code SustitucionPrestamoService.esReestructuracion}: PRSTRSTR = 1 o producto RESTRUCTURADO).
     */
    private Long operacionDe(Prestamo prestamo) throws Throwable {
        if (prestamo.getIdPrestamoOriginal() == null) {
            return Long.valueOf(OPERACION_NUEVO);
        }
        return Long.valueOf(sustitucionPrestamoService.esReestructuracion(prestamo)
            ? OPERACION_REESTRUCTURACION : OPERACION_NOVACION);
    }

    private Resolucion resolverPorDatos(Long idTipo, Double monto, Long plazo, Long operacion) throws Throwable {
        Resolucion resolucion = new Resolucion();
        List<ReglaAprobacionPrestamo> activas = reglaDao.selectActivas();
        if (activas.isEmpty()) {
            resolucion.sinReglasActivas = true;
            return resolucion;
        }
        List<ReglaAprobacionPrestamo> aplican = new ArrayList<>();
        for (ReglaAprobacionPrestamo regla : activas) {
            boolean tipoOk = regla.getIdTipoPrestamo() == null || regla.getIdTipoPrestamo().equals(idTipo);
            boolean montoOk = (regla.getMontoMinimo() == null || (monto != null && monto >= regla.getMontoMinimo()))
                && (regla.getMontoMaximo() == null || (monto != null && monto <= regla.getMontoMaximo()));
            boolean plazoOk = (regla.getPlazoMinimo() == null || (plazo != null && plazo >= regla.getPlazoMinimo()))
                && (regla.getPlazoMaximo() == null || (plazo != null && plazo <= regla.getPlazoMaximo()));
            boolean operacionOk = regla.getOperacion() == null || regla.getOperacion().equals(operacion);
            if (tipoOk && montoOk && plazoOk && operacionOk) {
                aplican.add(regla);
            }
        }
        if (aplican.isEmpty()) {
            resolucion.error = "No hay regla de aprobación para tipo " + idTipo + ", monto " + monto + ", plazo " + plazo
                + ", operación " + operacion + " (1 nuevo, 2 novación, 3 reestructuración)";
            return resolucion;
        }
        long menor = Long.MAX_VALUE;
        for (ReglaAprobacionPrestamo regla : aplican) {
            menor = Math.min(menor, regla.getPrioridad() != null ? regla.getPrioridad() : 100L);
        }
        List<ReglaAprobacionPrestamo> ganadoras = new ArrayList<>();
        for (ReglaAprobacionPrestamo regla : aplican) {
            if ((regla.getPrioridad() != null ? regla.getPrioridad() : 100L) == menor) {
                ganadoras.add(regla);
            }
        }
        if (ganadoras.size() > 1) {
            resolucion.error = "Configuración de niveles ambigua: las reglas " + ganadoras.get(0).getCodigo() + " y "
                + ganadoras.get(1).getCodigo() + " aplican a este préstamo con la misma prioridad";
            return resolucion;
        }
        resolucion.regla = ganadoras.get(0);
        return resolucion;
    }

    /** Carga los niveles y usuarios activos de la regla; el motivo si faltan niveles o algún nivel no tiene usuarios. */
    private Niveles cargarNiveles(ReglaAprobacionPrestamo regla) throws Throwable {
        Niveles resultado = new Niveles();
        resultado.completa = armar(regla);
        if (resultado.completa.getNiveles().isEmpty()) {
            resultado.error = "La regla '" + regla.getNombre() + "' no tiene niveles activos";
            return resultado;
        }
        for (ReglaAprobacionCompleta.Nivel nivel : resultado.completa.getNiveles()) {
            if (nivel.getUsuarios().isEmpty()) {
                resultado.error = "El nivel " + nivel.getOrden() + " (" + nivel.getNombre() + ") de la regla '"
                    + regla.getNombre() + "' no tiene usuarios activos";
                return resultado;
            }
        }
        return resultado;
    }

    private ReglaAprobacionCompleta armar(ReglaAprobacionPrestamo regla) throws Throwable {
        ReglaAprobacionCompleta completa = new ReglaAprobacionCompleta();
        completa.setRegla(regla);
        for (NivelAprobacionPrestamo nivel : nivelDao.selectActivosByRegla(regla.getCodigo())) {
            ReglaAprobacionCompleta.Nivel fila = new ReglaAprobacionCompleta.Nivel();
            fila.setCodigo(nivel.getCodigo());
            fila.setOrden(nivel.getOrden());
            fila.setNombre(nivel.getNombre());
            for (UsuarioNivelAprobacion habilitado : usuarioNivelDao.selectActivosByNivel(nivel.getCodigo())) {
                ReglaAprobacionCompleta.UsuarioNivel usuario = new ReglaAprobacionCompleta.UsuarioNivel();
                usuario.setCodigo(habilitado.getCodigo());
                usuario.setIdUsuario(habilitado.getIdUsuario());
                usuario.setNombre(nombreUsuario(habilitado.getIdUsuario()));
                fila.getUsuarios().add(usuario);
            }
            completa.getNiveles().add(fila);
        }
        return completa;
    }

    private String nombreUsuario(Long idUsuario) throws Throwable {
        Usuario usuario = idUsuario != null ? usuarioDaoService.find(new Usuario(), idUsuario) : null;
        return usuario != null && usuario.getNombre() != null ? usuario.getNombre() : "usuario " + idUsuario;
    }

    /** El primer nivel (por orden) que todavía no tiene una aprobación vigente. */
    private ReglaAprobacionCompleta.Nivel nivelPendiente(ReglaAprobacionCompleta completa,
            List<AprobacionPrestamo> vigentes) {
        for (ReglaAprobacionCompleta.Nivel nivel : completa.getNiveles()) {
            AprobacionPrestamo decision = decisionDe(vigentes, nivel.getOrden());
            if (decision == null || decision.getDecision() == null || decision.getDecision() != DECISION_APROBADO) {
                return nivel;
            }
        }
        return null;
    }

    private AprobacionPrestamo decisionDe(List<AprobacionPrestamo> vigentes, Long orden) {
        for (AprobacionPrestamo aprobacion : vigentes) {
            if (aprobacion.getOrden() != null && aprobacion.getOrden().equals(orden)) {
                return aprobacion;
            }
        }
        return null;
    }
}
