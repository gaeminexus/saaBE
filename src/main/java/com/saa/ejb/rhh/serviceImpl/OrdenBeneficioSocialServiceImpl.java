package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.rhh.dao.ConceptoNominaDaoService;
import com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.NovedadNominaDaoService;
import com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.service.OrdenBeneficioSocialService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.LiquidacionBeneficioSocial;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.NovedadNomina;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.OrdenBeneficioSocial;
import com.saa.model.rhh.OrdenBeneficioSocialResumen;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ReglonNomina;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RhhEstadoOrdenBeneficio;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhRolConceptoMotor;
import com.saa.rubros.RhhTipoBeneficioSocial;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * Implementacion de OrdenBeneficioSocialService.
 */
@Stateless
public class OrdenBeneficioSocialServiceImpl implements OrdenBeneficioSocialService {

    /** Prefijo del numero de la orden: ODBS-{anio}-{secuencial de 4 digitos}. */
    private static final String PREFIJO_NUMERO = "ODBS-%d-%04d";

    /** Estado PAGADA de una liquidacion (LQBSESTD), lo asigna confirmarPago. */
    private static final long LQBS_PAGADA = 2L;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private OrdenBeneficioSocialDaoService ordenBeneficioSocialDaoService;

    @EJB
    private LiquidacionBeneficioSocialDaoService liquidacionBeneficioSocialDaoService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    @EJB
    private ContabilizacionNominaService contabilizacionNominaService;

    @EJB
    private ConceptoNominaDaoService conceptoNominaDaoService;

    @EJB
    private PeriodoNominaDaoService periodoNominaDaoService;

    @EJB
    private NovedadNominaDaoService novedadNominaDaoService;

    @EJB
    private AsientoService asientoService;

    // ===== INICIO decimo acumulado en periodo CALCULADO (equipo omen-saa-2, bloqueo urgente 2026-09-08) =====
    @EJB
    private NominaDaoService nominaDaoService;

    @EJB
    private ReglonNominaDaoService reglonNominaDaoService;
    // ===== FIN decimo acumulado en periodo CALCULADO =====

    // =====================================================================
    // EntityService — los seis de la casa
    // =====================================================================

