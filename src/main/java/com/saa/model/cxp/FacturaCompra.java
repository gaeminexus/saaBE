package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;

import jakarta.persistence.*;

/**
 * Entity FacturaCompra.
 * Almacena los datos de la factura de compra (tabla pgs.fctc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FCTC", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "FacturaCompraAll", query = "select e from FacturaCompra e"),
	@NamedQuery(name = "FacturaCompraId", query = "select e from FacturaCompra e where e.id = :id")
})
public class FacturaCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Basic @Column(name = "TIPOCOMPROBANTE", length = 10)
	private String tipoComprobante;

	@ManyToOne @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;

	@ManyToOne @JoinColumn(name = "TITULAR", referencedColumnName = "TTLRCDGO")
	private Titular titular;

	@Basic @Column(name = "TIPODOC", length = 10)
	private String tipoDoc;

	@Basic @Column(name = "NUMERO", length = 100)
	private String numero;

	@Basic @Column(name = "NUMESTABLECIMIENTO", length = 500)
	private String numEstablecimiento;

	@Basic @Column(name = "NUMPTOEMISION", length = 500)
	private String numPtoEmision;

	@Basic @Column(name = "SECUENCIAL", length = 1000)
	private String secuencial;

	@Basic @Column(name = "AMBIENTE")
	private Long ambiente;

	@Basic @Column(name = "CLAVE", length = 100)
	private String clave;

	@Basic @Column(name = "FECHA")
	private LocalDateTime fecha;

	@Basic @Column(name = "OBSERVACION", length = 2000)
	private String observacion;

	@Basic @Column(name = "SUBTOTAL")
	private Double subtotal;

	@Basic @Column(name = "SUBCERO")
	private Double subcero;

	@Basic @Column(name = "SUBTOTAL5")
	private Double subtotal5;

	@Basic @Column(name = "SUBTOTAL8")
	private Double subtotal8;

	@Basic @Column(name = "PIVA")
	private Double pIVA;

	@Basic @Column(name = "VIVA")
	private Double vIVA;

	@Basic @Column(name = "VIVA5")
	private Double vIVA5;

	@Basic @Column(name = "VIVA8")
	private Double vIVA8;

	@Basic @Column(name = "VICE")
	private Double vICE;

	@Basic @Column(name = "VIRBPNR")
	private Double vIRBPNR;

	@Basic @Column(name = "DESCUENTO")
	private Double descuento;

	@Basic @Column(name = "PORDESCUENTO")
	private Double porDescuento;

	@Basic @Column(name = "PROPINA")
	private Double propina;

	@Basic @Column(name = "SUBSIDIO")
	private Double subsidio;

	@Basic @Column(name = "TOTALSINSUB")
	private Double totalSinSub;

	@Basic @Column(name = "AHORROSUB")
	private Double ahorroSub;

	@Basic @Column(name = "TOTAL")
	private Double total;

	@Basic @Column(name = "PTOEMISION")
	private Long ptoEmision;

	@ManyToOne @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;

	@Basic @Column(name = "PATHGEN", length = 2000)
	private String pathGen;

	@Basic @Column(name = "AUTORIZACION", length = 1000)
	private String autorizacion;

	@Basic @Column(name = "FECHAAUTORIZACION")
	private LocalDateTime fechaAutorizacion;

	@Basic @Column(name = "FORMAPAGO")
	private Long formaPago;

	@Basic @Column(name = "ESTADO")
	private Long estado;

        @Basic @Column(name = "ESTADOEMISION")
        private Long estadoEmision;

        /**
         * Estado de pago de la factura de compra.
         * 1 = Pendiente (sin ningún pago aplicado)
         * 2 = Pagada parcialmente
         * 3 = Pagada totalmente
         * Se recalcula automáticamente al insertar o reversar una AplicacionPagoCxp.
         */
        @Basic @Column(name = "FCTCEPAG")
        private Long estadoPago;

        /**
         * Asiento contable generado al contabilizar la factura de compra.
         */
        @ManyToOne
        @JoinColumn(name = "ASIENTO", referencedColumnName = "ASNTCDGO")
        private Asiento asiento;

        public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getTipoComprobante() { return tipoComprobante; }
	public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
	public Empresa getEmpresa() { return empresa; }
	public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
	public Titular getTitular() { return titular; }
	public void setTitular(Titular titular) { this.titular = titular; }
	public String getTipoDoc() { return tipoDoc; }
	public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }
	public String getNumero() { return numero; }
	public void setNumero(String numero) { this.numero = numero; }
	public String getNumEstablecimiento() { return numEstablecimiento; }
	public void setNumEstablecimiento(String numEstablecimiento) { this.numEstablecimiento = numEstablecimiento; }
	public String getNumPtoEmision() { return numPtoEmision; }
	public void setNumPtoEmision(String numPtoEmision) { this.numPtoEmision = numPtoEmision; }
	public String getSecuencial() { return secuencial; }
	public void setSecuencial(String secuencial) { this.secuencial = secuencial; }
	public Long getAmbiente() { return ambiente; }
	public void setAmbiente(Long ambiente) { this.ambiente = ambiente; }
	public String getClave() { return clave; }
	public void setClave(String clave) { this.clave = clave; }
	public LocalDateTime getFecha() { return fecha; }
	public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }
	public Double getSubtotal() { return subtotal; }
	public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
	public Double getSubcero() { return subcero; }
	public void setSubcero(Double subcero) { this.subcero = subcero; }
	public Double getSubtotal5() { return subtotal5; }
	public void setSubtotal5(Double subtotal5) { this.subtotal5 = subtotal5; }
	public Double getSubtotal8() { return subtotal8; }
	public void setSubtotal8(Double subtotal8) { this.subtotal8 = subtotal8; }
	public Double getpIVA() { return pIVA; }
	public void setpIVA(Double pIVA) { this.pIVA = pIVA; }
	public Double getvIVA() { return vIVA; }
	public void setvIVA(Double vIVA) { this.vIVA = vIVA; }
	public Double getvIVA5() { return vIVA5; }
	public void setvIVA5(Double vIVA5) { this.vIVA5 = vIVA5; }
	public Double getvIVA8() { return vIVA8; }
	public void setvIVA8(Double vIVA8) { this.vIVA8 = vIVA8; }
	public Double getvICE() { return vICE; }
	public void setvICE(Double vICE) { this.vICE = vICE; }
	public Double getvIRBPNR() { return vIRBPNR; }
	public void setvIRBPNR(Double vIRBPNR) { this.vIRBPNR = vIRBPNR; }
	public Double getDescuento() { return descuento; }
	public void setDescuento(Double descuento) { this.descuento = descuento; }
	public Double getPorDescuento() { return porDescuento; }
	public void setPorDescuento(Double porDescuento) { this.porDescuento = porDescuento; }
	public Double getPropina() { return propina; }
	public void setPropina(Double propina) { this.propina = propina; }
	public Double getSubsidio() { return subsidio; }
	public void setSubsidio(Double subsidio) { this.subsidio = subsidio; }
	public Double getTotalSinSub() { return totalSinSub; }
	public void setTotalSinSub(Double totalSinSub) { this.totalSinSub = totalSinSub; }
	public Double getAhorroSub() { return ahorroSub; }
	public void setAhorroSub(Double ahorroSub) { this.ahorroSub = ahorroSub; }
	public Double getTotal() { return total; }
	public void setTotal(Double total) { this.total = total; }
	public Long getPtoEmision() { return ptoEmision; }
	public void setPtoEmision(Long ptoEmision) { this.ptoEmision = ptoEmision; }
	public Usuario getUsuario() { return usuario; }
	public void setUsuario(Usuario usuario) { this.usuario = usuario; }
	public String getPathGen() { return pathGen; }
	public void setPathGen(String pathGen) { this.pathGen = pathGen; }
	public String getAutorizacion() { return autorizacion; }
	public void setAutorizacion(String autorizacion) { this.autorizacion = autorizacion; }
	public LocalDateTime getFechaAutorizacion() { return fechaAutorizacion; }
	public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) { this.fechaAutorizacion = fechaAutorizacion; }
	public Long getFormaPago() { return formaPago; }
	public void setFormaPago(Long formaPago) { this.formaPago = formaPago; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
        public Long getEstadoEmision() { return estadoEmision; }
        public void setEstadoEmision(Long estadoEmision) { this.estadoEmision = estadoEmision; }
        public Long getEstadoPago() { return estadoPago; }
        public void setEstadoPago(Long estadoPago) { this.estadoPago = estadoPago; }
        public Asiento getAsiento() { return asiento; }
        public void setAsiento(Asiento asiento) { this.asiento = asiento; }

        // ─── Campos de reembolso de gastos (§3 RMBF) ──────────────────────────

        /** Es factura de reembolso de gastos: 0=No 1=Si (FCTCESRM). */
        @Basic @Column(name = "FCTCESRM")
        private Long esReembolso;

        /** codDocReembolso del XML (tabla 3 SRI, normalmente 41) (FCTCCDRM). */
        @Basic @Column(name = "FCTCCDRM", length = 2)
        private String codDocReembolso;

        /** totalComprobantesReembolso (del XML o recalculado desde PGS.RMBF) (FCTCTCRM). */
        @Basic @Column(name = "FCTCTCRM")
        private Double totalComprobantesReembolso;

        /** totalBaseImponibleReembolso (FCTCTBRM). */
        @Basic @Column(name = "FCTCTBRM")
        private Double totalBaseImponibleReembolso;

        /** totalImpuestoReembolso (FCTCTIRM). */
        @Basic @Column(name = "FCTCTIRM")
        private Double totalImpuestoReembolso;

        /**
         * Sustento tributario SRI (Tabla 5 del ATS) resuelto para esta factura de compra
         * (FCTCCSUS). Cadena de dos digitos, con cero a la izquierda ("01".."15","00") -
         * ver {@link com.saa.rubros.SustentoTributarioSri}. Es el valor RESUELTO y guardado:
         * no se recalcula al generar el ATS, porque el grupo de producto de sus lineas pudo
         * cambiar despues. Nulo mientras no se pueda resolver (factura sin lineas, o lineas
         * cuyos grupos de producto no tienen sustento por defecto configurado).
         */
        @Basic @Column(name = "FCTCCSUS", length = 2)
        private String sustentoTributario;

        /**
         * ATS (campo {@code fechaRegistro}), fecha de REGISTRO CONTABLE de la factura,
         * distinta de {@link #fecha} (fecha de emisión del comprobante). Nula mientras no se
         * capture; ver docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §3.3/§4.3 fase 3.
         */
        @Basic @Column(name = "FCTCFCRG")
        private java.time.LocalDate fechaRegistroContable;

        /**
         * Auditoría de anulación (2026-08-28) — mismo patrón que {@code Factura.motivoAnulacion}
         * del lado venta. Nulo = nunca anulada (o anulada antes de este cambio, sin auditoría).
         * Ver docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10.3.
         */
        @Basic @Column(name = "FCTCMTAN", length = 1000)
        private String motivoAnulacion;

        @Basic @Column(name = "FCTCFCAN")
        private java.time.LocalDateTime fechaAnulacion;

        @Basic @Column(name = "FCTCUSAN", length = 200)
        private String usuarioAnulacion;

        public Long getEsReembolso() { return esReembolso; }
        public void setEsReembolso(Long esReembolso) { this.esReembolso = esReembolso; }
        public String getCodDocReembolso() { return codDocReembolso; }
        public void setCodDocReembolso(String codDocReembolso) { this.codDocReembolso = codDocReembolso; }
        public Double getTotalComprobantesReembolso() { return totalComprobantesReembolso; }
        public void setTotalComprobantesReembolso(Double totalComprobantesReembolso) { this.totalComprobantesReembolso = totalComprobantesReembolso; }
        public Double getTotalBaseImponibleReembolso() { return totalBaseImponibleReembolso; }
        public void setTotalBaseImponibleReembolso(Double totalBaseImponibleReembolso) { this.totalBaseImponibleReembolso = totalBaseImponibleReembolso; }
        public Double getTotalImpuestoReembolso() { return totalImpuestoReembolso; }
        public void setTotalImpuestoReembolso(Double totalImpuestoReembolso) { this.totalImpuestoReembolso = totalImpuestoReembolso; }
        public String getSustentoTributario() { return sustentoTributario; }
        public void setSustentoTributario(String sustentoTributario) { this.sustentoTributario = sustentoTributario; }
        public java.time.LocalDate getFechaRegistroContable() { return fechaRegistroContable; }
        public void setFechaRegistroContable(java.time.LocalDate fechaRegistroContable) { this.fechaRegistroContable = fechaRegistroContable; }
        public String getMotivoAnulacion() { return motivoAnulacion; }
        public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
        public java.time.LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
        public void setFechaAnulacion(java.time.LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
        public String getUsuarioAnulacion() { return usuarioAnulacion; }
        public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }
}
