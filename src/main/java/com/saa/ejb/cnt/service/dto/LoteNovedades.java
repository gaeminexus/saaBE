package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * C25 — Lote de novedades de un mes, tal como lo produce el expediente
 * (04-entregables/novedades/AAAA-MM/novedades-AAAA-MM.json, formato C25-NOVEDADES-1; ver
 * cuadreSuper 03-trabajo/cambios-saa/c25-seguimiento-novedades/FORMATO-JSON-NOVEDADES.md).
 * Lo recibe {@code POST /nvdd/cargaLote}.
 */
@SuppressWarnings("serial")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoteNovedades implements Serializable {

	private String formato;
	/** AAAA-MM */
	private String mes;
	private Long version;
	private Long empresa;
	private String preparado;
	private String origen;
	private List<Novedad> novedades = new ArrayList<Novedad>();
	/** C25-NOVEDADES-2: cabecera del HTML de revisión del mes (CNT.RVNM). Ver FORMATO-JSON-NOVEDADES.md. */
	private java.util.Map<String, Object> revisionMes;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Novedad implements Serializable {
		private String modulo;
		private String codigo;
		private String archivo;
		private String titulo;
		private String tipoRespuesta;
		private String area;
		private Double valor;
		private String hallazgo;
		private String resolucion;
		private String criterios;
		private List<String> fuentes = new ArrayList<String>();
		private List<Asiento> asientos = new ArrayList<Asiento>();
		private List<String> decisiones = new ArrayList<String>();
		private List<Relacionada> relacionadas = new ArrayList<Relacionada>();
		private List<Requerimiento> requerimientos = new ArrayList<Requerimiento>();
		// C25-NOVEDADES-2: textos curados de la revisión con el cliente (03-trabajo/revision/datos-AAAA-MM.json)
		private String porQue;
		private String registro;
		private Double monto;
		private String montoNota;
		private String nota;
		private String estadoRevision;
		private String pedidoGeneral;
		private Object cifras;
		private Object regularizar;

		public String getModulo() { return modulo; }
		public void setModulo(String modulo) { this.modulo = modulo; }
		public String getCodigo() { return codigo; }
		public void setCodigo(String codigo) { this.codigo = codigo; }
		public String getArchivo() { return archivo; }
		public void setArchivo(String archivo) { this.archivo = archivo; }
		public String getTitulo() { return titulo; }
		public void setTitulo(String titulo) { this.titulo = titulo; }
		public String getTipoRespuesta() { return tipoRespuesta; }
		public void setTipoRespuesta(String tipoRespuesta) { this.tipoRespuesta = tipoRespuesta; }
		public String getArea() { return area; }
		public void setArea(String area) { this.area = area; }
		public Double getValor() { return valor; }
		public void setValor(Double valor) { this.valor = valor; }
		public String getHallazgo() { return hallazgo; }
		public void setHallazgo(String hallazgo) { this.hallazgo = hallazgo; }
		public String getResolucion() { return resolucion; }
		public void setResolucion(String resolucion) { this.resolucion = resolucion; }
		public String getCriterios() { return criterios; }
		public void setCriterios(String criterios) { this.criterios = criterios; }
		public List<String> getFuentes() { return fuentes; }
		public void setFuentes(List<String> fuentes) { this.fuentes = fuentes; }
		public List<Asiento> getAsientos() { return asientos; }
		public void setAsientos(List<Asiento> asientos) { this.asientos = asientos; }
		public List<String> getDecisiones() { return decisiones; }
		public void setDecisiones(List<String> decisiones) { this.decisiones = decisiones; }
		public List<Relacionada> getRelacionadas() { return relacionadas; }
		public void setRelacionadas(List<Relacionada> relacionadas) { this.relacionadas = relacionadas; }
		public List<Requerimiento> getRequerimientos() { return requerimientos; }
		public void setRequerimientos(List<Requerimiento> requerimientos) { this.requerimientos = requerimientos; }
		public String getPorQue() { return porQue; }
		public void setPorQue(String porQue) { this.porQue = porQue; }
		public String getRegistro() { return registro; }
		public void setRegistro(String registro) { this.registro = registro; }
		public Double getMonto() { return monto; }
		public void setMonto(Double monto) { this.monto = monto; }
		public String getMontoNota() { return montoNota; }
		public void setMontoNota(String montoNota) { this.montoNota = montoNota; }
		public String getNota() { return nota; }
		public void setNota(String nota) { this.nota = nota; }
		public String getEstadoRevision() { return estadoRevision; }
		public void setEstadoRevision(String estadoRevision) { this.estadoRevision = estadoRevision; }
		public String getPedidoGeneral() { return pedidoGeneral; }
		public void setPedidoGeneral(String pedidoGeneral) { this.pedidoGeneral = pedidoGeneral; }
		public Object getCifras() { return cifras; }
		public void setCifras(Object cifras) { this.cifras = cifras; }
		public Object getRegularizar() { return regularizar; }
		public void setRegularizar(Object regularizar) { this.regularizar = regularizar; }
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Asiento implements Serializable {
		/** ASNTCDGO de la base en que se generó el mes. */
		private Long asnt;
		/** ASNTNMAL; si falta {@code asnt} se resuelve por él dentro de la empresa. */
		private String numeroAlterno;
		private String rol;

		public Long getAsnt() { return asnt; }
		public void setAsnt(Long asnt) { this.asnt = asnt; }
		public String getNumeroAlterno() { return numeroAlterno; }
		public void setNumeroAlterno(String numeroAlterno) { this.numeroAlterno = numeroAlterno; }
		public String getRol() { return rol; }
		public void setRol(String rol) { this.rol = rol; }
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Relacionada implements Serializable {
		private String mes;
		private String modulo;
		private String codigo;

		public String getMes() { return mes; }
		public void setMes(String mes) { this.mes = mes; }
		public String getModulo() { return modulo; }
		public void setModulo(String modulo) { this.modulo = modulo; }
		public String getCodigo() { return codigo; }
		public void setCodigo(String codigo) { this.codigo = codigo; }
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Requerimiento implements Serializable {
		private Long secuencia;
		private String tipo;
		private String area;
		private String descripcion;
		private String tipoReferencia;
		private String referencia;
		private Double valor;
		/** AAAA-MM-DD */
		private String fecha;
		private String opciones;
		private String supuesto;
		/** TXT, REVISION, AJUSTE (C25-NOVEDADES-2); por defecto TXT. */
		private String origen;

		public Long getSecuencia() { return secuencia; }
		public void setSecuencia(Long secuencia) { this.secuencia = secuencia; }
		public String getTipo() { return tipo; }
		public void setTipo(String tipo) { this.tipo = tipo; }
		public String getArea() { return area; }
		public void setArea(String area) { this.area = area; }
		public String getDescripcion() { return descripcion; }
		public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
		public String getTipoReferencia() { return tipoReferencia; }
		public void setTipoReferencia(String tipoReferencia) { this.tipoReferencia = tipoReferencia; }
		public String getReferencia() { return referencia; }
		public void setReferencia(String referencia) { this.referencia = referencia; }
		public Double getValor() { return valor; }
		public void setValor(Double valor) { this.valor = valor; }
		public String getFecha() { return fecha; }
		public void setFecha(String fecha) { this.fecha = fecha; }
		public String getOpciones() { return opciones; }
		public void setOpciones(String opciones) { this.opciones = opciones; }
		public String getSupuesto() { return supuesto; }
		public void setSupuesto(String supuesto) { this.supuesto = supuesto; }
		public String getOrigen() { return origen; }
		public void setOrigen(String origen) { this.origen = origen; }
	}

	/** Decisión del expediente para {@code POST /nvdd/cargaDecisiones} (resultados/decisiones.json). */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class DecisionCarga implements Serializable {
		private String numero;
		private String fecha;
		private String tema;
		private String texto;
		private String tipoDecide;
		private String cargo;
		private String evidencia;
		private String titulo;
		private String resumen;
		private String razones;

		public String getNumero() { return numero; }
		public void setNumero(String numero) { this.numero = numero; }
		public String getFecha() { return fecha; }
		public void setFecha(String fecha) { this.fecha = fecha; }
		public String getTema() { return tema; }
		public void setTema(String tema) { this.tema = tema; }
		public String getTexto() { return texto; }
		public void setTexto(String texto) { this.texto = texto; }
		public String getTipoDecide() { return tipoDecide; }
		public void setTipoDecide(String tipoDecide) { this.tipoDecide = tipoDecide; }
		public String getCargo() { return cargo; }
		public void setCargo(String cargo) { this.cargo = cargo; }
		public String getEvidencia() { return evidencia; }
		public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
		public String getTitulo() { return titulo; }
		public void setTitulo(String titulo) { this.titulo = titulo; }
		public String getResumen() { return resumen; }
		public void setResumen(String resumen) { this.resumen = resumen; }
		public String getRazones() { return razones; }
		public void setRazones(String razones) { this.razones = razones; }
	}

	/** Archivo de decisiones del expediente (formato C25-DECISIONES-1). */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class LoteDecisiones implements Serializable {
		private String formato;
		private List<DecisionCarga> decisiones = new ArrayList<DecisionCarga>();

		public String getFormato() { return formato; }
		public void setFormato(String formato) { this.formato = formato; }
		public List<DecisionCarga> getDecisiones() { return decisiones; }
		public void setDecisiones(List<DecisionCarga> decisiones) { this.decisiones = decisiones; }
	}

	public String getFormato() { return formato; }
	public void setFormato(String formato) { this.formato = formato; }
	public String getMes() { return mes; }
	public void setMes(String mes) { this.mes = mes; }
	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }
	public Long getEmpresa() { return empresa; }
	public void setEmpresa(Long empresa) { this.empresa = empresa; }
	public String getPreparado() { return preparado; }
	public void setPreparado(String preparado) { this.preparado = preparado; }
	public String getOrigen() { return origen; }
	public void setOrigen(String origen) { this.origen = origen; }
	public List<Novedad> getNovedades() { return novedades; }
	public void setNovedades(List<Novedad> novedades) { this.novedades = novedades; }
	public java.util.Map<String, Object> getRevisionMes() { return revisionMes; }
	public void setRevisionMes(java.util.Map<String, Object> revisionMes) { this.revisionMes = revisionMes; }
}