    @Override
    public OrdenBeneficioSocial selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById OrdenBeneficioSocial con id: " + id);
        return ordenBeneficioSocialDaoService.selectById(id, NombreEntidadesRhh.ORDEN_BENEFICIO_SOCIAL);
    }

    @Override
    public List<OrdenBeneficioSocial> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll OrdenBeneficioSocialService");
        List<OrdenBeneficioSocial> result =
                ordenBeneficioSocialDaoService.selectAll(NombreEntidadesRhh.ORDEN_BENEFICIO_SOCIAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total OrdenBeneficioSocial no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<OrdenBeneficioSocial> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria OrdenBeneficioSocialService");
        List<OrdenBeneficioSocial> result = ordenBeneficioSocialDaoService
                .selectByCriteria(datos, NombreEntidadesRhh.ORDEN_BENEFICIO_SOCIAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio OrdenBeneficioSocial no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public OrdenBeneficioSocial saveSingle(OrdenBeneficioSocial entidad) throws Throwable {
        System.out.println("saveSingle - OrdenBeneficioSocial");
        return ordenBeneficioSocialDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public void save(List<OrdenBeneficioSocial> lista) throws Throwable {
        for (OrdenBeneficioSocial registro : lista) {
            saveSingle(registro);
        }
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        OrdenBeneficioSocial entidad = new OrdenBeneficioSocial();
        for (Long registro : id) {
            ordenBeneficioSocialDaoService.remove(entidad, registro);
        }
    }

    // =====================================================================
    // Ciclo de la orden
    // =====================================================================

    @Override
    public Map<String, Object> generar(Long idEmpresa, Long tipoBeneficio, Integer anio, Long region,
            String usuario) throws Throwable {
        System.out.println("=== generar orden de beneficio social | idEmpresa=" + idEmpresa
                + " | tipoBeneficio=" + tipoBeneficio + " | anio=" + anio + " | region=" + region + " ===");

        exigeTipoBeneficioValido(tipoBeneficio);
        exigeRegionCoherente(tipoBeneficio, region);
        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar idEmpresa.");
        }
        if (anio == null) {
            throw new IncomeException("Debe indicar el anio.");
        }

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();

        OrdenBeneficioSocial viva = ordenBeneficioSocialDaoService
                .selectOrdenVivaByCombinacion(idEmpresa, tipoBeneficio, anio, region);
        if (viva != null) {
            resultado.put("exito", Boolean.FALSE);
            resultado.put("idOrdenExistente", viva.getCodigo());
            resultado.put("mensaje", "Ya existe la orden " + viva.getCodigo() + " en estado "
                    + textoEstadoOrden(viva.getEstado()) + ".");
            return resultado;
        }

        List<LiquidacionBeneficioSocial> pendientes = liquidacionBeneficioSocialDaoService
                .selectPendientesByCombinacion(idEmpresa, tipoBeneficio, anio, region);
        if (pendientes == null || pendientes.isEmpty()) {
            resultado.put("exito", Boolean.FALSE);
            resultado.put("mensaje", "No hay liquidaciones pendientes de pago para "
                    + textoTipoBeneficio(tipoBeneficio) + " " + anio + ".");
            return resultado;
        }

        Empresa empresa = em.find(Empresa.class, idEmpresa);
        if (empresa == null) {
            throw new IncomeException("No existe la empresa " + idEmpresa + ".");
        }

        Double total = Double.valueOf(0D);
        for (LiquidacionBeneficioSocial liquidacion : pendientes) {
            total = RedondeoNomina.suma(total, liquidacion.getValor());
        }

        OrdenBeneficioSocial orden = new OrdenBeneficioSocial();
        orden.setEmpresa(empresa);
        orden.setTipoBeneficio(tipoBeneficio);
        orden.setAnio(anio);
        orden.setRegion(region);
        orden.setNumero(armaNumero(anio));
        orden.setFechaEmision(LocalDate.now());
        orden.setTotal(total);
        orden.setNumeroEmpleados(Integer.valueOf(pendientes.size()));
        orden.setEstado(Long.valueOf(RhhEstadoOrdenBeneficio.GENERADA));
        orden.setFechaRegistro(LocalDateTime.now());
        orden.setUsuarioRegistro(usuario);
        orden = ordenBeneficioSocialDaoService.save(orden, orden.getCodigo());
        em.flush();

        for (LiquidacionBeneficioSocial liquidacion : pendientes) {
            liquidacion.setOrdenBeneficioSocial(orden);
            liquidacionBeneficioSocialDaoService.save(liquidacion, liquidacion.getCodigo());
        }

        resultado.put("exito", Boolean.TRUE);
        resultado.put("idOrden", orden.getCodigo());
        resultado.put("numero", orden.getNumero());
        resultado.put("tipoBeneficio", orden.getTipoBeneficio());
        resultado.put("tipoBeneficioTexto", textoTipoBeneficio(orden.getTipoBeneficio()));
        resultado.put("anio", orden.getAnio());
        resultado.put("region", orden.getRegion());
        resultado.put("total", orden.getTotal());
        resultado.put("numeroEmpleados", orden.getNumeroEmpleados());
        resultado.put("estado", orden.getEstado());
        resultado.put("estadoTexto", textoEstadoOrden(orden.getEstado()));
        resultado.put("mensaje", "Orden generada con " + orden.getNumeroEmpleados() + " empleados.");
        System.out.println("✓ Orden de beneficio social generada: id=" + orden.getCodigo()
                + " | total=" + total + " | empleados=" + pendientes.size());
        return resultado;
    }

    @Override
    public Map<String, Object> detalle(Long idOrden) throws Throwable {
        System.out.println("=== detalle orden de beneficio social | idOrden=" + idOrden + " ===");

        OrdenBeneficioSocial orden = em.find(OrdenBeneficioSocial.class, idOrden);
        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        if (orden == null) {
            resultado.put("exito", Boolean.FALSE);
            resultado.put("mensaje", "No existe la orden " + idOrden + ".");
            return resultado;
        }

        PagoProgramado pago = orden.getPagoProgramado();

        resultado.put("idOrden", orden.getCodigo());
        resultado.put("numero", orden.getNumero());
        resultado.put("tipoBeneficio", orden.getTipoBeneficio());
        resultado.put("anio", orden.getAnio());
        resultado.put("total", orden.getTotal());
        resultado.put("numeroEmpleados", orden.getNumeroEmpleados());
        resultado.put("estado", orden.getEstado());
        resultado.put("estadoTexto", textoEstadoOrden(orden.getEstado()));
        resultado.put("idPagoProgramado", pago != null ? pago.getId() : null);
        resultado.put("estadoPago", pago != null ? pago.getEstado() : null);
        resultado.put("estadoPagoTexto", pago != null ? textoEstadoPago(pago.getEstado()) : null);
        resultado.put("fechaPago", orden.getFechaPago());
        resultado.put("idAsiento", orden.getAsiento());

        List<Map<String, Object>> detalle = new ArrayList<Map<String, Object>>();
        for (LiquidacionBeneficioSocial liquidacion : liquidacionBeneficioSocialDaoService.selectByOrden(idOrden)) {
            Map<String, Object> fila = new LinkedHashMap<String, Object>();
            fila.put("idLiquidacion", liquidacion.getCodigo());
            fila.put("idEmpleado", liquidacion.getEmpleado() != null ? liquidacion.getEmpleado().getCodigo() : null);
            fila.put("identificacion", liquidacion.getEmpleado() != null
                    ? liquidacion.getEmpleado().getIdentificacion() : null);
            fila.put("nombreEmpleado", nombreEmpleado(liquidacion));
            fila.put("fechaInicio", liquidacion.getFechaInicio());
            fila.put("fechaFin", liquidacion.getFechaFin());
            fila.put("baseCalculo", liquidacion.getBaseCalculo());
            fila.put("dias", liquidacion.getDias());
            fila.put("valor", liquidacion.getValor());
            fila.put("valorPagado", liquidacion.getValorPagado());
            fila.put("estado", liquidacion.getEstado());
            detalle.add(fila);
        }
        resultado.put("detalle", detalle);
        return resultado;
    }

    @Override
    public List<OrdenBeneficioSocialResumen> listar(Long idEmpresa, Integer anio, Long tipoBeneficio,
            Long estado) throws Throwable {
        System.out.println("=== listar ordenes de beneficio social | idEmpresa=" + idEmpresa
                + " | anio=" + anio + " | tipoBeneficio=" + tipoBeneficio + " | estado=" + estado + " ===");
        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar idEmpresa.");
        }
        List<OrdenBeneficioSocialResumen> filas = ordenBeneficioSocialDaoService
                .selectListado(idEmpresa, anio, tipoBeneficio, estado);
        for (OrdenBeneficioSocialResumen fila : filas) {
            fila.setTipoBeneficioTexto(textoTipoBeneficio(fila.getTipoBeneficio()));
            fila.setEstadoTexto(textoEstadoOrden(fila.getEstado()));
            fila.setEstadoPagoTexto(textoEstadoPago(fila.getEstadoPago()));
        }
        return filas;
    }

    @Override
    public Map<String, Object> enviarATesoreria(Long idOrden, Long idUsuario, String observacion)
            throws Throwable {
        System.out.println("=== enviarATesoreria orden de beneficio social | idOrden=" + idOrden
                + " | idUsuario=" + idUsuario + " ===");

        OrdenBeneficioSocial orden = em.find(OrdenBeneficioSocial.class, idOrden);
        if (orden == null) {
            throw new IncomeException("No existe la orden de beneficio social " + idOrden + ".");
        }
        if (!Long.valueOf(RhhEstadoOrdenBeneficio.GENERADA).equals(orden.getEstado())) {
            throw new IncomeException("La orden " + idOrden + " no esta GENERADA (esta "
                    + textoEstadoOrden(orden.getEstado()) + "): no se puede enviar a tesoreria.");
        }
        if (idUsuario == null) {
            throw new IncomeException("Falta idUsuario para registrar el pago en tesoreria.");
        }
        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
        if (idEmpresa == null) {
            throw new IncomeException("La orden " + idOrden + " no tiene empresa: sin ella no se puede"
                    + " registrar el pago en la bandeja de tesoreria.");
        }

        // Beneficiario informativo: la orden paga a muchos empleados a la vez, no a uno solo,
        // y sin desglose este registro no genera archivo de transferencia propio (D1). Mismo
        // criterio que el frente 2 de nomina (GeneracionOrdenPagoServiceImpl).
        BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
        beneficiario.setNombre("Beneficios sociales " + textoTipoBeneficio(orden.getTipoBeneficio())
                + " " + orden.getAnio() + " - " + orden.getNumeroEmpleados() + " empleado(s)");
        beneficiario.setIdentificacion(orden.getNumero());

        String obs = (observacion != null && !observacion.trim().isEmpty()) ? observacion.trim()
                : ("Beneficio social " + textoTipoBeneficio(orden.getTipoBeneficio()) + " " + orden.getAnio());

        Map<String, Object> resultadoPago = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.RHH_BENEFICIO_SOCIAL, orden.getCodigo(), idEmpresa,
                null, orden.getTotal(),
                orden.getFechaEmision() != null ? orden.getFechaEmision().toString() : null,
                beneficiario, null, obs, idUsuario, false, orden.getNumero());

        Long idPago = (Long) resultadoPago.get("pago");
        PagoProgramado pago = idPago != null ? em.find(PagoProgramado.class, idPago) : null;

        orden.setPagoProgramado(pago);
        orden.setEstado(Long.valueOf(RhhEstadoOrdenBeneficio.ENVIADA_A_TESORERIA));
        orden.setUsuarioRegistro(nombreUsuario(idUsuario));
        orden = ordenBeneficioSocialDaoService.save(orden, orden.getCodigo());

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("exito", Boolean.TRUE);
        resultado.put("idOrden", orden.getCodigo());
        resultado.put("idPagoProgramado", idPago);
        resultado.put("estadoPago", pago != null ? pago.getEstado() : null);
        resultado.put("estadoPagoTexto", pago != null ? textoEstadoPago(pago.getEstado()) : null);
        resultado.put("mensaje", "Orden enviada a tesorería. Queda pendiente de aprobación.");
        System.out.println("✓ Orden " + idOrden + " enviada a tesoreria | idPago=" + idPago);
        return resultado;
    }

    @Override
    public Map<String, Object> confirmarPago(Long idOrden, LocalDate fechaPago, String usuario)
            throws Throwable {
        System.out.println("=== confirmarPago orden de beneficio social | idOrden=" + idOrden
                + " | fechaPago=" + fechaPago + " ===");

        OrdenBeneficioSocial orden = em.find(OrdenBeneficioSocial.class, idOrden);
        if (orden == null) {
            throw new IncomeException("No existe la orden de beneficio social " + idOrden + ".");
        }
        if (!Long.valueOf(RhhEstadoOrdenBeneficio.ENVIADA_A_TESORERIA).equals(orden.getEstado())) {
            throw new IncomeException("La orden " + idOrden + " no esta ENVIADA_A_TESORERIA (esta "
                    + textoEstadoOrden(orden.getEstado()) + ").");
        }
        PagoProgramado pago = orden.getPagoProgramado();
        if (pago == null || pago.getEstado() == null
                || pago.getEstado().intValue() != EstadoPagoProgramado.CONFIRMADO) {
            throw new IncomeException("El pago " + (pago != null ? pago.getId() : "?")
                    + " no está CONFIRMADO en tesorería (estado actual: "
                    + (pago != null ? textoEstadoPago(pago.getEstado()) : "sin pago") + ").");
        }
        LocalDate fecha = fechaPago != null ? fechaPago : LocalDate.now();

        List<LiquidacionBeneficioSocial> liquidaciones = liquidacionBeneficioSocialDaoService
                .selectByOrden(idOrden);
        List<Long> idsEmpleados = new ArrayList<Long>();
        for (LiquidacionBeneficioSocial liquidacion : liquidaciones) {
            liquidacion.setValorPagado(liquidacion.getValor());
            liquidacion.setFechaPago(fecha);
            liquidacion.setEstado(Long.valueOf(LQBS_PAGADA));
            liquidacionBeneficioSocialDaoService.save(liquidacion, liquidacion.getCodigo());
            if (liquidacion.getEmpleado() != null) {
                idsEmpleados.add(liquidacion.getEmpleado().getCodigo());
            }
        }

        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
        Asiento asiento = contabilizacionNominaService.contabilizarBajaProvisionBeneficioSocial(
                idEmpresa, orden.getTipoBeneficio().intValue(), idsEmpleados, orden.getTotal(), fecha,
                "Pago " + textoTipoBeneficio(orden.getTipoBeneficio()) + " " + orden.getAnio()
                        + " orden " + orden.getNumero(),
                usuario);

        String advertenciaRecalculo = crearNovedadesDecimoAcumulado(orden, liquidaciones, fecha, usuario);

        orden.setEstado(Long.valueOf(RhhEstadoOrdenBeneficio.PAGADA));
        orden.setFechaPago(fecha);
        orden.setAsiento(asiento.getCodigo());
        orden.setUsuarioRegistro(usuario);
        orden = ordenBeneficioSocialDaoService.save(orden, orden.getCodigo());

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("exito", Boolean.TRUE);
        resultado.put("idOrden", orden.getCodigo());
        resultado.put("idAsiento", asiento.getCodigo());
        resultado.put("numeroAsiento", asiento.getNumeroAlterno());
        resultado.put("liquidacionesPagadas", Integer.valueOf(liquidaciones.size()));
        resultado.put("total", orden.getTotal());
        // ===== INICIO decimo acumulado en periodo CALCULADO (equipo omen-saa-2, bloqueo urgente 2026-09-08) =====
        // Si la novedad quedo en un periodo ya CALCULADO, el rol no la refleja hasta que se
        // recalcule -y eso tiene que decirlo la respuesta, no solo el log del servidor.
        String mensaje = "Pago confirmado y provisión dada de baja.";
        if (advertenciaRecalculo != null) {
            mensaje += " " + advertenciaRecalculo;
            resultado.put("advertencia", advertenciaRecalculo);
        }
        resultado.put("mensaje", mensaje);
        // ===== FIN decimo acumulado en periodo CALCULADO =====
        System.out.println("✓ Orden " + idOrden + " PAGADA | asiento=" + asiento.getCodigo()
                + " | liquidaciones=" + liquidaciones.size()
                + (advertenciaRecalculo != null ? " | ADVERTENCIA: " + advertenciaRecalculo : ""));
        return resultado;
    }

    @Override
    public Map<String, Object> anular(Long idOrden, String motivo, String usuario) throws Throwable {
        System.out.println("=== anular orden de beneficio social | idOrden=" + idOrden + " ===");

        OrdenBeneficioSocial orden = em.find(OrdenBeneficioSocial.class, idOrden);
        if (orden == null) {
            throw new IncomeException("No existe la orden de beneficio social " + idOrden + ".");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo de la anulación.");
        }
        if (Long.valueOf(RhhEstadoOrdenBeneficio.PAGADA).equals(orden.getEstado())) {
            PagoProgramado pagoOrden = orden.getPagoProgramado();
            throw new IncomeException("La orden " + idOrden + " ya está PAGADA. Para anularla:"
                    + " 1) POST /pgtr/revertirConfirmado/"
                    + (pagoOrden != null ? pagoOrden.getId() : "{idPago}")
                    + " (revierte el pago en tesorería), 2) POST /rest/odbs/revertirPago/" + idOrden
                    + " (deja la orden en REVERTIDA), y recién ahí anular.");
        }
        if (Long.valueOf(RhhEstadoOrdenBeneficio.ANULADA).equals(orden.getEstado())) {
            throw new IncomeException("La orden " + idOrden + " ya está ANULADA.");
        }

        for (LiquidacionBeneficioSocial liquidacion : liquidacionBeneficioSocialDaoService.selectByOrden(idOrden)) {
            liquidacion.setOrdenBeneficioSocial(null);
            liquidacionBeneficioSocialDaoService.save(liquidacion, liquidacion.getCodigo());
        }

        orden.setEstado(Long.valueOf(RhhEstadoOrdenBeneficio.ANULADA));
        // ODBS no tiene columna dedicada de motivo de anulacion (a diferencia de
        // AnticipoEmpleado.motivoAnulacion): se deja en observaciones, que es lo unico que el
        // DDL de esta tabla ofrece para texto libre.
        String obsAnterior = orden.getObservaciones();
        orden.setObservaciones("ANULADA: " + motivo.trim()
                + (obsAnterior != null && !obsAnterior.trim().isEmpty() ? " | " + obsAnterior : ""));
        orden.setUsuarioRegistro(usuario);
        orden = ordenBeneficioSocialDaoService.save(orden, orden.getCodigo());

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("exito", Boolean.TRUE);
        resultado.put("idOrden", orden.getCodigo());
        resultado.put("mensaje", "Orden anulada.");
        System.out.println("✓ Orden " + idOrden + " anulada. Motivo: " + motivo);
        return resultado;
    }

    @Override
    public Map<String, Object> revertirPago(Long idOrden, String motivo, String usuario) throws Throwable {
        System.out.println("=== revertirPago orden de beneficio social | idOrden=" + idOrden + " ===");

        OrdenBeneficioSocial orden = em.find(OrdenBeneficioSocial.class, idOrden);
        if (orden == null) {
            throw new IncomeException("No existe la orden de beneficio social " + idOrden + ".");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo de la reversión.");
        }
        if (!Long.valueOf(RhhEstadoOrdenBeneficio.PAGADA).equals(orden.getEstado())) {
            throw new IncomeException("La orden " + idOrden + " no está PAGADA (estado actual: "
                    + textoEstadoOrden(orden.getEstado()) + "). Sólo se revierte un pago confirmado.");
        }

        PagoProgramado pago = orden.getPagoProgramado();
        if (pago != null && pago.getEstado() != null
                && pago.getEstado().intValue() == EstadoPagoProgramado.CONFIRMADO) {
            throw new IncomeException("El pago " + pago.getId() + " sigue CONFIRMADO en tesorería."
                    + " Revierta primero con POST /pgtr/revertirConfirmado/" + pago.getId() + ".");
        }

        List<LiquidacionBeneficioSocial> liquidaciones = liquidacionBeneficioSocialDaoService
                .selectByOrden(idOrden);

        // Caso duro (contrato §6.3): si el rol del periodo de las novedades ya se proceso
        // (paso mas alla de ABIERTO), rechazar entero -- borrar solo la novedad no borra el
        // renglon que el rol ya consumio, y eso no da ningun error: se ve como un rol correcto.
        int tipoBeneficio = orden.getTipoBeneficio().intValue();
        ConceptoNomina concepto = null;
        List<NovedadNomina> novedades = new ArrayList<NovedadNomina>();
        if (tipoBeneficio == RhhTipoBeneficioSocial.DECIMO_TERCERO
                || tipoBeneficio == RhhTipoBeneficioSocial.DECIMO_CUARTO) {
            int rolMotor = tipoBeneficio == RhhTipoBeneficioSocial.DECIMO_TERCERO
                    ? RhhRolConceptoMotor.DECIMO_TERCERO_ACUMULADO_PAGADO
                    : RhhRolConceptoMotor.DECIMO_CUARTO_ACUMULADO_PAGADO;
            Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
            concepto = conceptoNominaDaoService.selectByRolMotor(Integer.valueOf(rolMotor), idEmpresa);
            String descripcion = marcadorNovedadDecimo(tipoBeneficio, orden.getCodigo());
            if (concepto != null) {
                for (LiquidacionBeneficioSocial liquidacion : liquidaciones) {
                    if (liquidacion.getEmpleado() == null) {
                        continue;
                    }
                    NovedadNomina novedad = novedadNominaDaoService.selectPorDescripcion(
                            liquidacion.getEmpleado().getCodigo(), concepto.getCodigo(), descripcion);
                    if (novedad != null) {
                        novedades.add(novedad);
                    }
                }
            }
            // ===== INICIO decimo acumulado en periodo CALCULADO (equipo omen-saa-2, bloqueo urgente 2026-09-08) =====
            // Antes rechazaba con solo mirar el ESTADO del periodo (> ABIERTO). Eso ya no
            // alcanza: confirmarPago ahora puede crear la novedad con el periodo en
            // CALCULADO (mismo criterio que VNPG), asi que un periodo CALCULADO ya no
            // implica que el rol haya absorbido la novedad -- puede que se haya creado
            // DESPUES del ultimo calculo, y entonces no hay ningun renglon huerfano que
            // proteger. Se verifica renglon por renglon (novedadAbsorbidaPorRol), no el
            // estado del periodo: solo si el rol REALMENTE la consumio se rechaza.
            for (NovedadNomina novedad : novedades) {
                PeriodoNomina periodoNovedad = novedad.getPeriodoNomina();
                if (periodoNovedad != null && novedadAbsorbidaPorRol(novedad, periodoNovedad)) {
                    throw new IncomeException("El rol del período " + periodoNovedad.getMes() + "/"
                            + periodoNovedad.getAnio() + " ya calculó y absorbió la novedad de esta orden"
                            + " (empleado " + novedad.getEmpleado().getCodigo() + "). Reabra o reprocese"
                            + " el período antes de revertir el pago.");
                }
            }
            // ===== FIN decimo acumulado en periodo CALCULADO =====
        }

        Long idAsiento = orden.getAsiento();
        if (idAsiento != null) {
            asientoService.anulaAsiento(idAsiento);
        }

        for (LiquidacionBeneficioSocial liquidacion : liquidaciones) {
            liquidacion.setValorPagado(Double.valueOf(0D));
            liquidacion.setFechaPago(null);
            liquidacion.setEstado(Long.valueOf(1L));
            liquidacionBeneficioSocialDaoService.save(liquidacion, liquidacion.getCodigo());
        }

        for (NovedadNomina novedad : novedades) {
            novedadNominaDaoService.remove(new NovedadNomina(), novedad.getCodigo());
        }

        orden.setEstado(Long.valueOf(RhhEstadoOrdenBeneficio.REVERTIDA));
        orden.setFechaPago(null);
        orden.setAsiento(null);
        String obsAnterior = orden.getObservaciones();
        orden.setObservaciones("REVERTIDA: " + motivo.trim()
                + (obsAnterior != null && !obsAnterior.trim().isEmpty() ? " | " + obsAnterior : ""));
        orden.setUsuarioRegistro(usuario);
        orden = ordenBeneficioSocialDaoService.save(orden, orden.getCodigo());

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("exito", Boolean.TRUE);
        resultado.put("idOrden", orden.getCodigo());
        resultado.put("liquidacionesRevertidas", Integer.valueOf(liquidaciones.size()));
        resultado.put("novedadesEliminadas", Integer.valueOf(novedades.size()));
        resultado.put("asientoAnulado", idAsiento);
        resultado.put("mensaje", "Pago revertido. La provisión vuelve a estar viva y la orden puede anularse.");
        System.out.println("✓ Orden " + idOrden + " REVERTIDA | asiento anulado=" + idAsiento
                + " | liquidaciones=" + liquidaciones.size() + " | novedades=" + novedades.size());
        return resultado;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void exigeTipoBeneficioValido(Long tipoBeneficio) throws Throwable {
        if (tipoBeneficio == null
                || (tipoBeneficio.intValue() != RhhTipoBeneficioSocial.DECIMO_TERCERO
                        && tipoBeneficio.intValue() != RhhTipoBeneficioSocial.DECIMO_CUARTO
                        && tipoBeneficio.intValue() != RhhTipoBeneficioSocial.FONDOS_DE_RESERVA)) {
            throw new IncomeException("tipoBeneficio debe ser 1 (decimo tercero), 2 (decimo cuarto) o"
                    + " 3 (fondos de reserva). Esta orden no maneja vacaciones ni utilidades.");
        }
    }

    /**
     * La region solo aplica al decimo cuarto: obligatoria ahi, prohibida en los demas tipos.
     * No se ignora en silencio (contrato #3.4).
     */
    private void exigeRegionCoherente(Long tipoBeneficio, Long region) throws Throwable {
        boolean esDecimoCuarto = tipoBeneficio != null
                && tipoBeneficio.intValue() == RhhTipoBeneficioSocial.DECIMO_CUARTO;
        if (esDecimoCuarto && region == null) {
            throw new IncomeException("El decimo cuarto exige region (rubro RHH_REGION_DECIMO_CUARTO):"
                    + " sin ella no se sabe que ventana aplicarle.");
        }
        if (!esDecimoCuarto && region != null) {
            throw new IncomeException("region solo aplica al decimo cuarto (tipoBeneficio=2). Para"
                    + " tipoBeneficio=" + tipoBeneficio + " debe ir null.");
        }
    }

    /**
     * Numera con un COUNT+1 sobre las ordenes ya emitidas del anio, no con un MAX parseado.
     * Consecuencia aceptada (2026-09-01): si se anula una orden y se genera otra el mismo
     * anio, el COUNT vuelve a contar la anulada y el numero visible se repite entre las dos.
     * No hay unicidad forzada en ODBSNMRO, asi que no rompe nada -es solo lo que se ve-, pero
     * puede confundir. Si algun dia molesta, pasar a MAX(secuencial) parseado del numero.
     */
    private String armaNumero(Integer anio) throws Throwable {
        long secuencial = ordenBeneficioSocialDaoService.countByAnio(anio) + 1;
        return String.format(PREFIJO_NUMERO, anio, Long.valueOf(secuencial));
    }

    /**
     * Registra el decimo acumulado ya pagado como NovedadNomina INFORMATIVA, una por empleado
     * de la orden, para que el rol de fin de mes lo refleje sin sumarlo al neto (concepto de
     * rol 32/33, RhhTipoConceptoNomina.INFORMATIVO — el motor arma el neto solo con INGRESO y
     * EGRESO, asi que este concepto no puede duplicar el pago). Fondos de reserva
     * (tipoBeneficio=3) no lleva novedad: se saltea sin error. El enlace es por convencion de
     * descripcion, igual que SolicitudVacacionesServiceImpl (sin FK, decision del usuario —
     * ver docs/logica-negocio/rhh/PLAN-PAGO-DECIMOS-EN-EL-MES.md §7.C).
     *
     * <p><b>El periodo admite ABIERTO o CALCULADO</b> (bloqueo urgente 2026-09-08, mismo
     * criterio que {@code ValorNoPagadoServiceImpl.exigePeriodoModificable}, decision del
     * usuario: "Sí debería, pero debería dejarme recalcular después"). Antes exigia ABIERTO
     * estricto, y {@code reabrirPeriodo} deja el periodo en CALCULADO, no en ABIERTO -no hay
     * ningun camino en todo el proyecto que devuelva un periodo a ABIERTO desde CALCULADO-,
     * asi que ese guard era una pared sin puerta. La novedad es INFORMATIVA: no suma al
     * neto, no toca bases de IESS/IR ni contabilidad, asi que registrarla en un periodo ya
     * calculado no corrompe nada, solo no se ve hasta recalcular -por eso este metodo
     * devuelve una advertencia en vez de solo loguearla, para que confirmarPago se la pase
     * al usuario en la respuesta.</p>
     *
     * @return	: Advertencia para el usuario si el periodo estaba CALCULADO (hay que
     *			  recalcular el rol para ver el renglon), o null si estaba ABIERTO
     */
    private String crearNovedadesDecimoAcumulado(OrdenBeneficioSocial orden,
            List<LiquidacionBeneficioSocial> liquidaciones, LocalDate fecha, String usuario) throws Throwable {
        int tipoBeneficio = orden.getTipoBeneficio().intValue();
        if (tipoBeneficio != RhhTipoBeneficioSocial.DECIMO_TERCERO
                && tipoBeneficio != RhhTipoBeneficioSocial.DECIMO_CUARTO) {
            return null;
        }
        int rolMotor = tipoBeneficio == RhhTipoBeneficioSocial.DECIMO_TERCERO
                ? RhhRolConceptoMotor.DECIMO_TERCERO_ACUMULADO_PAGADO
                : RhhRolConceptoMotor.DECIMO_CUARTO_ACUMULADO_PAGADO;
        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;

        ConceptoNomina concepto = conceptoNominaDaoService.selectByRolMotor(Integer.valueOf(rolMotor), idEmpresa);
        if (concepto == null) {
            throw new IncomeException("No existe en la empresa " + idEmpresa + " el concepto de nomina"
                    + " con rol de motor " + rolMotor + " (" + textoTipoBeneficio(orden.getTipoBeneficio())
                    + " acumulado pagado). Falta correr el script que lo crea (rhh/sql/e2-18) antes de"
                    + " poder registrar la novedad del pago de la orden " + orden.getCodigo() + ".");
        }

        PeriodoNomina periodo = periodoNominaDaoService.selectByFechaEmpresa(idEmpresa, fecha);
        if (periodo == null) {
            throw new IncomeException("No existe un periodo de nomina de la empresa " + idEmpresa
                    + " que contenga la fecha " + fecha + ": no se puede registrar la novedad del pago"
                    + " de la orden " + orden.getCodigo() + ".");
        }
        exigePeriodoModificable(periodo, "registrar la novedad del pago de la orden " + orden.getCodigo());

        String descripcion = marcadorNovedadDecimo(tipoBeneficio, orden.getCodigo());
        for (LiquidacionBeneficioSocial liquidacion : liquidaciones) {
            if (liquidacion.getEmpleado() == null) {
                continue;
            }
            NovedadNomina novedad = new NovedadNomina();
            novedad.setPeriodoNomina(periodo);
            novedad.setEmpleado(liquidacion.getEmpleado());
            novedad.setConceptoNomina(concepto);
            novedad.setValor(liquidacion.getValorPagado());
            novedad.setDescripcion(descripcion);
            novedad.setAprobada("S");
            novedad.setUsuarioAprueba(usuario);
            novedad.setFechaAprobacion(LocalDate.now());
            novedad.setEstado(Long.valueOf(1L));
            novedad.setFechaRegistro(LocalDateTime.now());
            novedad.setUsuarioRegistro(usuario);
            novedadNominaDaoService.save(novedad, null);
        }

        if (Long.valueOf(RhhEstadoPeriodoNomina.CALCULADO).equals(periodo.getEstado())) {
            return "El período " + periodo.getMes() + "/" + periodo.getAnio() + " ya estaba CALCULADO:"
                    + " recalcule el rol para que el " + textoTipoBeneficio(orden.getTipoBeneficio())
                    + " acumulado pagado se refleje.";
        }
        return null;
    }

    // ===== INICIO decimo acumulado en periodo CALCULADO (equipo omen-saa-2, bloqueo urgente 2026-09-08) =====
    /**
     * Exige que el periodo admita cambios de nomina (registrar la novedad del decimo
     * acumulado). Copiado de {@code ValorNoPagadoServiceImpl.exigePeriodoModificable} -mismo
     * criterio, mismo texto de mensaje-, no extraido a un lugar compartido por el apuro del
     * bloqueo: hay al menos tres clases con la misma pregunta (esta,
     * {@code ValorNoPagadoServiceImpl}, {@code SolicitudVacacionesServiceImpl}) y mover eso
     * ahora es mas alcance del que un arreglo urgente deberia llevar. Pendiente de
     * centralizar.
     *
     * <ul>
     * <li>{@code ABIERTO}(1) o {@code CALCULADO}(3): permitido.</li>
     * <li>{@code EN_CALCULO}(2): rechazado -- registrar en medio de un calculo es una carrera
     * contra el motor que esta leyendo/escribiendo esta misma nomina.</li>
     * <li>Cualquier otro ({@code >= APROBADO}): rechazado, ya no admite cambios de nomina.</li>
     * </ul>
     *
     * @param periodo		: Periodo de nomina
     * @param operacion		: Texto de la operacion, para el mensaje
     * @throws Throwable	: IncomeException si el periodo no admite la operacion
     */
    private void exigePeriodoModificable(PeriodoNomina periodo, String operacion) throws Throwable {
        int estado = periodo.getEstado() != null ? periodo.getEstado().intValue() : -1;
        if (estado == RhhEstadoPeriodoNomina.ABIERTO || estado == RhhEstadoPeriodoNomina.CALCULADO) {
            return;
        }
        if (estado == RhhEstadoPeriodoNomina.EN_CALCULO) {
            throw new IncomeException("El rol del periodo " + periodo.getMes() + "/" + periodo.getAnio()
                    + " se esta calculando: espere a que termine antes de " + operacion + ".");
        }
        throw new IncomeException("El periodo " + periodo.getMes() + "/" + periodo.getAnio() + " esta "
                + textoEstadoPeriodo(periodo.getEstado()) + ": ya no admite cambios de nomina.");
    }

    private String textoEstadoPeriodo(Long estado) {
        if (estado == null) {
            return "en un estado desconocido";
        }
        switch (estado.intValue()) {
            case RhhEstadoPeriodoNomina.ABIERTO:
                return "ABIERTO";
            case RhhEstadoPeriodoNomina.EN_CALCULO:
                return "EN_CALCULO";
            case RhhEstadoPeriodoNomina.CALCULADO:
                return "CALCULADO";
            case RhhEstadoPeriodoNomina.APROBADO:
                return "APROBADO";
            case RhhEstadoPeriodoNomina.CONTABILIZADO:
                return "CONTABILIZADO";
            case RhhEstadoPeriodoNomina.PAGADO:
                return "PAGADO";
            case RhhEstadoPeriodoNomina.CERRADO:
                return "CERRADO";
            case RhhEstadoPeriodoNomina.ANULADO:
                return "ANULADO";
            default:
                return "en estado " + estado;
        }
    }

    /**
     * Indica si el rol ya calculo y absorbio esta novedad: existe un renglon de la nomina de
     * ese empleado en ese periodo que la referencia (misma trazabilidad que usa el motor,
     * {@code ProcesoNominaServiceImpl}, tabla "RHH.NVNM" + el codigo de la novedad). Distingue
     * "la novedad existe" (el registro esta en RHH.NVNM, pero el rol no la vio -- se creo con
     * el periodo ya CALCULADO, o se creo antes pero el rol no se ha vuelto a correr) de "el
     * rol ya la absorbio" (se recalculo el periodo despues de que la novedad existiera): solo
     * el segundo caso deja un renglon huerfano si se borra la novedad sin mas.
     *
     * @param novedad	: Novedad a verificar
     * @param periodo	: Periodo de la novedad
     * @return			: true si ya hay un renglon que la referencia
     * @throws Throwable	: Excepcion
     */
    private boolean novedadAbsorbidaPorRol(NovedadNomina novedad, PeriodoNomina periodo) throws Throwable {
        if (novedad.getEmpleado() == null) {
            return false;
        }
        Nomina nomina = nominaDaoService.selectByPeriodoYEmpleado(periodo.getCodigo(),
                novedad.getEmpleado().getCodigo());
        if (nomina == null) {
            return false;
        }
        List<ReglonNomina> renglones = reglonNominaDaoService.selectByNomina(nomina.getCodigo());
        if (renglones == null) {
            return false;
        }
        for (ReglonNomina renglon : renglones) {
            if ("RHH.NVNM".equals(renglon.getTablaReferencia())
                    && novedad.getCodigo().equals(renglon.getIdReferencia())) {
                return true;
            }
        }
        return false;
    }
    // ===== FIN decimo acumulado en periodo CALCULADO =====

    /** Marcador de descripcion acordado en el §7.C del plan: "Décimo <tercero|cuarto> acumulado — orden #{idOrden}". */
    private String marcadorNovedadDecimo(int tipoBeneficio, Long idOrden) {
        String tipo = tipoBeneficio == RhhTipoBeneficioSocial.DECIMO_TERCERO ? "tercero" : "cuarto";
        return "Décimo " + tipo + " acumulado — orden #" + idOrden;
    }

    private String nombreEmpleado(LiquidacionBeneficioSocial liquidacion) {
        if (liquidacion.getEmpleado() == null) {
            return null;
        }
        String apellidos = liquidacion.getEmpleado().getApellidos();
        String nombres = liquidacion.getEmpleado().getNombres();
        return ((apellidos != null ? apellidos : "") + " " + (nombres != null ? nombres : "")).trim();
    }

    private String textoTipoBeneficio(Long tipoBeneficio) {
        if (tipoBeneficio == null) {
            return null;
        }
        switch (tipoBeneficio.intValue()) {
            case RhhTipoBeneficioSocial.DECIMO_TERCERO:
                return "DECIMO TERCERO";
            case RhhTipoBeneficioSocial.DECIMO_CUARTO:
                return "DECIMO CUARTO";
            case RhhTipoBeneficioSocial.FONDOS_DE_RESERVA:
                return "FONDOS DE RESERVA";
            default:
                return "TIPO " + tipoBeneficio;
        }
    }

    private String textoEstadoOrden(Long estado) {
        if (estado == null) {
            return null;
        }
        switch (estado.intValue()) {
            case RhhEstadoOrdenBeneficio.GENERADA:
                return "GENERADA";
            case RhhEstadoOrdenBeneficio.ENVIADA_A_TESORERIA:
                return "ENVIADA_A_TESORERIA";
            case RhhEstadoOrdenBeneficio.PAGADA:
                return "PAGADA";
            case RhhEstadoOrdenBeneficio.ANULADA:
                return "ANULADA";
            default:
                return "ESTADO " + estado;
        }
    }

    private String textoEstadoPago(Long estado) {
        if (estado == null) {
            return null;
        }
        switch (estado.intValue()) {
            case EstadoPagoProgramado.POR_APROBAR:
                return "POR_APROBAR";
            case EstadoPagoProgramado.REGISTRADO:
                return "REGISTRADO";
            case EstadoPagoProgramado.EN_ARCHIVO:
                return "EN_ARCHIVO";
            case EstadoPagoProgramado.CONFIRMADO:
                return "CONFIRMADO";
            case EstadoPagoProgramado.RECHAZADO:
                return "RECHAZADO";
            case EstadoPagoProgramado.ANULADO:
                return "ANULADO";
            default:
                return "ESTADO " + estado;
        }
    }

    private String nombreUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return "SISTEMA";
        }
        Usuario usuario = em.find(Usuario.class, idUsuario);
        return (usuario != null && usuario.getNombre() != null) ? usuario.getNombre() : "SISTEMA";
    }
}
