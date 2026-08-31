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
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class AporteServiceImpl implements AporteService {

    /** Longitud máxima en BYTES de APRT.APRTGLSA (VARCHAR2(2000)) */
    private static final int MAX_BYTES_GLOSA = 2000;

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
