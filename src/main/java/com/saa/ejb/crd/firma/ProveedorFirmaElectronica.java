package com.saa.ejb.crd.firma;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pieza aislada que habla con el proveedor de firma electrónica (hoy Security Data / DatAgree). No es un EJB de negocio:
 * el servicio {@code FirmaContratoServiceImpl} la usa y ella no toca la base. Ninguna implementación escribe en el log la
 * clave, el token ni el contenido de los documentos.
 *
 * <p>Los estados que devuelve {@link EstadoTramite} usan los mismos códigos de {@link com.saa.rubros.EstadoFirmaContrato}
 * y {@link com.saa.rubros.EstadoFirmanteContrato}: el traductor del formato del proveedor vive en la implementación.</p>
 */
public interface ProveedorFirmaElectronica {

    /** Nombre del proveedor tal como se guarda en {@code CRD.FRCN.FRCNPRVD}. */
    String nombre();

    /** Sube el documento y los firmantes; el proveedor avisa a los firmantes. */
    ResultadoEnvio enviar(byte[] documentoPdf, String nombreDocumento, List<Firmante> firmantes) throws ProveedorFirmaException;

    /** Estado actual del trámite y de cada firmante. */
    EstadoTramite consultar(String idExterno) throws ProveedorFirmaException;

    /** PDF firmado de un trámite ya firmado por todos. */
    byte[] descargarFirmado(String idExterno) throws ProveedorFirmaException;

    /** Cancela el trámite en el proveedor. */
    void anular(String idExterno, String motivo) throws ProveedorFirmaException;

    /** Firmante que se manda al proveedor. */
    class Firmante {
        private int orden;
        private String rol;
        private String identificacion;
        private String nombre;
        private String correo;
        private String celular;

        public int getOrden() { return orden; }
        public void setOrden(int orden) { this.orden = orden; }
        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }
        public String getIdentificacion() { return identificacion; }
        public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
        public String getCelular() { return celular; }
        public void setCelular(String celular) { this.celular = celular; }
    }

    /** Respuesta del envío: id del trámite y, por orden de firma, el id de cada firmante (puede venir vacío). */
    class ResultadoEnvio {
        private String idExterno;
        private List<String> idsFirmantes = new ArrayList<>();

        public String getIdExterno() { return idExterno; }
        public void setIdExterno(String idExterno) { this.idExterno = idExterno; }
        public List<String> getIdsFirmantes() { return idsFirmantes; }
        public void setIdsFirmantes(List<String> idsFirmantes) { this.idsFirmantes = idsFirmantes; }
    }

    /** Estado del trámite en el proveedor. */
    class EstadoTramite {
        /** Código de {@link com.saa.rubros.EstadoFirmaContrato}. */
        private int estado;
        private List<EstadoFirmante> firmantes = new ArrayList<>();

        public int getEstado() { return estado; }
        public void setEstado(int estado) { this.estado = estado; }
        public List<EstadoFirmante> getFirmantes() { return firmantes; }
        public void setFirmantes(List<EstadoFirmante> firmantes) { this.firmantes = firmantes; }
    }

    /** Estado de un firmante en el proveedor. */
    class EstadoFirmante {
        private int orden;
        /** Código de {@link com.saa.rubros.EstadoFirmanteContrato}. */
        private int estado;
        private LocalDateTime fechaFirma;

        public int getOrden() { return orden; }
        public void setOrden(int orden) { this.orden = orden; }
        public int getEstado() { return estado; }
        public void setEstado(int estado) { this.estado = estado; }
        public LocalDateTime getFechaFirma() { return fechaFirma; }
        public void setFechaFirma(LocalDateTime fechaFirma) { this.fechaFirma = fechaFirma; }
    }
}
