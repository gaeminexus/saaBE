package com.saa.ejb.historico.serviceImpl;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.historico.service.CargaHistoricaCxcCxpService;
import com.saa.ejb.historico.service.CargaHistoricaDocumentoService;
import com.saa.ejb.historico.service.CargaHistoricaDocumentoService.Contexto;
import com.saa.model.historico.LoteHistoricoCxcCxp;
import com.saa.model.historico.ResultadoCargaHistorica;
import com.saa.model.historico.ResultadoCargaHistorica.ItemResultado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Orquestador de la carga histórica de CxC/CxP (C11). Corre SIN transacción: cada documento se procesa en la
 * transacción propia de {@link CargaHistoricaDocumentoService} (REQUIRES_NEW). Orden: titulares → compras →
 * retenciones emitidas → pagos → ventas → retenciones recibidas → cobros → NC de compra → NC de venta, porque
 * cada paso necesita que el anterior esté confirmado en la base (la retención cruza contra la factura, el pago
 * contra el saldo que deja la retención, etc.).
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CargaHistoricaCxcCxpServiceImpl implements CargaHistoricaCxcCxpService {

    /** Usuario del SAA con el que se graban las cargas históricas (SCP.PJRQ 38, "usuario 1"); decisión del coordinador, 28-oct-2026. */
    static final Long ID_USUARIO_CARGA_HISTORICA = 38L;

    @EJB
    private CargaHistoricaDocumentoService documentos;

    @Override
    public ResultadoCargaHistorica cargar(LoteHistoricoCxcCxp lote) throws Throwable {
        System.out.println("Ingresa al metodo cargar de CargaHistoricaCxcCxpService, mes: " + (lote != null ? lote.mes : null));
        if (lote == null) throw new IncomeException("No se recibio el lote.");
        ResultadoCargaHistorica resultado = new ResultadoCargaHistorica();
        resultado.mes = lote.mes;
        resultado.soloValidar = lote.soloValidar;

        Contexto ctx = new Contexto();
        ctx.idEmpresa = lote.idEmpresa;
        if (lote.idUsuario == null) lote.idUsuario = ID_USUARIO_CARGA_HISTORICA;
        ctx.idUsuario = lote.idUsuario;
        ctx.usuario = lote.usuario != null && !lote.usuario.trim().isEmpty() ? lote.usuario.trim() : null; // si viene vacio, validarLote pone el nombre del usuario idUsuario
        ctx.mes = lote.mes;
        ctx.soloValidar = lote.soloValidar;
        ctx.aceptarInferidos = lote.aceptarInferidos;
        String etiqueta = lote.etiqueta != null && !lote.etiqueta.trim().isEmpty() ? lote.etiqueta.trim() : "sin etiqueta";
        ctx.observacion = lote.observacion != null && !lote.observacion.trim().isEmpty() ? lote.observacion.trim()
                : CargaHistoricaDocumentoServiceImpl.PREFIJO_OBSERVACION + etiqueta + "] mes " + lote.mes;

        if (lote.idEmpresa == null) resultado.erroresLote.add("idEmpresa es obligatorio");
        if (lote.mes == null || !lote.mes.matches("\\d{4}-\\d{2}")) resultado.erroresLote.add("mes debe venir como AAAA-MM");
        if (resultado.erroresLote.isEmpty()) {
            try {
                List<String> errores = documentos.validarLote(ctx, lote);
                resultado.erroresLote.addAll(errores);
            } catch (Throwable t) {
                resultado.erroresLote.add("Error validando el lote: " + causaRaiz(t));
            }
        }
        if (!resultado.erroresLote.isEmpty()) {
            resultado.exito = false;
            return resultado;
        }

        for (LoteHistoricoCxcCxp.TitularLote t : lote.titulares)
            procesar(resultado, new ItemResultado("titular", t.identificacion, t.razonSocial), () -> documentos.cargarTitular(ctx, t));
        for (LoteHistoricoCxcCxp.CompraLote c : lote.compras)
            procesar(resultado, new ItemResultado("compra", c.ref, c.numero), () -> documentos.cargarCompra(ctx, c));
        for (LoteHistoricoCxcCxp.RetencionEmitidaLote r : lote.retencionesEmitidas)
            procesar(resultado, new ItemResultado("retencion-emitida", r.ref, r.numero), () -> documentos.cargarRetencionEmitida(ctx, r));
        for (LoteHistoricoCxcCxp.PagoLote p : lote.pagos)
            procesar(resultado, new ItemResultado("pago", p.ref, p.facturaNumero), () -> documentos.cargarPago(ctx, p));
        for (LoteHistoricoCxcCxp.VentaLote v : lote.ventas)
            procesar(resultado, new ItemResultado("venta", v.ref, v.numero), () -> documentos.cargarVenta(ctx, v));
        for (LoteHistoricoCxcCxp.RetencionRecibidaLote r : lote.retencionesRecibidas)
            procesar(resultado, new ItemResultado("retencion-recibida", r.ref, r.numero), () -> documentos.cargarRetencionRecibida(ctx, r));
        for (LoteHistoricoCxcCxp.CobroLote c : lote.cobros)
            procesar(resultado, new ItemResultado("cobro", c.ref, c.facturaNumero), () -> documentos.cargarCobro(ctx, c));
        for (LoteHistoricoCxcCxp.NotaCreditoCompraLote n : lote.ncCompras)
            procesar(resultado, new ItemResultado("nc-compra", n.ref, n.numero), () -> documentos.cargarNotaCreditoCompra(ctx, n));
        for (LoteHistoricoCxcCxp.NotaCreditoVentaLote n : lote.ncVentas)
            procesar(resultado, new ItemResultado("nc-venta", n.ref, n.numero), () -> documentos.cargarNotaCreditoVenta(ctx, n));

        resultado.exito = resultado.rechazados == 0;
        System.out.println("Carga historica " + lote.mes + (lote.soloValidar ? " (solo validar)" : "") + ": creados " + resultado.creados
                + ", omitidos " + resultado.omitidos + ", rechazados " + resultado.rechazados + ", validados " + resultado.validados);
        return resultado;
    }

    @FunctionalInterface
    private interface Paso {
        ItemResultado ejecutar() throws Throwable;
    }

    /** Ejecuta un documento en su transacción; cualquier excepción se vuelve RECHAZADO con la causa raíz. */
    private void procesar(ResultadoCargaHistorica resultado, ItemResultado porDefecto, Paso paso) {
        ItemResultado item;
        try {
            item = paso.ejecutar();
            if (item == null) item = porDefecto.rechazado("el paso no devolvio resultado");
        } catch (Throwable t) {
            item = porDefecto.rechazado(causaRaiz(t));
            System.err.println("✗ Carga historica " + porDefecto.tipo + " " + porDefecto.ref + ": " + item.mensaje);
        }
        resultado.agregar(item);
    }

    private static String causaRaiz(Throwable t) {
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
