package com.saa.ejb.historico.serviceImpl;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.historico.service.CargaHistoricaNominaPasoService;
import com.saa.ejb.historico.service.CargaHistoricaNominaPasoService.Contexto;
import com.saa.ejb.historico.service.CargaHistoricaNominaService;
import com.saa.model.historico.LoteHistoricoNomina;
import com.saa.model.historico.ResultadoCargaHistorica;
import com.saa.model.historico.ResultadoCargaHistorica.ItemResultado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Orquestador de la carga histórica de nómina (C13). Corre SIN transacción: cada paso se ejecuta en la transacción
 * propia de {@link CargaHistoricaNominaPasoService} (REQUIRES_NEW), en este orden, porque cada uno necesita lo que el
 * anterior dejó confirmado en la base: empleados → período (nóminas, renglones, provisiones; queda APROBADO) → asiento
 * de rol → asiento de provisiones → pagos (período PAGADO) → planillas IESS → cierre (acumulados).
 * Si el período no se puede crear, los pasos siguientes no se intentan.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CargaHistoricaNominaServiceImpl implements CargaHistoricaNominaService {

    /** Mismo usuario de carga que C11 (SCP.PJRQ 38). */
    static final Long ID_USUARIO_CARGA_HISTORICA = CargaHistoricaCxcCxpServiceImpl.ID_USUARIO_CARGA_HISTORICA;

    @EJB
    private CargaHistoricaNominaPasoService pasos;

    @Override
    public ResultadoCargaHistorica cargar(LoteHistoricoNomina lote) throws Throwable {
        System.out.println("Ingresa al metodo cargar de CargaHistoricaNominaService, mes: " + (lote != null ? lote.mes : null));
        if (lote == null) throw new IncomeException("No se recibio el lote.");
        ResultadoCargaHistorica resultado = new ResultadoCargaHistorica();
        resultado.mes = lote.mes;
        resultado.soloValidar = lote.soloValidar;

        Contexto ctx = new Contexto();
        ctx.idEmpresa = lote.idEmpresa;
        if (lote.idUsuario == null) lote.idUsuario = ID_USUARIO_CARGA_HISTORICA;
        ctx.idUsuario = lote.idUsuario;
        ctx.usuario = lote.usuario != null && !lote.usuario.trim().isEmpty() ? lote.usuario.trim() : null;
        ctx.mes = lote.mes;
        ctx.soloValidar = lote.soloValidar;
        ctx.aceptarInferidos = lote.aceptarInferidos;
        ctx.centroCosto = lote.centroCosto;
        String etiqueta = lote.etiqueta != null && !lote.etiqueta.trim().isEmpty() ? lote.etiqueta.trim() : "sin etiqueta";
        ctx.observacion = lote.observacion != null && !lote.observacion.trim().isEmpty() ? lote.observacion.trim()
                : CargaHistoricaNominaPasoServiceImpl.PREFIJO_OBSERVACION + etiqueta + "] mes " + lote.mes;

        if (lote.idEmpresa == null) resultado.erroresLote.add("idEmpresa es obligatorio");
        if (lote.mes == null || !lote.mes.matches("\\d{4}-\\d{2}")) resultado.erroresLote.add("mes debe venir como AAAA-MM");
        if (lote.periodo == null) resultado.erroresLote.add("falta el bloque periodo");
        if (resultado.erroresLote.isEmpty()) {
            try {
                List<String> errores = pasos.validarLote(ctx, lote);
                resultado.erroresLote.addAll(errores);
            } catch (Throwable t) {
                resultado.erroresLote.add("Error validando el lote: " + causaRaiz(t));
            }
        }
        if (!resultado.erroresLote.isEmpty()) {
            resultado.exito = false;
            return resultado;
        }

        for (LoteHistoricoNomina.EmpleadoLote e : lote.empleados)
            procesar(resultado, new ItemResultado("empleado", e.ref, null), () -> pasos.cargarEmpleado(ctx, e));

        ItemResultado periodo = procesar(resultado, new ItemResultado("periodo", lote.mes, null), () -> pasos.cargarPeriodo(ctx, lote));
        Long idPeriodo = periodo.id;
        boolean seguir = ctx.soloValidar || (idPeriodo != null && !ResultadoCargaHistorica.RECHAZADO.equals(periodo.estado));
        if (!seguir) {
            resultado.avisosLote.add("El periodo no se creo: no se intentan asientos, pagos, planillas ni cierre.");
        } else {
            procesar(resultado, new ItemResultado("asiento-rol", lote.mes, null), () -> pasos.contabilizarRol(ctx, idPeriodo));
            procesar(resultado, new ItemResultado("asiento-provisiones", lote.mes, null), () -> pasos.contabilizarProvisiones(ctx, idPeriodo));
            for (LoteHistoricoNomina.PagoLote p : lote.pagos)
                procesar(resultado, new ItemResultado("pago", p.ref, p.fecha), () -> pasos.cargarPago(ctx, idPeriodo, p));
            for (LoteHistoricoNomina.PlanillaIessLote p : lote.planillasIess)
                procesar(resultado, new ItemResultado("planilla-iess", p.ref, p.numeroComprobante), () -> pasos.cargarPlanillaIess(ctx, idPeriodo, p));
            // El cierre no tumba el lote: si el motor lo bloquea (novedades IESS, pagos pendientes) queda como aviso.
            ItemResultado cierre = new ItemResultado("cierre", lote.mes, null);
            try {
                cierre = pasos.cerrarPeriodo(ctx, idPeriodo);
            } catch (Throwable t) {
                cierre = cierre.omitido(idPeriodo, "no se cerro el periodo: " + causaRaiz(t));
                resultado.avisosLote.add("Cierre del periodo " + idPeriodo + " pendiente: " + causaRaiz(t));
            }
            resultado.agregar(cierre);
        }

        resultado.exito = resultado.rechazados == 0;
        System.out.println("Carga historica de nomina " + lote.mes + (lote.soloValidar ? " (solo validar)" : "") + ": creados " + resultado.creados
                + ", omitidos " + resultado.omitidos + ", rechazados " + resultado.rechazados + ", validados " + resultado.validados);
        return resultado;
    }

    @FunctionalInterface
    private interface Paso {
        ItemResultado ejecutar() throws Throwable;
    }

    private ItemResultado procesar(ResultadoCargaHistorica resultado, ItemResultado porDefecto, Paso paso) {
        ItemResultado item;
        try {
            item = paso.ejecutar();
            if (item == null) item = porDefecto.rechazado("el paso no devolvio resultado");
        } catch (Throwable t) {
            item = porDefecto.rechazado(causaRaiz(t));
            System.err.println("Carga historica de nomina " + porDefecto.tipo + " " + porDefecto.ref + ": " + item.mensaje);
        }
        resultado.agregar(item);
        return item;
    }

    static String causaRaiz(Throwable t) {
        String mensaje = null;
        Throwable actual = t;
        int guarda = 0;
        while (actual != null && guarda++ < 20) {
            if (actual.getMessage() != null && !actual.getMessage().trim().isEmpty()) mensaje = actual.getMessage();
            actual = actual.getCause();
        }
        return mensaje != null ? mensaje : t.getClass().getName();
    }
}
