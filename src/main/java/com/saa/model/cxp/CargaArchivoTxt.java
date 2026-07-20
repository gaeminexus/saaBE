package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;

import jakarta.persistence.*;

/**
 * Entity CargaArchivoTxt.
 * Cabecera de carga del archivo TXT recibidos del SRI (tabla pgs.crtx).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRTX", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "CargaArchivoTxtAll", query = "select e from CargaArchivoTxt e"),
    @NamedQuery(name = "CargaArchivoTxtId",  query = "select e from CargaArchivoTxt e where e.id = :id"),
    @NamedQuery(name = "CargaArchivoTxtByEmpresa",
                query = "select e from CargaArchivoTxt e where e.empresa.codigo = :idEmpresa order by e.id desc")
})
public class CargaArchivoTxt implements Serializable {

    @Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    @ManyToOne @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    @Basic @Column(name = "FECHACARGA")
    private LocalDateTime fechaCarga;

    @Basic @Column(name = "NOMBREARCHIVO", length = 500)
    private String nombreArchivo;

    @Basic @Column(name = "TOTALREGISTROS")
    private Long totalRegistros;

    @Basic @Column(name = "REGISTROSNUEVOS")
    private Long registrosNuevos;

    @Basic @Column(name = "REGISTROSDUPLICADOS")
    private Long registrosDuplicados;

    @Basic @Column(name = "REGISTROSNOVEDAD")
    private Long registrosNovedad;

    @Basic @Column(name = "ESTADO")
    private Long estado;

    @Basic @Column(name = "OBSERVACION", length = 2000)
    private String observacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public Long getTotalRegistros() { return totalRegistros; }
    public void setTotalRegistros(Long totalRegistros) { this.totalRegistros = totalRegistros; }
    public Long getRegistrosNuevos() { return registrosNuevos; }
    public void setRegistrosNuevos(Long registrosNuevos) { this.registrosNuevos = registrosNuevos; }
    public Long getRegistrosDuplicados() { return registrosDuplicados; }
    public void setRegistrosDuplicados(Long registrosDuplicados) { this.registrosDuplicados = registrosDuplicados; }
    public Long getRegistrosNovedad() { return registrosNovedad; }
    public void setRegistrosNovedad(Long registrosNovedad) { this.registrosNovedad = registrosNovedad; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
