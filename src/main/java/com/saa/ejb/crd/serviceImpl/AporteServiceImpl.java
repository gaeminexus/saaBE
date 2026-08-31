package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.VigenciaContratoService;
import com.saa.ejb.crd.service.dto.EstadoCuentaAportesDTO;
import com.saa.ejb.crd.service.dto.MovimientoEstadoCuentaDTO;
import com.saa.ejb.crd.service.dto.PeriodoEstadoCuentaDTO;
import com.saa.ejb.crd.service.dto.ResultadoRegistroAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAporte;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.TipoAporte;
import com.saa.model.scp.DetalleRubro;
import com.saa.rubros.CrdTipoMovimientoAporte;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class AporteServiceImpl implements AporteService {

    /** Longitud máxima en BYTES de APRT.APRTGLSA (VARCHAR2(2000)) */
    private static final int MAX_BYTES_GLOSA = 2000;

    /** Tolerancia para comparar valores monetarios (cuadre de asientos, saldos) */
    private static final double TOLERANCIA = 0.01;

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private SaldoAporteService saldoAporteService;

    @EJB
    private VigenciaContratoService vigenciaContratoService;

    @EJB
    private DetalleRubroDaoService detalleRubroDaoService;

    @EJB
    private com.saa.ejb.crd.service.EntidadService entidadService;

    @EJB
    private com.saa.ejb.cnt.service.PlantillaService plantillaService;

    @EJB
    private com.saa.ejb.cnt.dao.DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

    @EJB
    private com.saa.ejb.crd.service.ConfiguracionContabilidadService configuracionContabilidadService;

    /** CRD.TPAP.TPAPCDGO — jubilación, cesantía y pensión complementaria (J6: "PENSIÓN
     * COMPLEMENTARIA es el tipo de aporte TPAPCDGO = 23", confirmado por el usuario). */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;
    private static final long TIPO_APORTE_PENSION_COMPLEMENTARIA = 23L;

    /**
     * Recupera un registro de Aporte por su ID.
     */
    @Override
    public Aporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Aporte con id: " + id);
        return aporteDaoService.selectById(id, NombreEntidadesCredito.APORTE);
    }

    /**
     * Elimina uno o varios registros de Aporte.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de AporteService ... depurado");
        Aporte aporte = new Aporte();
        for (Long registro : id) {
            aporteDaoService.remove(aporte, registro);
        }
    }

    /**
     * Guarda una lista de registros de Aporte.
     */
    @Override
    public void save(List<Aporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de AporteService");
        for (Aporte registro : lista) {
            aporteDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Aporte.
     */
    @Override
    public List<Aporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll AporteService");
        List<Aporte> result = aporteDaoService.selectAll(NombreEntidadesCredito.APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Aporte no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Aporte.
     */
    @Override
    public Aporte saveSingle(Aporte aporte) throws Throwable {
        System.out.println("saveSingle - Aporte");
        if(aporte.getCodigo() == null){
        	aporte.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        aporte = aporteDaoService.save(aporte, aporte.getCodigo());
        return aporte;
    }

    /**
     * Recupera registros de Aporte segun criterios de búsqueda.
     */
    @Override
    public List<Aporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria AporteService");
        List<Aporte> result = aporteDaoService.selectByCriteria(datos, NombreEntidadesCredito.APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Aporte no devolvio ningun registro");
        }
        return result;
    }

    // ============================================================
    // G42 — Sumatorias agrupadas por entidad (delegadas al DAO)
    // ============================================================

    @Override
    public List<Object[]> selectSumaRendimientoPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
        return aporteDaoService.selectSumaRendimientoPorEntidad(fechaCorte);
    }

    @Override
    public List<Object[]> selectSumaPatronalPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
        return aporteDaoService.selectSumaPatronalPorEntidad(fechaCorte);
    }

    @Override
    public List<Object[]> selectSumaPersonalPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
        return aporteDaoService.selectSumaPersonalPorEntidad(fechaCorte);
    }

    @Override
    public List<Object[]> selectCountImposicionesJubilacionPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
        return aporteDaoService.selectCountImposicionesJubilacionPorEntidad(fechaCorte);
    }

    @Override
    public List<Object[]> selectSumaSaldoCuentaJubilacionPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
        return aporteDaoService.selectSumaSaldoCuentaJubilacionPorEntidad(fechaCorte);
    }

    @Override
    public List<Object[]> selectSumaAportesTipo23EnRango(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
        return aporteDaoService.selectSumaAportesTipo23EnRango(fechaInicio, fechaFin);
    }

    @Override
    public List<Object[]> selectTiposAportePorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable {
        return aporteDaoService.selectTiposAportePorEntidad(fechaCorte);
    }

    @Override
    public Double selectSumaTotalPorTipoAporte(java.time.LocalDateTime fechaCorte, Long tipoAporte) throws Throwable {
        return aporteDaoService.selectSumaTotalPorTipoAporte(fechaCorte, tipoAporte);
    }

    @Override
    public java.util.List<Object[]> selectSumaPorEntidadYTipoAporte(java.time.LocalDateTime fechaCorte) throws Throwable {
        return aporteDaoService.selectSumaPorEntidadYTipoAporte(fechaCorte);
    }

    // ============================================================
    // G43 — Imposiciones y saldo cuenta individual
    // ============================================================

    @Override
    public Long selectCountImposicionesPersonalesPorEntidad(Long codigoEntidad) throws Throwable {
        return aporteDaoService.selectCountImposicionesPersonalesPorEntidad(codigoEntidad);
    }

    @Override
    public Long selectCountImposicionesPatronalesPorEntidad(Long codigoEntidad) throws Throwable {
        return aporteDaoService.selectCountImposicionesPatronalesPorEntidad(codigoEntidad);
    }

    @Override
    public Double selectSumaAportesNegativosMesPorEntidad(Long codigoEntidad,
            java.time.LocalDateTime fechaInicio,
            java.time.LocalDateTime fechaFin) throws Throwable {
        return aporteDaoService.selectSumaAportesNegativosMesPorEntidad(codigoEntidad, fechaInicio, fechaFin);
    }

    // ========================================================================
    // Pago de aportes en ventanilla
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoRegistroAporte registrarAporte(SolicitudRegistroAporte solicitud) throws Throwable {
        System.out.println("AporteService.registrarAporte - Entidad: "
            + (solicitud != null ? solicitud.getIdEntidad() : null)
            + " - Tipo: " + (solicitud != null ? solicitud.getIdTipoAporte() : null)
            + " - Valor: " + (solicitud != null ? solicitud.getValor() : null));

        // ------------------------------------------------------------------
        // VALIDACIÓN
        // ------------------------------------------------------------------
        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": no se recibió el cuerpo de la solicitud");
        }
        if (solicitud.getIdEntidad() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEntidad es obligatorio");
        }
        if (solicitud.getIdTipoAporte() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idTipoAporte es obligatorio");
        }
        if (solicitud.getIdEmpresa() == null) {
            throw new IncomeException("idEmpresa es obligatorio: es la empresa contable sobre la que"
                + " se genera el asiento de la operación.");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }

        // find() (em.find) devuelve null si no existe, a diferencia de selectById
        Entidad entidad = entidadDaoService.find(new Entidad(), solicitud.getIdEntidad());
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el partícipe "
                + solicitud.getIdEntidad());
        }

        TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), solicitud.getIdTipoAporte());
        if (tipo == null) {
            throw new IncomeException(ERR_TIPO_APORTE_NO_VIGENTE + ": no existe el tipo de aporte "
                + solicitud.getIdTipoAporte());
        }
        if (tipo.getEstado() == null || tipo.getEstado() != 1L) {
            throw new IncomeException(ERR_TIPO_APORTE_NO_VIGENTE + ": el tipo de aporte "
                + tipo.getCodigo() + " (" + tipo.getNombre() + ") no está vigente");
        }

        double valor = redondear(solicitud.getValor() != null ? solicitud.getValor() : 0.0);
        if (valor <= 0.0) {
            throw new IncomeException(ERR_VALOR_INVALIDO + ": el valor del aporte debe ser mayor a cero");
        }

        LocalDate fecha = solicitud.getFechaTransaccion() != null
            ? solicitud.getFechaTransaccion() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": la fecha " + fecha + " es futura");
        }
        // Si es hoy se conserva la hora del reloj; si es una fecha pasada, el inicio del día
        LocalDateTime fechaHora = fecha.isEqual(LocalDate.now())
            ? LocalDateTime.now() : fecha.atStartOfDay();

        // ------------------------------------------------------------------
        // EJECUCIÓN
        // ------------------------------------------------------------------
        String glosa = truncarPorBytes("REGISTRO APORTE " + tipo.getNombre()
            + (solicitud.getObservacion() != null && !solicitud.getObservacion().trim().isEmpty()
                ? " - " + solicitud.getObservacion().trim() : ""), MAX_BYTES_GLOSA);

        // Fila POSITIVA y YA PAGADA: en el modelo vigente (Fase 1 del plan de devengo de
        // aportes, D1) el saldo del partícipe es SUM(valor) y toda fila nace pagada, así que
        // esta fila suma directo al saldo sin ningún abono posterior.
        Aporte aporte = new Aporte();
        aporte.setEntidad(entidad);
        aporte.setFilial(entidad.getFilial());
        aporte.setTipoAporte(tipo);
        aporte.setValor(valor);
        aporte.setValorPagado(valor);
        aporte.setSaldo(0.0);
        aporte.setEstado((long) EstadoCuotaPrestamo.PAGADA);
        aporte.setIdAsoprep(null);
        aporte.setFechaTransaccion(fechaHora);
        aporte.setPeriodoDevengo(solicitud.getPeriodoDevengo() != null
            ? solicitud.getPeriodoDevengo().withDayOfMonth(1) : fecha.withDayOfMonth(1));
        aporte.setTipoMovimiento((long) CrdTipoMovimientoAporte.AJUSTE_MANUAL);
        aporte.setGlosa(glosa);
        aporte.setUsuarioRegistro(solicitud.getUsuario());
        aporte.setFechaRegistro(LocalDateTime.now());
        // DAO directo: saveSingle forzaría estado = 1 y la fila volvería a ser visible
        // para el FIFO del proceso Petro.
        aporte = aporteDaoService.save(aporte, null);

        PagoAporte pagoAporte = new PagoAporte();
        pagoAporte.setAporte(aporte);
        pagoAporte.setFilial(entidad.getFilial());
        pagoAporte.setValor(valor);
        pagoAporte.setFechaContable(fechaHora);
        pagoAporte.setNumeroAsiento(null);
        pagoAporte.setConcepto(glosa);
        pagoAporte.setUsuarioRegistro(solicitud.getUsuario());
        pagoAporte.setFechaRegistro(LocalDateTime.now());
        pagoAporte.setEstado(1L);
        pagoAporte.setPagoPrestamo(null);
        pagoAporte.setRutaDocumentoRespaldo(solicitud.getRutaDocumentoRespaldo());
        pagoAporte = pagoAporteDaoService.save(pagoAporte, null);

        double saldoTipo = saldoAporteService.saldoPorEntidadYTipo(entidad.getCodigo(), tipo.getCodigo());

        ResultadoRegistroAporte resultado = new ResultadoRegistroAporte();
        resultado.setIdAporte(aporte.getCodigo());
        resultado.setIdPagoAporte(pagoAporte.getCodigo());
        resultado.setIdEntidad(entidad.getCodigo());
        resultado.setIdTipoAporte(tipo.getCodigo());
        resultado.setNombreTipoAporte(tipo.getNombre());
        resultado.setValor(valor);
        resultado.setSaldoTipoAporte(saldoTipo);
        resultado.setFechaTransaccion(fechaHora);

        System.out.println("  ✅ Aporte registrado - APRT " + aporte.getCodigo()
            + ", PGAP " + pagoAporte.getCodigo() + " - Valor: $" + valor
            + " - Nuevo saldo del tipo " + tipo.getCodigo() + ": $" + saldoTipo);

        return resultado;
    }

    @Override
    public Long reversarAporte(Long idAporte, String usuario, String motivo) throws Throwable {
        System.out.println("AporteService.reversarAporte - Aporte: " + idAporte);
        if (idAporte == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idAporte es obligatorio");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el motivo de la reversa es obligatorio");
        }

        Aporte original = aporteDaoService.find(new Aporte(), idAporte);
        if (original == null) {
            throw new IncomeException(ERR_APORTE_NO_ENCONTRADO + ": no existe el aporte " + idAporte);
        }
        double valorOriginal = original.getValor() != null ? original.getValor() : 0.0;
        if (valorOriginal < 0) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el aporte " + idAporte
                + " ya es una fila de reverso (valor negativo); no se puede reversar de nuevo");
        }

        String glosa = truncarPorBytes("REVERSO APORTE " + idAporte + " - " + motivo.trim(), MAX_BYTES_GLOSA);
        LocalDateTime ahora = LocalDateTime.now();

        Aporte reverso = new Aporte();
        reverso.setEntidad(original.getEntidad());
        reverso.setFilial(original.getFilial());
        reverso.setTipoAporte(original.getTipoAporte());
        reverso.setValor(-valorOriginal);
        reverso.setValorPagado(0.0);
        reverso.setSaldo(0.0);
        reverso.setEstado((long) EstadoCuotaPrestamo.PAGADA);
        reverso.setIdAsoprep(null);
        reverso.setFechaTransaccion(ahora);
        reverso.setPeriodoDevengo(original.getPeriodoDevengo());
        reverso.setTipoMovimiento((long) CrdTipoMovimientoAporte.REVERSO);
        reverso.setGlosa(glosa);
        reverso.setUsuarioRegistro(usuario.trim());
        reverso.setFechaRegistro(ahora);
        // DAO directo: saveSingle forzaría estado = 1 y la fila volvería a ser visible para
        // el FIFO del proceso Petro.
        reverso = aporteDaoService.save(reverso, null);

        PagoAporte pagoReverso = new PagoAporte();
        pagoReverso.setAporte(reverso);
        pagoReverso.setFilial(original.getFilial());
        pagoReverso.setValor(valorOriginal);
        pagoReverso.setFechaContable(ahora);
        pagoReverso.setNumeroAsiento(null);
        pagoReverso.setConcepto(glosa);
        pagoReverso.setUsuarioRegistro(usuario.trim());
        pagoReverso.setFechaRegistro(ahora);
        pagoReverso.setEstado(1L);
        pagoReverso.setPagoPrestamo(null);
        pagoAporteDaoService.save(pagoReverso, null);

        System.out.println("  ↩️ Aporte " + idAporte + " reversado con APRT " + reverso.getCodigo()
            + " (valor -$" + valorOriginal + ")");
        return reverso.getCodigo();
    }

    // ========================================================================
    // Jubilación — traslado a pensión complementaria (J2/J3) + cambio de estado
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public com.saa.ejb.crd.service.dto.ResultadoJubilacion procesarJubilacion(Long idEntidad, String usuario,
            LocalDate fecha, Long idEmpresa) throws Throwable {
        System.out.println("AporteService.procesarJubilacion - Entidad: " + idEntidad + " - Usuario: " + usuario);

        if (idEntidad == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEntidad es obligatorio");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }
        if (idEmpresa == null) {
            throw new IncomeException("idEmpresa es obligatorio: es la empresa contable sobre la que"
                + " se genera el asiento de la jubilación.");
        }

        Entidad entidad = entidadDaoService.find(new Entidad(), idEntidad);
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el partícipe " + idEntidad);
        }

        // Elegible solo desde ACTIVO o ACTIVO_EN_MORA — no desde un estado ya terminal
        // (jubilado en cualquiera de sus formas, cesante) ni desde NUEVO (todavía sin
        // reportar en el G41).
        long estadoActual = entidad.getIdEstado() != null ? entidad.getIdEstado() : -1L;
        if (estadoActual != EstadoParticipeEntidad.ACTIVO && estadoActual != EstadoParticipeEntidad.ACTIVO_EN_MORA) {
            throw new IncomeException(ERR_ESTADO_NO_ELEGIBLE + ": el partícipe " + idEntidad
                + " está en estado " + estadoActual + "; solo se puede procesar la jubilación desde"
                + " ACTIVO o ACTIVO EN MORA.");
        }

        LocalDate fechaEfectiva = fecha != null ? fecha : LocalDate.now();
        if (fechaEfectiva.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": la fecha " + fechaEfectiva + " es futura");
        }
        LocalDateTime fechaHora = fechaEfectiva.isEqual(LocalDate.now())
            ? LocalDateTime.now() : fechaEfectiva.atStartOfDay();

        double saldoCesantia = redondear(saldoAporteService.saldoPorEntidadYTipo(idEntidad, TIPO_APORTE_CESANTIA));
        double saldoJubilacion = redondear(saldoAporteService.saldoPorEntidadYTipo(idEntidad, TIPO_APORTE_JUBILACION));

        com.saa.ejb.crd.service.dto.ResultadoJubilacion resultado = new com.saa.ejb.crd.service.dto.ResultadoJubilacion();
        resultado.setIdEntidad(idEntidad);
        resultado.setFecha(fechaEfectiva);
        resultado.setValorCesantiaTrasladado(0.0);
        resultado.setValorJubilacionTrasladado(0.0);

        List<com.saa.ejb.crd.service.dto.MovimientoAporte> movimientos = new java.util.ArrayList<>();
        double totalTrasladado = 0.0;

        if (saldoCesantia > 0.01) {
            movimientos.add(crearMovimientoJubilacion(entidad, TIPO_APORTE_CESANTIA, -saldoCesantia,
                "JUBILACION - se jubiló por el total de cesantía a la fecha ($" + saldoCesantia + ")",
                fechaHora, usuario));
            resultado.setValorCesantiaTrasladado(saldoCesantia);
            totalTrasladado += saldoCesantia;
        }
        if (saldoJubilacion > 0.01) {
            movimientos.add(crearMovimientoJubilacion(entidad, TIPO_APORTE_JUBILACION, -saldoJubilacion,
                "JUBILACION - se jubiló por el total de jubilación a la fecha ($" + saldoJubilacion + ")",
                fechaHora, usuario));
            resultado.setValorJubilacionTrasladado(saldoJubilacion);
            totalTrasladado += saldoJubilacion;
        }
        totalTrasladado = redondear(totalTrasladado);

        if (totalTrasladado > 0.01) {
            movimientos.add(crearMovimientoJubilacion(entidad, TIPO_APORTE_PENSION_COMPLEMENTARIA, totalTrasladado,
                "JUBILACION - traslado de cesantía/jubilación a pensión complementaria",
                fechaHora, usuario));
        }
        resultado.setValorTotalTrasladado(totalTrasladado);
        resultado.setMovimientos(movimientos);

        // Paso 5: el estado cambia SIEMPRE, incluso si totalTrasladado es $0 (ver el javadoc
        // de la interfaz). saveSingle(entidad, usuario) sella la auditoría (sellarActualizacion).
        //
        // JUBILADO_COMPLEMENTARIO (3) es UNA de las TRES formas de jubilado del rubro
        // (EstadoParticipeEntidad: 3 complementario, 6 aportante, 7 pasivo) — elección
        // deliberada, la que pide el alcance de este frente (ALCANCE-EQUIPOS-CRD.md, Equipo 1),
        // no la única jubilación posible del catálogo. Si en el futuro este método necesita
        // distinguir entre las tres, la lógica de elegibilidad de arriba tiene que revisarse
        // también (hoy solo valida ACTIVO/ACTIVO_EN_MORA de entrada).
        entidad.setIdEstado(Long.valueOf(EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO));
        entidadService.saveSingle(entidad, usuario);
        resultado.setEstadoNuevo(Long.valueOf(EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO));

        // Asiento (§3.1 del levantamiento contable + plantilla alterno 29, confirmada contra
        // la base 2026-08-31). Gate de contabilidadActiva() primero.
        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("  Contabilidad de CRD INACTIVA: jubilación de la entidad " + idEntidad
                + " procesada sin generar el asiento de reclasificación.");
            resultado.setNumeroAsiento(null);
        } else {
            Long numeroAsiento = generarAsientoJubilacion(entidad, resultado.getValorCesantiaTrasladado(),
                resultado.getValorJubilacionTrasladado(), fechaEfectiva, usuario, idEmpresa);
            resultado.setNumeroAsiento(numeroAsiento);
        }

        System.out.println("  ✅ Jubilación procesada - Entidad " + idEntidad + " - Cesantía trasladada: $"
            + resultado.getValorCesantiaTrasladado() + " - Jubilación trasladada: $"
            + resultado.getValorJubilacionTrasladado() + " - Total a pensión complementaria: $"
            + totalTrasladado + " - Estado: JUBILADO_COMPLEMENTARIO");

        return resultado;
    }

    /** aux1 de la plantilla 29 — POSICIONALES, de ESTA plantilla y de ninguna otra. Ver el
     * javadoc completo en {@code PlantillasCredito.JUBILACION}. */
    private static final int AUX1_JUBILACION_DEBE_CESANTIA = 1;
    private static final int AUX1_JUBILACION_DEBE_JUBILACION = 2;
    private static final int AUX1_JUBILACION_HABER_PENSIONES_POR_PAGAR = 5;

    /**
     * Asiento de reclasificación de la jubilación — §3.1 del levantamiento contable, plantilla
     * alterno 29. D {@code 2.1.01.05.01} (aux1 1, cesantía) + D {@code 2.1.02.05.01} (aux1 2,
     * jubilación) → H {@code 2.3.01.10.03} (aux1 5, pensiones complementarias por pagar).
     *
     * <p><b>Usa solo aux1 1, 2 y 5 — NUNCA 3 ni 4, y no por indecisión: usarlas aquí
     * duplicaría un asiento.</b> La plantilla también resuelve aux1 3 ({@code 2.3.01.05.01},
     * liquidación cesantía) y 4 ({@code 2.3.01.10.01}, liquidación jubilación), pero este método
     * solo mueve el REMANENTE del partícipe — el cruce contra préstamos y la devolución en
     * efectivo son los otros dos destinos del flujo de jubilación (J1-J7,
     * {@code LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md} §4.b) y cada uno genera su propio asiento
     * por su propio proceso ({@code PrestamoService#pagarConAportes},
     * {@code DevolucionAporteService}). Aux1 3/4 de la plantilla 29 corresponden a esas dos
     * patas; si este método también las contabilizara, el cruce/la devolución y esta
     * reclasificación asentarían el mismo dinero dos veces. Por eso el traslado va ÍNTEGRO a
     * pensión complementaria (aux1 5): no es que quede "en liquidación" sin llegar a pensión
     * complementaria, es que lo que hubiera ido a liquidación ya se contabilizó por otro lado
     * antes de llegar acá. (Los rendimientos 12/24 tampoco entran en este traslado — decisión
     * CERRADA del usuario 2026-08-31, ver el javadoc de
     * {@link AporteService#procesarJubilacion}). Confirmado con el árbitro el 2026-08-31; el
     * asiento propio del pago mensual de la pensión (§3.1, devengo pensión/seguro) queda fuera
     * de este método y pendiente de encargo aparte.
     *
     * @return el código (PK) del asiento generado
     * @throws Throwable {@code IncomeException} si falta una línea en la plantilla, o si el
     *         cuadre contra el monto trasladado no da exacto (tolerancia $0.01)
     */
    private Long generarAsientoJubilacion(Entidad entidad, Double valorCesantiaTrasladado,
            Double valorJubilacionTrasladado, LocalDate fecha, String usuario, Long idEmpresa) throws Throwable {

        double saldoCesantia = valorCesantiaTrasladado != null ? valorCesantiaTrasladado : 0.0;
        double saldoJubilacion = valorJubilacionTrasladado != null ? valorJubilacionTrasladado : 0.0;
        double total = redondear(saldoCesantia + saldoJubilacion);

        if (total <= 0.01) {
            // Nada que contabilizar: el partícipe no tenía saldo en ninguna de las dos cuentas
            // (todo se cruzó/retiró en el paso 2). No es un error, es la ausencia esperada.
            System.out.println("  Jubilación de la entidad " + entidad.getCodigo()
                + " sin monto trasladado; no se genera asiento.");
            return null;
        }

        Long idPlantilla = plantillaService.codigoByAlterno(com.saa.rubros.PlantillasCredito.JUBILACION, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                + com.saa.rubros.PlantillasCredito.JUBILACION + " (CRD JUBILACION DE UN PARTICIPE) para"
                + " la empresa " + idEmpresa + ".");
        }

        String prefijo = "Jubilación - Entidad " + entidad.getCodigo();
        List<com.saa.model.cnt.DetalleAsiento> lineas = new java.util.ArrayList<>();
        double totalDebe = 0.0;

        if (saldoCesantia > 0.01) {
            lineas.add(lineaJubilacion(idPlantilla, AUX1_JUBILACION_DEBE_CESANTIA, saldoCesantia, true,
                prefijo + " - baja aporte cesantía"));
            totalDebe += saldoCesantia;
        }
        if (saldoJubilacion > 0.01) {
            lineas.add(lineaJubilacion(idPlantilla, AUX1_JUBILACION_DEBE_JUBILACION, saldoJubilacion, true,
                prefijo + " - baja aporte jubilación"));
            totalDebe += saldoJubilacion;
        }
        totalDebe = redondear(totalDebe);

        lineas.add(lineaJubilacion(idPlantilla, AUX1_JUBILACION_HABER_PENSIONES_POR_PAGAR, total, false,
            prefijo + " - pensión complementaria"));
        double totalHaber = total;

        // Cuadre contra el MONTO DE LA OPERACIÓN, no solo D=H (regla §4 de
        // PLAN-CIERRE-CONTABLE-TOTAL.md).
        if (Math.abs(redondear(totalDebe - total)) > TOLERANCIA
                || Math.abs(redondear(totalHaber - total)) > TOLERANCIA) {
            throw new IncomeException("El asiento de jubilación de la entidad " + entidad.getCodigo()
                + " no cuadra contra el monto trasladado: DEBE $" + totalDebe + ", HABER $" + totalHaber
                + ", trasladado $" + total + ". No se genera un asiento desbalanceado.");
        }

        com.saa.model.cnt.Asiento asiento = asientoContableService.generarAsiento(idEmpresa,
            com.saa.rubros.TipoAsientos.CREDITOS, fecha, prefijo + " - reclasificación por jubilación",
            usuario, lineas, Long.valueOf(com.saa.rubros.ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ Asiento de jubilación generado - Entidad " + entidad.getCodigo()
            + " - Asiento " + asiento.getCodigo() + " - $" + total);

        return asiento.getCodigo();
    }

    /** Línea de la plantilla 29 por aux1 explícito — posicional, no resuelto por tipo de aporte. */
    private com.saa.model.cnt.DetalleAsiento lineaJubilacion(Long idPlantilla, int aux1, double valor,
            boolean debe, String descripcion) throws Throwable {
        com.saa.model.cnt.DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + com.saa.rubros.PlantillasCredito.JUBILACION
                + " no tiene la línea aux1=" + aux1 + ".");
        }
        com.saa.model.cnt.PlanCuenta cuenta = linea.getPlanCuenta();
        com.saa.model.cnt.DetalleAsiento detalle = new com.saa.model.cnt.DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(debe ? redondear(valor) : 0.0);
        detalle.setValorHaber(debe ? 0.0 : redondear(valor));
        return detalle;
    }

    /**
     * Crea una fila de CRD.APRT + su PagoAporte para un movimiento de jubilación (J3): NEGATIVA
     * en cesantía/jubilación, POSITIVA en pensión complementaria. {@code tipoMovimiento =
     * JUBILACION} en los dos casos — el signo de {@code valor} decide si suma o resta.
     *
     * {@code periodoDevengo = null}: el traslado no corresponde al aporte esperado de ningún
     * mes en particular (mismo criterio que el remanente sin anticipo de
     * {@code DevolucionAporteServiceImpl}, D5 §2.4 del plan de devengo de aportes).
     */
    private com.saa.ejb.crd.service.dto.MovimientoAporte crearMovimientoJubilacion(Entidad entidad,
            long idTipoAporte, double valor, String glosaBase, LocalDateTime fechaHora, String usuario)
            throws Throwable {

        TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), idTipoAporte);
        if (tipo == null) {
            throw new IncomeException("No existe el tipo de aporte " + idTipoAporte
                + " en el catálogo CRD.TPAP — no se puede generar el movimiento de jubilación"
                + " de la entidad " + entidad.getCodigo() + ".");
        }

        String glosa = truncarPorBytes(glosaBase, MAX_BYTES_GLOSA);

        Aporte aporte = new Aporte();
        aporte.setEntidad(entidad);
        aporte.setFilial(entidad.getFilial());
        aporte.setTipoAporte(tipo);
        aporte.setValor(redondear(valor));
        aporte.setValorPagado(valor > 0 ? redondear(valor) : 0.0);
        aporte.setSaldo(0.0);
        aporte.setEstado((long) EstadoCuotaPrestamo.PAGADA);
        aporte.setIdAsoprep(null);
        aporte.setFechaTransaccion(fechaHora);
        aporte.setPeriodoDevengo(null);
        aporte.setTipoMovimiento((long) CrdTipoMovimientoAporte.JUBILACION);
        aporte.setGlosa(glosa);
        aporte.setUsuarioRegistro(usuario.trim());
        aporte.setFechaRegistro(LocalDateTime.now());
        // DAO directo: saveSingle forzaría estado = 1 (Estado.ACTIVO) en todo INSERT, pisando
        // el PAGADA(4) recién asignado — mismo motivo que registrarAporte/reversarAporte.
        aporte = aporteDaoService.save(aporte, null);

        PagoAporte pagoAporte = new PagoAporte();
        pagoAporte.setAporte(aporte);
        pagoAporte.setFilial(entidad.getFilial());
        pagoAporte.setValor(Math.abs(redondear(valor)));
        pagoAporte.setFechaContable(fechaHora);
        pagoAporte.setNumeroAsiento(null);
        pagoAporte.setConcepto(glosa);
        pagoAporte.setUsuarioRegistro(usuario.trim());
        pagoAporte.setFechaRegistro(LocalDateTime.now());
        pagoAporte.setEstado(1L);
        pagoAporte.setPagoPrestamo(null);
        pagoAporte = pagoAporteDaoService.save(pagoAporte, null);

        System.out.println("    💠 Movimiento de jubilación - APRT " + aporte.getCodigo()
            + " (tipo " + idTipoAporte + ", $" + redondear(valor) + "), PGAP " + pagoAporte.getCodigo());

        com.saa.ejb.crd.service.dto.MovimientoAporte movimiento = new com.saa.ejb.crd.service.dto.MovimientoAporte();
        movimiento.setIdAporte(aporte.getCodigo());
        movimiento.setIdTipoAporte(idTipoAporte);
        movimiento.setValor(redondear(valor));
        movimiento.setIdPagoAporte(pagoAporte.getCodigo());
        return movimiento;
    }

    /** Tipos de aporte que cubre el estado de cuenta: 9 jubilación, 11 cesantía. */
    private static final List<Long> TIPOS_APORTE_ESTADO_CUENTA = java.util.Arrays.asList(9L, 11L);

    @Override
    public EstadoCuentaAportesDTO estadoCuenta(Long idEntidad, LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("AporteService.estadoCuenta - Entidad: " + idEntidad
            + " - Desde: " + desde + " - Hasta: " + hasta);

        if (idEntidad == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEntidad es obligatorio");
        }
        if (desde == null || hasta == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": desde y hasta son obligatorios");
        }
        if (hasta.isBefore(desde)) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": hasta no puede ser anterior a desde");
        }

        Entidad entidad = entidadDaoService.find(new Entidad(), idEntidad);
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el partícipe " + idEntidad);
        }

        LocalDate desdeMes = desde.withDayOfMonth(1);
        LocalDate hastaMes = hasta.withDayOfMonth(1);
        LocalDate mesActual = LocalDate.now().withDayOfMonth(1);

        // aportado(m,tipo) agregado — UNA sola consulta para todo el rango (sin selectAll()).
        java.util.Map<java.util.AbstractMap.SimpleEntry<LocalDate, Long>, Double> aportadoPorClave =
            new java.util.HashMap<>();
        List<Object[]> agregados = aporteDaoService.sumValorPorEntidadTipoYRangoDevengo(idEntidad, desdeMes, hastaMes);
        if (agregados != null) {
            for (Object[] fila : agregados) {
                LocalDate periodo = (LocalDate) fila[0];
                Long idTipo = (Long) fila[1];
                Double suma = (Double) fila[2];
                if (periodo != null && idTipo != null) {
                    aportadoPorClave.put(new java.util.AbstractMap.SimpleEntry<>(periodo, idTipo),
                        suma != null ? suma : 0.0);
                }
            }
        }

        // tipoMovimientoTexto sale del rubro 235, no de un switch: una sola consulta para
        // sus (a lo sumo) seis detalles.
        java.util.Map<Long, String> textoTipoMovimiento = new java.util.HashMap<>();
        List<DetalleRubro> detallesTipoMovimiento =
            detalleRubroDaoService.selectByCodigoAlternoRubro(Rubros.CRD_TIPO_MOVIMIENTO_APORTE, 1L);
        if (detallesTipoMovimiento != null) {
            for (DetalleRubro detalle : detallesTipoMovimiento) {
                if (detalle.getCodigoAlterno() != null) {
                    textoTipoMovimiento.put(detalle.getCodigoAlterno(), detalle.getDescripcion());
                }
            }
        }

        // Movimientos crudos — la segunda y última consulta. Incluye los de periodo NULL
        // (histórico sin backfillear / retiros de saldo): esos nunca se esconden.
        java.util.Map<Long, String> nombreTipoAporte = new java.util.HashMap<>();
        java.util.Map<java.util.AbstractMap.SimpleEntry<LocalDate, Long>, List<MovimientoEstadoCuentaDTO>>
            movimientosPorClave = new java.util.LinkedHashMap<>();

        List<Object[]> movimientosCrudos =
            aporteDaoService.selectMovimientosPorEntidadYRangoDevengo(idEntidad, desdeMes, hastaMes);
        if (movimientosCrudos != null) {
            for (Object[] fila : movimientosCrudos) {
                LocalDate periodo = (LocalDate) fila[0];
                Long idTipo = (Long) fila[1];
                Long idAporte = (Long) fila[2];
                LocalDateTime fechaTransaccion = (LocalDateTime) fila[3];
                Double valor = (Double) fila[4];
                Long tipoMovimiento = (Long) fila[5];
                String glosa = (String) fila[6];

                if (idTipo != null && !nombreTipoAporte.containsKey(idTipo)) {
                    TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), idTipo);
                    nombreTipoAporte.put(idTipo, tipo != null ? tipo.getNombre() : null);
                }

                MovimientoEstadoCuentaDTO movimiento = new MovimientoEstadoCuentaDTO();
                movimiento.setIdAporte(idAporte);
                movimiento.setFechaTransaccion(fechaTransaccion);
                movimiento.setValor(valor);
                movimiento.setTipoMovimiento(tipoMovimiento);
                movimiento.setTipoMovimientoTexto(
                    tipoMovimiento != null ? textoTipoMovimiento.get(tipoMovimiento) : null);
                movimiento.setGlosa(glosa);

                java.util.AbstractMap.SimpleEntry<LocalDate, Long> clave =
                    new java.util.AbstractMap.SimpleEntry<>(periodo, idTipo);
                movimientosPorClave.computeIfAbsent(clave, k -> new java.util.ArrayList<>()).add(movimiento);
            }
        }

        java.time.format.DateTimeFormatter formatoPeriodo = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        List<PeriodoEstadoCuentaDTO> periodos = new java.util.ArrayList<>();
        double totalFaltante = 0.0;

        // Recorrido del CALENDARIO (todo mes en [desdeMes,hastaMes] x {9,11}), no sólo los
        // meses con movimientos: si nada se aportó ese mes, igual hay que poder mostrar
        // "SIN APORTE" cuando algo se esperaba. Sólo se descarta el mes cuando ni se esperó
        // ni se aportó nada.
        for (LocalDate mes = desdeMes; !mes.isAfter(hastaMes); mes = mes.plusMonths(1)) {
            for (Long idTipo : TIPOS_APORTE_ESTADO_CUENTA) {
                java.util.AbstractMap.SimpleEntry<LocalDate, Long> clave =
                    new java.util.AbstractMap.SimpleEntry<>(mes, idTipo);

                double esperado = vigenciaContratoService.esperadoPorEntidad(idEntidad, idTipo, mes);
                double aportado = aportadoPorClave.getOrDefault(clave, 0.0);
                if (esperado <= 0.01 && aportado <= 0.01) {
                    continue;
                }
                double faltante = Math.max(0.0, esperado - aportado);

                String estado;
                if (mes.isAfter(mesActual) && aportado > 0.01) {
                    estado = "ANTICIPADO";
                } else if (faltante <= 0.01) {
                    estado = "COMPLETO";
                } else if (aportado > 0.01) {
                    estado = "PARCIAL";
                } else {
                    estado = "SIN APORTE";
                }

                PeriodoEstadoCuentaDTO periodoDto = new PeriodoEstadoCuentaDTO();
                periodoDto.setPeriodo(mes.format(formatoPeriodo));
                periodoDto.setIdTipoAporte(idTipo);
                periodoDto.setNombreTipoAporte(nombreTipoAporte.get(idTipo));
                periodoDto.setEsperado(redondear(esperado));
                periodoDto.setAportado(redondear(aportado));
                periodoDto.setFaltante(redondear(faltante));
                periodoDto.setEstado(estado);
                periodoDto.setMovimientos(
                    movimientosPorClave.getOrDefault(clave, java.util.Collections.emptyList()));

                totalFaltante += faltante;
                periodos.add(periodoDto);
            }
        }

        // Grupo(s) "SIN PERIODO" — uno por tipo que tenga movimientos sin devengo. Nunca se
        // esconden, aunque no exista ningún mes del calendario que los reclame.
        for (java.util.Map.Entry<java.util.AbstractMap.SimpleEntry<LocalDate, Long>,
                List<MovimientoEstadoCuentaDTO>> entrada : movimientosPorClave.entrySet()) {
            if (entrada.getKey().getKey() != null) {
                continue;
            }
            Long idTipo = entrada.getKey().getValue();
            double aportadoSinPeriodo = 0.0;
            for (MovimientoEstadoCuentaDTO movimiento : entrada.getValue()) {
                aportadoSinPeriodo += movimiento.getValor() != null ? movimiento.getValor() : 0.0;
            }

            PeriodoEstadoCuentaDTO periodoDto = new PeriodoEstadoCuentaDTO();
            periodoDto.setPeriodo(null);
            periodoDto.setIdTipoAporte(idTipo);
            periodoDto.setNombreTipoAporte(nombreTipoAporte.get(idTipo));
            periodoDto.setEsperado(0.0);
            periodoDto.setAportado(redondear(aportadoSinPeriodo));
            periodoDto.setFaltante(0.0);
            periodoDto.setEstado("SIN PERIODO");
            periodoDto.setMovimientos(entrada.getValue());
            periodos.add(periodoDto);
        }

        EstadoCuentaAportesDTO resultado = new EstadoCuentaAportesDTO();
        resultado.setIdEntidad(entidad.getCodigo());
        resultado.setIdentificacion(entidad.getNumeroIdentificacion());
        resultado.setRazonSocial(entidad.getRazonSocial());
        resultado.setPeriodos(periodos);
        resultado.setTotalFaltante(redondear(totalFaltante));

        System.out.println("  Estado de cuenta - Entidad: " + idEntidad + " - Periodos: " + periodos.size()
            + " - Total faltante: $" + resultado.getTotalFaltante());

        return resultado;
    }

    /** Recorta el texto para que su representación UTF-8 quepa en la columna. */
    private String truncarPorBytes(String texto, int maxBytes) {
        if (texto == null || texto.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return texto;
        }
        int hasta = texto.length();
        while (hasta > 0 && texto.substring(0, hasta).getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            hasta--;
        }
        return texto.substring(0, hasta);
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
