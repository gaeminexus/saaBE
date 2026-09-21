package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.RecepcionValorSeguroDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.CuentaTipoAporteService;
import com.saa.ejb.crd.service.RecepcionValorSeguroService;
import com.saa.ejb.crd.service.dto.ResultadoRegistroAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroRecepcionSeguro;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.CuentaTipoAporte;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.RecepcionValorSeguro;
import com.saa.model.crd.TipoAporte;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.rubros.CrdEstadoRecepcionSeguro;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class RecepcionValorSeguroServiceImpl implements RecepcionValorSeguroService {

    @EJB
    private RecepcionValorSeguroDaoService recepcionValorSeguroDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private AporteService aporteService;

    @EJB
    private CuentaTipoAporteService cuentaTipoAporteService;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoService asientoService;

    // ---------------------------------------------------------------------------------------
    // EntityService
    // ---------------------------------------------------------------------------------------

    @Override
    public RecepcionValorSeguro selectById(Long id) throws Throwable {
        System.out.println("RecepcionValorSeguroService.selectById - id: " + id);
        return recepcionValorSeguroDaoService.selectById(id, NombreEntidadesCredito.RECEPCION_VALOR_SEGURO);
    }

    @Override
    public List<RecepcionValorSeguro> selectAll() throws Throwable {
        System.out.println("RecepcionValorSeguroService.selectAll");
        return recepcionValorSeguroDaoService.selectAll(NombreEntidadesCredito.RECEPCION_VALOR_SEGURO);
    }

    @Override
    public List<RecepcionValorSeguro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("RecepcionValorSeguroService.selectByCriteria");
        return recepcionValorSeguroDaoService.selectByCriteria(datos, NombreEntidadesCredito.RECEPCION_VALOR_SEGURO);
    }

    @Override
    public RecepcionValorSeguro saveSingle(RecepcionValorSeguro registro) throws Throwable {
        System.out.println("RecepcionValorSeguroService.saveSingle");
        return recepcionValorSeguroDaoService.save(registro, registro.getCodigo());
    }

    @Override
    public void save(List<RecepcionValorSeguro> registros) throws Throwable {
        System.out.println("RecepcionValorSeguroService.save - lote: " + registros.size());
        for (RecepcionValorSeguro registro : registros) {
            saveSingle(registro);
        }
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("RecepcionValorSeguroService.remove - lote: " + ids.size());
        RecepcionValorSeguro entidad = new RecepcionValorSeguro();
        for (Long id : ids) {
            recepcionValorSeguroDaoService.remove(entidad, id);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Registro
    // ---------------------------------------------------------------------------------------

    @Override
    public RecepcionValorSeguro registrar(SolicitudRegistroRecepcionSeguro solicitud) throws Throwable {
        System.out.println("RecepcionValorSeguroService.registrar - entidad: "
                + (solicitud != null ? solicitud.getIdEntidad() : null));

        if (solicitud == null) {
            throw new IncomeException(ERR_VALIDACION + ": no se recibió el cuerpo de la solicitud");
        }
        if (solicitud.getIdEntidad() == null) {
            throw new IncomeException(ERR_VALIDACION + ": idEntidad es obligatorio");
        }
        if (solicitud.getIdTipoAporte() == null) {
            throw new IncomeException(ERR_VALIDACION + ": idTipoAporte es obligatorio");
        }
        if (solicitud.getIdCuentaBancaria() == null) {
            throw new IncomeException(ERR_VALIDACION + ": idCuentaBancaria es obligatorio");
        }
        if (solicitud.getFecha() == null) {
            throw new IncomeException(ERR_VALIDACION + ": fecha es obligatoria");
        }
        if (solicitud.getFecha().isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_VALIDACION + ": la fecha " + solicitud.getFecha() + " es futura");
        }
        if (vacio(solicitud.getUsuario())) {
            throw new IncomeException(ERR_VALIDACION + ": usuario es obligatorio");
        }
        if (vacio(solicitud.getReferencia())) {
            throw new IncomeException(ERR_VALIDACION + ": referencia es obligatoria");
        }
        if (vacio(solicitud.getRutaRespaldo())) {
            throw new IncomeException(ERR_VALIDACION + ": el respaldo digitalizado (rutaRespaldo) es obligatorio");
        }
        double valor = redondear(solicitud.getValor() != null ? solicitud.getValor() : 0.0);
        if (valor <= 0.0) {
            throw new IncomeException(ERR_VALIDACION + ": el valor debe ser mayor a cero");
        }

        Entidad entidad = entidadDaoService.find(new Entidad(), solicitud.getIdEntidad());
        if (entidad == null) {
            throw new IncomeException("No existe el partícipe " + solicitud.getIdEntidad() + ".");
        }
        TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), solicitud.getIdTipoAporte());
        if (tipo == null || tipo.getEstado() == null || tipo.getEstado() != 1L) {
            throw new IncomeException("El tipo de aporte " + solicitud.getIdTipoAporte()
                    + (tipo != null ? " (" + tipo.getNombre() + ")" : "") + " no existe o no está vigente.");
        }
        CuentaBancaria cuentaBancaria = cuentaBancariaDaoService.find(new CuentaBancaria(),
                solicitud.getIdCuentaBancaria());
        if (cuentaBancaria == null) {
            throw new IncomeException("No existe la cuenta bancaria " + solicitud.getIdCuentaBancaria() + ".");
        }
        // Falla ahora, no al aprobar: una cuenta sin cuenta contable/empresa nunca podría aprobarse.
        derivarEmpresa(cuentaBancaria);

        RecepcionValorSeguro recepcion = new RecepcionValorSeguro();
        recepcion.setEntidad(entidad);
        recepcion.setTipoAporte(tipo);
        recepcion.setEstado(Long.valueOf(CrdEstadoRecepcionSeguro.REGISTRADO));
        recepcion.setCuentaBancaria(cuentaBancaria);
        recepcion.setReferencia(solicitud.getReferencia().trim());
        recepcion.setRutaRespaldo(solicitud.getRutaRespaldo().trim());
        recepcion.setValor(valor);
        recepcion.setFecha(solicitud.getFecha());
        recepcion.setObservacion(vacio(solicitud.getObservacion()) ? null : solicitud.getObservacion().trim());
        recepcion.setUsuarioRegistro(solicitud.getUsuario().trim());
        recepcion.setFechaRegistro(LocalDateTime.now());
        return recepcionValorSeguroDaoService.save(recepcion, null);
    }

    // ---------------------------------------------------------------------------------------
    // Aprobación: un asiento + el aporte positivo + estado, en una sola transacción
    // ---------------------------------------------------------------------------------------

    @Override
    public RecepcionValorSeguro aprobar(Long idRecepcion, String usuario) throws Throwable {
        System.out.println("RecepcionValorSeguroService.aprobar - recepcion: " + idRecepcion);
        if (vacio(usuario)) {
            throw new IncomeException(ERR_VALIDACION + ": usuario es obligatorio");
        }
        RecepcionValorSeguro recepcion = buscarBloqueada(idRecepcion);
        exigirEstado(recepcion, CrdEstadoRecepcionSeguro.REGISTRADO, "aprobar");

        // Sin contabilidad activa no se aprueba: el valor entraría al saldo del partícipe sin
        // asiento. A diferencia del cobro, acá no se sigue "en silencio".
        if (!configuracionContabilidadService.contabilidadActiva()) {
            throw new IncomeException("La contabilidad de CRD está desactivada: no se puede aprobar la"
                    + " recepción " + idRecepcion + " porque no se generaría el asiento. Active la"
                    + " contabilidad de CRD y vuelva a aprobar.");
        }

        CuentaBancaria cuentaBancaria = recepcion.getCuentaBancaria();
        Long idEmpresa = derivarEmpresa(cuentaBancaria);
        TipoAporte tipo = recepcion.getTipoAporte();

        CuentaTipoAporte config = cuentaTipoAporteService.selectByTipoAporteYEmpresa(tipo.getCodigo(), idEmpresa);
        if (config == null || config.getCuentaPasivo() == null) {
            throw new IncomeException("El tipo de aporte " + tipo.getCodigo() + " (" + tipo.getNombre()
                    + ") no tiene cuenta de pasivo configurada en CRD.CTAP para la empresa " + idEmpresa
                    + "; configúrela antes de aprobar esta recepción.");
        }

        double valor = redondear(recepcion.getValor() != null ? recepcion.getValor() : 0.0);
        String prefijo = "Recepción valor de seguro " + recepcion.getCodigo();

        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();
        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(lineaDesdePlanCuenta(cuentaBanco, valor, true, prefijo + " - ingreso a banco"));
        lineas.add(lineaDesdePlanCuenta(config.getCuentaPasivo(), valor, false,
                prefijo + " - " + tipo.getNombre()));

        Entidad entidad = recepcion.getEntidad();
        String observacion = prefijo + " | Cédula: "
                + (entidad.getNumeroIdentificacion() != null ? entidad.getNumeroIdentificacion() : "-")
                + " | Nombre: " + (entidad.getRazonSocial() != null ? entidad.getRazonSocial() : "-");

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS,
                recepcion.getFecha(), observacion, usuario.trim(), lineas,
                Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        SolicitudRegistroAporte solicitudAporte = new SolicitudRegistroAporte();
        solicitudAporte.setIdEntidad(entidad.getCodigo());
        solicitudAporte.setIdTipoAporte(tipo.getCodigo());
        solicitudAporte.setValor(valor);
        solicitudAporte.setUsuario(usuario.trim());
        solicitudAporte.setObservacion(prefijo + (recepcion.getObservacion() != null
                ? " - " + recepcion.getObservacion() : ""));
        solicitudAporte.setFechaTransaccion(recepcion.getFecha());
        solicitudAporte.setRutaDocumentoRespaldo(recepcion.getRutaRespaldo());
        solicitudAporte.setIdEmpresa(idEmpresa);
        ResultadoRegistroAporte resultadoAporte = aporteService.registrarAporte(solicitudAporte);

        Aporte aporte = aporteDaoService.find(new Aporte(), resultadoAporte.getIdAporte());

        recepcion.setAsiento(asiento);
        recepcion.setAporte(aporte);
        recepcion.setEstado(Long.valueOf(CrdEstadoRecepcionSeguro.APROBADO));
        recepcion.setUsuarioAprobacion(usuario.trim());
        recepcion.setFechaAprobacion(LocalDateTime.now());
        return recepcionValorSeguroDaoService.save(recepcion, recepcion.getCodigo());
    }

    // ---------------------------------------------------------------------------------------
    // Rechazo y anulación
    // ---------------------------------------------------------------------------------------

    @Override
    public RecepcionValorSeguro rechazar(Long idRecepcion, String usuario, String motivo) throws Throwable {
        System.out.println("RecepcionValorSeguroService.rechazar - recepcion: " + idRecepcion);
        if (vacio(usuario)) {
            throw new IncomeException(ERR_VALIDACION + ": usuario es obligatorio");
        }
        if (vacio(motivo)) {
            throw new IncomeException(ERR_VALIDACION + ": el motivo del rechazo es obligatorio");
        }
        RecepcionValorSeguro recepcion = buscarBloqueada(idRecepcion);
        exigirEstado(recepcion, CrdEstadoRecepcionSeguro.REGISTRADO, "rechazar");

        recepcion.setEstado(Long.valueOf(CrdEstadoRecepcionSeguro.RECHAZADO));
        recepcion.setUsuarioRechazo(usuario.trim());
        recepcion.setFechaRechazo(LocalDateTime.now());
        recepcion.setMotivoRechazo(motivo.trim());
        return recepcionValorSeguroDaoService.save(recepcion, recepcion.getCodigo());
    }

    @Override
    public RecepcionValorSeguro anular(Long idRecepcion, String usuario, String motivo) throws Throwable {
        System.out.println("RecepcionValorSeguroService.anular - recepcion: " + idRecepcion);
        if (vacio(usuario)) {
            throw new IncomeException(ERR_VALIDACION + ": usuario es obligatorio");
        }
        if (vacio(motivo)) {
            throw new IncomeException(ERR_VALIDACION + ": el motivo de la anulación es obligatorio");
        }
        RecepcionValorSeguro recepcion = buscarBloqueada(idRecepcion);
        exigirEstado(recepcion, CrdEstadoRecepcionSeguro.APROBADO, "anular");

        if (recepcion.getAporte() == null || recepcion.getAsiento() == null) {
            throw new IncomeException("La recepción " + idRecepcion + " está APROBADA pero no tiene"
                    + " registrado su aporte o su asiento; no se puede reversar automáticamente.");
        }

        // Primero el aporte: reversarAporte rechaza (ERR_SALDO_INSUFICIENTE) si el partícipe ya
        // usó ese dinero, y en ese caso no se ha tocado nada todavía.
        aporteService.reversarAporte(recepcion.getAporte().getCodigo(), usuario.trim(),
                "Anulación de la recepción de valor de seguro " + idRecepcion + ": " + motivo.trim());
        asientoService.anulaAsiento(recepcion.getAsiento().getCodigo(), usuario.trim(),
                "Anulación de la recepción de valor de seguro " + idRecepcion + ": " + motivo.trim());

        recepcion.setEstado(Long.valueOf(CrdEstadoRecepcionSeguro.ANULADO));
        recepcion.setUsuarioAnulacion(usuario.trim());
        recepcion.setFechaAnulacion(LocalDateTime.now());
        recepcion.setMotivoAnulacion(motivo.trim());
        return recepcionValorSeguroDaoService.save(recepcion, recepcion.getCodigo());
    }

    // ---------------------------------------------------------------------------------------
    // Auxiliares
    // ---------------------------------------------------------------------------------------

    private RecepcionValorSeguro buscarBloqueada(Long idRecepcion) throws Throwable {
        if (idRecepcion == null) {
            throw new IncomeException(ERR_VALIDACION + ": el código de la recepción es obligatorio");
        }
        RecepcionValorSeguro recepcion = recepcionValorSeguroDaoService.selectParaActualizar(idRecepcion);
        if (recepcion == null) {
            throw new IncomeException(ERR_NO_ENCONTRADA + ": no existe la recepción " + idRecepcion);
        }
        return recepcion;
    }

    private void exigirEstado(RecepcionValorSeguro recepcion, int esperado, String accion) {
        if (recepcion.getEstado() == null || recepcion.getEstado() != esperado) {
            throw new IncomeException("La recepción " + recepcion.getCodigo() + " está en estado "
                    + textoEstado(recepcion.getEstado()) + "; solo se puede " + accion + " una recepción en"
                    + " estado " + textoEstado(Long.valueOf(esperado)) + ".");
        }
    }

    private String textoEstado(Long estado) {
        if (estado == null) {
            return "SIN ESTADO";
        }
        switch (estado.intValue()) {
            case CrdEstadoRecepcionSeguro.REGISTRADO: return "REGISTRADO";
            case CrdEstadoRecepcionSeguro.APROBADO: return "APROBADO";
            case CrdEstadoRecepcionSeguro.RECHAZADO: return "RECHAZADO";
            case CrdEstadoRecepcionSeguro.ANULADO: return "ANULADO";
            default: return "DESCONOCIDO (" + estado + ")";
        }
    }

    /** Misma derivación que CobroCreditoServiceImpl.derivarEmpresaCobro: nunca la del cliente. */
    private Long derivarEmpresa(CuentaBancaria cuentaBancaria) {
        if (cuentaBancaria == null) {
            throw new IncomeException("La recepción no tiene cuenta bancaria asignada; no se puede"
                    + " determinar la empresa contable de la operación.");
        }
        if (cuentaBancaria.getPlanCuenta() == null || cuentaBancaria.getPlanCuenta().getEmpresa() == null) {
            throw new IncomeException("La cuenta bancaria " + cuentaBancaria.getCodigo()
                    + " no tiene cuenta contable/empresa asignada; no se puede determinar la empresa"
                    + " contable de la recepción.");
        }
        return cuentaBancaria.getPlanCuenta().getEmpresa().getCodigo();
    }

    private DetalleAsiento lineaDesdePlanCuenta(PlanCuenta cuenta, double valor, boolean debe,
            String descripcion) {
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(debe ? redondear(valor) : 0.0);
        detalle.setValorHaber(debe ? 0.0 : redondear(valor));
        return detalle;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
