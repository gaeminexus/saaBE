package com.saa.model.cxc;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Entidad que representa un Facturador (Negocio que emite la factura)
 * Tabla: CBR.FCDR
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FCDR", schema = "CBR")
@SequenceGenerator(name = "SQ_FCDR", sequenceName = "CBR.SQ_FCDR", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "FacturadorAll", query = "select e from Facturador e"),
	@NamedQuery(name = "FacturadorId", query = "select e from Facturador e where e.id = :id")
})
public class Facturador implements Serializable {

	/**
	 * ID del facturador
	 */
	@Basic
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Número de documento (RUC/Cédula)
	 */
	@Basic
	@Column(name = "NUMDOC", length = 50)
	private String numDoc;
	
	/**
	 * Nombre del facturador
	 */
	@Basic
	@Column(name = "NOMBRE", length = 1000)
	private String nombre;
	
	/**
	 * Razón social
	 */
	@Basic
	@Column(name = "RAZONSOCIAL", length = 1000)
	private String razonSocial;
	
	/**
	 * Nombre comercial
	 */
	@Basic
	@Column(name = "NOMBRECOMERCIAL", length = 500)
	private String nombreComercial;
	
	/**
	 * Email
	 */
	@Basic
	@Column(name = "MAIL", length = 200)
	private String mail;
	
	/**
	 * Teléfono
	 */
	@Basic
	@Column(name = "TELEFONO", length = 45)
	private String telefono;
	
	/**
	 * Dirección
	 */
	@Basic
	@Column(name = "DIRECCION", length = 1000)
	private String direccion;
	
	/**
	 * Fecha de creación
	 */
	@Basic
	@Column(name = "CREADO")
	private LocalDateTime creado;
	
	/**
	 * Logo
	 */
	@Basic
	@Column(name = "LOGO", length = 1000)
	private String logo;
	
	/**
	 * Firma electrónica
	 */
	@Basic
	@Column(name = "FIRMA", length = 1000)
	private String firma;
	
	/**
	 * Clave de la firma
	 */
	@Basic
	@Column(name = "CLAVEFIRMA", length = 500)
	private String claveFirma;
	
	/**
	 * Empresa firma
	 */
	@Basic
	@Column(name = "EMPRESAFIRMA")
	private Long empresaFirma;
	
	/**
	 * Código de 8 dígitos para formar clave para factura
	 */
	@Basic
	@Column(name = "CODCLAVE", length = 100)
	private String codClave;
	
	/**
	 * Obligado a llevar contabilidad
	 */
	@Basic
	@Column(name = "CONTABILIDAD")
	private Long contabilidad;
	
	/**
	 * Número de resolución de agente de retención
	 */
	@Basic
	@Column(name = "AGENTERETENCION", length = 1000)
	private String agenteRetencion;
	
	/**
	 * Número de resolución de contribuyente especial
	 */
	@Basic
	@Column(name = "CONTRIBUYENTEESPECIAL", length = 1000)
	private String contribuyenteEspecial;
	
	/**
	 * Código de artesano
	 */
	@Basic
	@Column(name = "ARTESANO", length = 1000)
	private String artesano;
	
	/**
	 * Régimen de Micro Empresa (1=sí, 0=no)
	 */
	@Basic
	@Column(name = "MICROEMPRESA")
	private Long microEmpresa;
	
	/**
	 * Contribuyente Régimen RIMPE
	 */
	@Basic
	@Column(name = "RIMPE")
	private Long rimpe;
	
	/**
	 * Contribuyente Negocio Popular Régimen RIMPE
	 */
	@Basic
	@Column(name = "POPULARRIMPE")
	private Long popularRimpe;
	
	/**
	 * Registro Turístico
	 */
	@Basic
	@Column(name = "TURISTICO", length = 1000)
	private String turistico;
	
	/**
	 * Fecha de inicio del servicio
	 */
	@Basic
	@Column(name = "INICIA")
	private LocalDateTime inicia;
	
	/**
	 * Fecha de vencimiento del servicio
	 */
	@Basic
	@Column(name = "VENCE")
	private LocalDateTime vence;
	
	/**
	 * Número de documentos emitidos. Se reinicia con la renovación del servicio
	 */
	@Basic
	@Column(name = "DOCEMITIDOS")
	private Long docEmitidos;
	
	/**
	 * Número total de documentos contratados
	 */
	@Basic
	@Column(name = "DOCPERMITIDOS")
	private Long docPermitidos;
	
	/**
	 * Indica si imprime los códigos de los productos en las facturas (1=sí, 0=no)
	 */
	@Basic
	@Column(name = "IMPCODPROD")
	private Long impCodProd;
	
