package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.dto.ResultadoRegistroAporte;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAporte;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.TipoAporte;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;

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

        // Fila POSITIVA y YA PAGADA: sube el saldo disponible y queda fuera del FIFO petro
        // (selectMinAporteConSaldo exige saldo > 0.01 y estado PARCIAL).
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
