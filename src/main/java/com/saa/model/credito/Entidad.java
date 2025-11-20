package com.saa.model.credito;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Representa la tabla ENTD (Entidad).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ENTD", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "EntidadAll", query = "select e from Entidad e"),
    @NamedQuery(name = "EntidadId", query = "select e from Entidad e where e.codigo = :id")
})
public class Entidad implements Serializable {

    // ============================================================
    // CAMPOS – MAPEO DIRECTO A COLUMNAS DE LA TABLA
    // ============================================================

    /** Código */
    @Id
    @Basic
    @Column(name = "ENTDCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Código Filial */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /** FK - Código Tipo Hidrocarburífica */
    @ManyToOne
    @JoinColumn(name = "TPHDCDGO", referencedColumnName = "TPHDCDGO")
    private TipoHidrocarburifica tipoHidrocarburifica;

    /** FK - Código Tipo Identificación */
    @ManyToOne
    @JoinColumn(name = "TPDNCDGO", referencedColumnName = "TPDNCDGO")
    private TipoIdentificacion tipoIdentificacion;

    /** Número de identificación */
    @Basic
    @Column(name = "ENTDNMID", length = 50)
    private String numeroIdentificacion;

    /** Razón social */
    @Basic
    @Column(name = "ENTDRZNS", length = 2000)
    private String razonSocial;

    /** Número de cargas familiares */
    @Basic
    @Column(name = "ENTDNMCF")
    private Long cargasFamiliares;

    /** Nombre comercial */
    @Basic
    @Column(name = "ENTDNMCM", length = 2000)
    private String nombreComercial;

    /** Fecha nacimiento */
    @Basic
    @Column(name = "ENTDFCNC")
    private String fechaNacimiento;

    
    /** FK - Código tipoVivienda */
    @ManyToOne
    @JoinColumn(name = "TPVVCDGO", referencedColumnName = "TPVVCDGO")
    private TipoVivienda tipoVivienda;

    /** Sector público */
    @Basic
    @Column(name = "ENTDSCPB")
    private Long sectorPublico;

    /** Correo Personal */
    @Basic
    @Column(name = "ENTDCRPR", length = 2000)
    private String correoPersonal;

    /** Correo Institucional */
    @Basic
    @Column(name = "ENTDCRIN", length = 2000)
    private String correoInstitucional;

    /** Teléfono */
    @Basic
    @Column(name = "ENTDTLFN", length = 50)
    private String telefono;

    /** Tiene correo personal */
    @Basic
    @Column(name = "ENTDCPVR")
    private Long tieneCorreoPersonal;

    /** Tiene correo trabajo */
    @Basic
    @Column(name = "ENTDCIVR")
    private Long tieneCorreoTrabajo;

    /** Tiene teléfono */
    @Basic
    @Column(name = "ENTDTLVR")
    private Long tieneTelefono;

    /** Migrado */
    @Basic
    @Column(name = "ENTDMGRD")
    private Long migrado;

    /** Móvil */
    @Basic
    @Column(name = "ENTDMVLI", length = 50)
    private String movil;

    /** ID ciudad */
    @Basic
    @Column(name = "ENTDIDCD", length = 50)
    private String ciudadId;

    /** Porcentaje similitud */
    @Basic
    @Column(name = "ENTDPRSM")
    private Long porcentajeSimilitud;

    /** Búsqueda */
    @Basic
    @Column(name = "ENTDBSQD", length = 2000)
    private String busqueda;

    /** IP ingreso */
    @Basic
    @Column(name = "ENTDIPIN", length = 50)
    private String ipIngreso;

    /** Usuario ingreso */
    @Basic
    @Column(name = "ENTDUSIN", length = 50)
    private String usuarioIngreso;

    /** Fecha ingreso */
    @Basic
    @Column(name = "ENTDFCIN")
    private LocalDateTime fechaIngreso;

    /** IP modificación */
    @Basic
    @Column(name = "ENTDIPMD", length = 50)
    private String ipModificacion;

    /** Usuario modificación */
    @Basic
    @Column(name = "ENTDUSMD", length = 50)
    private String usuarioModificacion;

    /** Fecha modificación */
//    @Basic
//    @Column(name = "ENTDFCMD")
//    private LocalDateTime fechaModificacion;

    /** ID Estado */
    @Basic
    @Column(name = "ENTDIDST")
    private Long idEstado;

    /** URL foto/logo */
    @Basic
    @Column(name = "ENTDURFL", length = 2000)
    private String urlFotoLogo;


    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Filial getFilial() { return filial; }
    public void setFilial(Filial filial) { this.filial = filial; }

    public TipoHidrocarburifica getTipoHidrocarburifica() { return tipoHidrocarburifica; }
    public void setTipoHidrocarburifica(TipoHidrocarburifica tipoHidrocarburifica) { this.tipoHidrocarburifica = tipoHidrocarburifica; }

    public TipoIdentificacion getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(TipoIdentificacion tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public Long getCargasFamiliares() { return cargasFamiliares; }
    public void setCargasFamiliares(Long cargasFamiliares) { this.cargasFamiliares = cargasFamiliares; }

    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public TipoVivienda getTipoVivienda() { return tipoVivienda; }
    public void setTipoVivienda(TipoVivienda tipoVivienda) { this.tipoVivienda = tipoVivienda; }

    public Long getSectorPublico() { return sectorPublico; }
    public void setSectorPublico(Long sectorPublico) { this.sectorPublico = sectorPublico; }

    public String getCorreoPersonal() { return correoPersonal; }
    public void setCorreoPersonal(String correoPersonal) { this.correoPersonal = correoPersonal; }

    public String getCorreoInstitucional() { return correoInstitucional; }
    public void setCorreoInstitucional(String correoInstitucional) { this.correoInstitucional = correoInstitucional; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public Long getTieneCorreoPersonal() { return tieneCorreoPersonal; }
    public void setTieneCorreoPersonal(Long tieneCorreoPersonal) { this.tieneCorreoPersonal = tieneCorreoPersonal; }

    public Long getTieneCorreoTrabajo() { return tieneCorreoTrabajo; }
    public void setTieneCorreoTrabajo(Long tieneCorreoTrabajo) { this.tieneCorreoTrabajo = tieneCorreoTrabajo; }

    public Long getTieneTelefono() { return tieneTelefono; }
    public void setTieneTelefono(Long tieneTelefono) { this.tieneTelefono = tieneTelefono; }

    public Long getMigrado() { return migrado; }
    public void setMigrado(Long migrado) { this.migrado = migrado; }

    public String getMovil() { return movil; }
    public void setMovil(String movil) { this.movil = movil; }

    public String getCiudadId() { return ciudadId; }
    public void setCiudadId(String ciudadId) { this.ciudadId = ciudadId; }

    public Long getPorcentajeSimilitud() { return porcentajeSimilitud; }
    public void setPorcentajeSimilitud(Long porcentajeSimilitud) { this.porcentajeSimilitud = porcentajeSimilitud; }

    public String getBusqueda() { return busqueda; }
    public void setBusqueda(String busqueda) { this.busqueda = busqueda; }

    public String getIpIngreso() { return ipIngreso; }
    public void setIpIngreso(String ipIngreso) { this.ipIngreso = ipIngreso; }

    public String getUsuarioIngreso() { return usuarioIngreso; }
    public void setUsuarioIngreso(String usuarioIngreso) { this.usuarioIngreso = usuarioIngreso; }

    public LocalDateTime getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDateTime fechaIngreso) { this.fechaIngreso = fechaIngreso; }

    public String getIpModificacion() { return ipModificacion; }
    public void setIpModificacion(String ipModificacion) { this.ipModificacion = ipModificacion; }

    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }

//    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
//    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public Long getIdEstado() { return idEstado; }
    public void setIdEstado(Long idEstado) { this.idEstado = idEstado; }

    public String getUrlFotoLogo() { return urlFotoLogo; }
    public void setUrlFotoLogo(String urlFotoLogo) { this.urlFotoLogo = urlFotoLogo; }
}