	/**
	 * Indica si tiene inventario (0=No, 1=Sí)
	 */
	@Basic
	@Column(name = "INVENTARIO")
	private Long inventario;
	
	/**
	 * Indica si los puntos de venta son transportistas (1=Sí)
	 */
	@Basic
	@Column(name = "EMPTRANSPORTE")
	private Long empTransporte;
	
	/**
	 * Sin límite consumidor final
	 */
	@Basic
	@Column(name = "SINLIMITECONSFINAL")
	private Long sinLimiteConsFinal;
	
	/**
	 * Estado del facturador
	 */
	@Basic
	@Column(name = "ESTADO")
	private Long estado;

	// Getters y Setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumDoc() {
		return numDoc;
	}

	public void setNumDoc(String numDoc) {
		this.numDoc = numDoc;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getRazonSocial() {
		return razonSocial;
	}

	public void setRazonSocial(String razonSocial) {
		this.razonSocial = razonSocial;
	}

	public String getNombreComercial() {
		return nombreComercial;
	}

	public void setNombreComercial(String nombreComercial) {
		this.nombreComercial = nombreComercial;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public LocalDateTime getCreado() {
		return creado;
	}

	public void setCreado(LocalDateTime creado) {
		this.creado = creado;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public String getFirma() {
		return firma;
	}

	public void setFirma(String firma) {
		this.firma = firma;
	}

	public String getClaveFirma() {
		return claveFirma;
	}

	public void setClaveFirma(String claveFirma) {
		this.claveFirma = claveFirma;
	}

	public Long getEmpresaFirma() {
		return empresaFirma;
	}

	public void setEmpresaFirma(Long empresaFirma) {
		this.empresaFirma = empresaFirma;
	}

	public String getCodClave() {
		return codClave;
	}

	public void setCodClave(String codClave) {
		this.codClave = codClave;
	}

	public Long getContabilidad() {
		return contabilidad;
	}

	public void setContabilidad(Long contabilidad) {
		this.contabilidad = contabilidad;
	}

	public String getAgenteRetencion() {
		return agenteRetencion;
	}

	public void setAgenteRetencion(String agenteRetencion) {
		this.agenteRetencion = agenteRetencion;
	}

	public String getContribuyenteEspecial() {
		return contribuyenteEspecial;
	}

	public void setContribuyenteEspecial(String contribuyenteEspecial) {
		this.contribuyenteEspecial = contribuyenteEspecial;
	}

	public String getArtesano() {
		return artesano;
	}

	public void setArtesano(String artesano) {
		this.artesano = artesano;
	}

	public Long getMicroEmpresa() {
		return microEmpresa;
	}

	public void setMicroEmpresa(Long microEmpresa) {
		this.microEmpresa = microEmpresa;
	}

	public Long getRimpe() {
		return rimpe;
	}

	public void setRimpe(Long rimpe) {
		this.rimpe = rimpe;
	}

	public Long getPopularRimpe() {
		return popularRimpe;
	}

	public void setPopularRimpe(Long popularRimpe) {
		this.popularRimpe = popularRimpe;
	}

	public String getTuristico() {
		return turistico;
	}

	public void setTuristico(String turistico) {
		this.turistico = turistico;
	}

	public LocalDateTime getInicia() {
		return inicia;
	}

	public void setInicia(LocalDateTime inicia) {
		this.inicia = inicia;
	}

	public LocalDateTime getVence() {
		return vence;
	}

	public void setVence(LocalDateTime vence) {
		this.vence = vence;
	}

	public Long getDocEmitidos() {
		return docEmitidos;
	}

	public void setDocEmitidos(Long docEmitidos) {
		this.docEmitidos = docEmitidos;
	}

	public Long getDocPermitidos() {
		return docPermitidos;
	}

	public void setDocPermitidos(Long docPermitidos) {
		this.docPermitidos = docPermitidos;
	}

	public Long getImpCodProd() {
		return impCodProd;
	}

	public void setImpCodProd(Long impCodProd) {
		this.impCodProd = impCodProd;
	}

	public Long getInventario() {
		return inventario;
	}

	public void setInventario(Long inventario) {
		this.inventario = inventario;
	}

	public Long getEmpTransporte() {
		return empTransporte;
	}

	public void setEmpTransporte(Long empTransporte) {
		this.empTransporte = empTransporte;
	}

	public Long getSinLimiteConsFinal() {
		return sinLimiteConsFinal;
	}

	public void setSinLimiteConsFinal(Long sinLimiteConsFinal) {
		this.sinLimiteConsFinal = sinLimiteConsFinal;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}
}
