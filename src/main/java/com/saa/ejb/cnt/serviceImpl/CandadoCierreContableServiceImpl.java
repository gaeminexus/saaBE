package com.saa.ejb.cnt.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.CandadoCierreContableService;
import com.saa.ejb.cnt.service.ControlCierreService;
import com.saa.ejb.cnt.service.ReglaCandadoCierre;
import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.cnt.service.dto.ExcepcionCandado;
import com.saa.ejb.cnt.service.dto.FallaCandado;
import com.saa.ejb.cnt.service.dto.ResultadoCandado;
import com.saa.ejb.rpr.service.ActualizacionEjecucionGService;
import com.saa.ejb.rpr.service.ReglaActualizacionG;
import com.saa.ejb.rpr.service.ValidacionEstructurasGService;
import com.saa.ejb.rpr.service.dto.ObservacionValidacionG;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** C51: ver {@link CandadoCierreContableService}. Solo lectura. */
@Stateless
@SuppressWarnings("unchecked")
public class CandadoCierreContableServiceImpl implements CandadoCierreContableService {

    /** Estado COMPLETO de RPR.EJRC (GeneracionReportesServiceImpl.EJRC_COMPLETO). */
    static final long EJRC_COMPLETO = 3L;

    /** Código del control del C50: lo cubre la condición (c); no se duplica ni se excusa. */
    static final String CONTROL_G_DESACTUALIZADAS = "G_DESACTUALIZADAS";

    @PersistenceContext
    EntityManager em;

    @EJB private ControlCierreService controlCierreService;
    @EJB private ValidacionEstructurasGService validacionService;
    @EJB private ActualizacionEjecucionGService actualizacionService;

    /**
     * REQUIRES_NEW (no NOT_SUPPORTED): es solo lectura y no debe compartir la transaccion del cierre. Si adentro algo lanza una RuntimeException (controlar, validar,
     * una consulta), solo esta transaccion queda marcada para rollback y el cierre sigue; con el candado apagado la nota de bitacora dice «no se pudo evaluar».
     * Se eligio REQUIRES_NEW porque controlar() llama a otros EJB que abren su propia transaccion y consultas JPA que conviene que corran dentro de una; el patron es el
     * mismo de SeguimientoNovedadServiceImpl.controlesCierre. Lo que lee ya esta confirmado: cerrar() no ha escrito nada cuando la llama.
     */
    @jakarta.ejb.TransactionAttribute(jakarta.ejb.TransactionAttributeType.REQUIRES_NEW)
    @Override
    public ResultadoCandado consultar(Long idEmpresa, Long anio, Long mes, List<ExcepcionCandado> excepciones) throws Throwable {
        System.out.println("CandadoCierreContableService.consultar - empresa: " + idEmpresa + " - " + anio + "-" + mes);
        List<FallaCandado> fallas = new ArrayList<FallaCandado>();

        // (a) semáforo del período: todo control en ERROR.
        for (ControlCierre c : controlCierreService.controlar(idEmpresa, anio, mes)) {
            if (ControlCierre.ERROR.equals(c.getEstado()) && !CONTROL_G_DESACTUALIZADAS.equals(c.getCodigo())) {
                fallas.add(new FallaCandado("CONTROL", c.getCodigo(), c.getDescripcion(), c.getValor(), c.getDetalle(), true));
            }
        }
        // (b) y (c): última ejecución de las G40-G51 del mes.
        fallasDeLasG(anio, mes, fallas);

        boolean bloquea;
        try {
            bloquea = ReglaCandadoCierre.aplicar(fallas, excepciones);
        } catch (IllegalArgumentException e) {
            throw new IncomeException("CIERRE_CANDADO_EXCEPCION: " + e.getMessage());
        }
        ResultadoCandado r = new ResultadoCandado();
        r.setIdEmpresa(idEmpresa);
        r.setAnio(anio);
        r.setMes(mes);
        r.setCandadoActivo(CandadoCierreContableService.activo());
        r.setBloquearia(bloquea);
        r.setFallas(fallas);
        return r;
    }

    private void fallasDeLasG(Long anio, Long mes, List<FallaCandado> fallas) throws Throwable {
        List<Object[]> filas = em.createNativeQuery(
                " SELECT EJRCCDGO, EJRCESTD FROM RPR.EJRC WHERE EJRCCDGO = (SELECT MAX(EJRCCDGO) FROM RPR.EJRC WHERE EJRCANOO = :anio AND EJRCMESS = :mes)")
                .setParameter("anio", anio).setParameter("mes", mes).getResultList();
        if (filas.isEmpty()) {
            fallas.add(new FallaCandado("G", "G_NO_GENERADAS", "Estructuras G40-G51 del mes", Double.valueOf(0),
                    "El mes " + anio + "-" + String.format("%02d", mes) + " no tiene ninguna ejecucion de las G40-G51 (RPR.EJRC).", false));
            return;
        }
        Long codigo = ((Number) filas.get(0)[0]).longValue();
        long estado = filas.get(0)[1] != null ? ((Number) filas.get(0)[1]).longValue() : 0L;
        // (b) completa y con la validación previa sin errores.
        if (estado != EJRC_COMPLETO) {
            fallas.add(new FallaCandado("G", "G_NO_COMPLETA", "Ultima ejecucion de las G40-G51 completa", Double.valueOf(estado),
                    "La ejecucion " + codigo + " esta en estado " + estado + " (3 = COMPLETO): hay reportes pendientes o con novedades.", false));
        }
        long errores = 0L;
        StringBuilder resumen = new StringBuilder();
        for (ObservacionValidacionG o : validacionService.validar(codigo)) {
            if (ObservacionValidacionG.ERROR.equals(o.getNivel())) {
                errores += o.getCantidad() != null ? o.getCantidad().longValue() : 1L;
                if (resumen.length() > 0) {
                    resumen.append(", ");
                }
                resumen.append(o.getEstructura()).append(" ").append(o.getRegla());
            }
        }
        if (errores > 0) {
            fallas.add(new FallaCandado("G", "G_VALIDACION_ERRORES", "Validacion previa de las G40-G51 sin errores", Double.valueOf(errores),
                    "La validacion previa de la ejecucion " + codigo + " tiene errores (la Superintendencia los rechaza): " + resumen, false));
        }
        // (c) vigente según el C50; SIN_DATO cuenta como no vigente.
        ReglaActualizacionG.Resultado vigencia = actualizacionService.evaluarUltima(anio, mes);
        if (vigencia == null || !ReglaActualizacionG.VIGENTE.equals(vigencia.getEstado())) {
            fallas.add(new FallaCandado("G", "G_NO_VIGENTE", "Ultima ejecucion de las G40-G51 vigente (C50)", null,
                    "La ejecucion " + codigo + " no esta VIGENTE: " + (vigencia != null ? vigencia.getEstado() + ". " + vigencia.getMotivo() : "sin dato."), false));
        }
    }
}
