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
import com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService;
import com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.service.OrdenBeneficioSocialService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.rhh.LiquidacionBeneficioSocial;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.OrdenBeneficioSocial;
import com.saa.model.rhh.OrdenBeneficioSocialResumen;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RhhEstadoOrdenBeneficio;
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
        for (LiquidacionBeneficioSocial liquidacion : liquidaciones) {
            liquidacion.setValorPagado(liquidacion.getValor());
            liquidacion.setFechaPago(fecha);
            liquidacion.setEstado(Long.valueOf(LQBS_PAGADA));
            liquidacionBeneficioSocialDaoService.save(liquidacion, liquidacion.getCodigo());
        }

        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
        Asiento asiento = contabilizacionNominaService.contabilizarBajaProvisionBeneficioSocial(
                idEmpresa, orden.getTipoBeneficio().intValue(), orden.getTotal(), fecha,
                "Pago " + textoTipoBeneficio(orden.getTipoBeneficio()) + " " + orden.getAnio()
                        + " orden " + orden.getNumero(),
                usuario);

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
        resultado.put("mensaje", "Pago confirmado y provisión dada de baja.");
        System.out.println("✓ Orden " + idOrden + " PAGADA | asiento=" + asiento.getCodigo()
                + " | liquidaciones=" + liquidaciones.size());
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
            throw new IncomeException("La orden " + idOrden + " ya está PAGADA: revierta el pago en"
                    + " tesorería primero (POST /pgtr/revertirConfirmado/{id}) antes de anular.");
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

    private String armaNumero(Integer anio) throws Throwable {
        long secuencial = ordenBeneficioSocialDaoService.countByAnio(anio) + 1;
        return String.format(PREFIJO_NUMERO, anio, Long.valueOf(secuencial));
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
