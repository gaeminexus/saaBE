package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Proyección de {@link OrdenBeneficioSocial} para la bandeja de órdenes
 * ({@code GET /rest/odbs/listar}) — NO es la entidad completa. Ver
 * docs/logica-negocio/rhh/API-PAGO-BENEFICIOS-SOCIALES.md #1.3bis y
 * docs/estandar/ESTANDAR-PROYECCIONES-EN-LISTADOS.md. Mismo precedente que
 * {@link com.saa.model.cxp.PagoPorAprobar}.
 *
 * <p>Los campos crudos (todo salvo los tres {@code *Texto}) se arman con
 * {@code select new} en {@code OrdenBeneficioSocialDaoServiceImpl}, con
 * {@code left join o.pagoProgramado} para no perder las órdenes que todavía no se
 * enviaron a tesorería (esas filas quedan con {@code idPagoProgramado}/{@code estadoPago}
 * en null). Los tres {@code *Texto} los completa
 * {@code OrdenBeneficioSocialServiceImpl} después de la consulta: no son expresables en el
 * {@code select new} sin un join adicional al catálogo de rubros, y son enumeraciones
 * chicas y fijas (ver el Javadoc de {@code OrdenBeneficioSocialServiceImpl#textoTipoBeneficio}
 * y hermanos para la fuente exacta de cada texto).</p>
 */
public class OrdenBeneficioSocialResumen implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idOrden;
    private String numero;
    private Long tipoBeneficio;
    private String tipoBeneficioTexto;
    private Integer anio;
    private Long region;
    private Double total;
    private Integer numeroEmpleados;
    private LocalDate fechaEmision;
    private LocalDate fechaPago;
    private Long estado;
    private String estadoTexto;
    private Long idPagoProgramado;
    private Long estadoPago;
    private String estadoPagoTexto;
    private Long idAsiento;

    public OrdenBeneficioSocialResumen() {
    }

    /**
     * Constructor de proyección JPQL: exactamente los campos crudos que arma el
     * {@code select new} del DAO, en el mismo orden. Los tres {@code *Texto} se
     * completan después, en el service.
     */
    public OrdenBeneficioSocialResumen(Long idOrden, String numero, Long tipoBeneficio, Integer anio,
            Long region, Double total, Integer numeroEmpleados, LocalDate fechaEmision,
            LocalDate fechaPago, Long estado, Long idPagoProgramado, Long estadoPago, Long idAsiento) {
        this.idOrden = idOrden;
        this.numero = numero;
        this.tipoBeneficio = tipoBeneficio;
        this.anio = anio;
        this.region = region;
        this.total = total;
        this.numeroEmpleados = numeroEmpleados;
        this.fechaEmision = fechaEmision;
        this.fechaPago = fechaPago;
        this.estado = estado;
        this.idPagoProgramado = idPagoProgramado;
        this.estadoPago = estadoPago;
        this.idAsiento = idAsiento;
    }

    public Long getIdOrden() { return idOrden; }
    public void setIdOrden(Long idOrden) { this.idOrden = idOrden; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public Long getTipoBeneficio() { return tipoBeneficio; }
    public void setTipoBeneficio(Long tipoBeneficio) { this.tipoBeneficio = tipoBeneficio; }

    public String getTipoBeneficioTexto() { return tipoBeneficioTexto; }
    public void setTipoBeneficioTexto(String tipoBeneficioTexto) { this.tipoBeneficioTexto = tipoBeneficioTexto; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public Long getRegion() { return region; }
    public void setRegion(Long region) { this.region = region; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Integer getNumeroEmpleados() { return numeroEmpleados; }
    public void setNumeroEmpleados(Integer numeroEmpleados) { this.numeroEmpleados = numeroEmpleados; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getEstadoTexto() { return estadoTexto; }
    public void setEstadoTexto(String estadoTexto) { this.estadoTexto = estadoTexto; }

    public Long getIdPagoProgramado() { return idPagoProgramado; }
    public void setIdPagoProgramado(Long idPagoProgramado) { this.idPagoProgramado = idPagoProgramado; }

    public Long getEstadoPago() { return estadoPago; }
    public void setEstadoPago(Long estadoPago) { this.estadoPago = estadoPago; }

    public String getEstadoPagoTexto() { return estadoPagoTexto; }
    public void setEstadoPagoTexto(String estadoPagoTexto) { this.estadoPagoTexto = estadoPagoTexto; }

    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
}
