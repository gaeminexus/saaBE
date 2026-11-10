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

    /**
     * Manda el documento y los datos del partícipe. El partícipe firma desde el enlace que le llega del proveedor y el
     * representante legal de ASOPREP firma solo al final (firma embebida). El PDF firmado se obtiene después con
     * {@link #consultar} y {@link #descargarFirmado}; un proveedor que firme de inmediato lo puede devolver en
     * {@link ResultadoEnvio#getPdfFirmado}.
     */
    ResultadoEnvio enviar(byte[] documentoPdf, String nombreDocumento, DatosParticipe participe, PosicionesFirma posiciones)
            throws ProveedorFirmaException;

    /** Estado actual del trámite y de cada firmante. */
    EstadoTramite consultar(String idExterno) throws ProveedorFirmaException;

    /** PDF firmado de un trámite ya firmado por todos. */
    byte[] descargarFirmado(String idExterno) throws ProveedorFirmaException;

    /** Cancela el trámite en el proveedor. */
    void anular(String idExterno, String motivo) throws ProveedorFirmaException;

    /**
     * Datos del partícipe que se envían al proveedor, reunidos en UN solo lugar: si el proveedor pide más campos, la fase 2
     * los agrega aquí.
     */
    class DatosParticipe {
        private String identificacion;
        private String nombre;
        private String correo;
        private String celular;
        /** Código dactilar (6 caracteres): solo viaja al proveedor, NUNCA se guarda ni se escribe en un log. */
        private String codigoDactilar;

        public String getCodigoDactilar() { return codigoDactilar; }
        public void setCodigoDactilar(String codigoDactilar) { this.codigoDactilar = codigoDactilar; }
        public String getIdentificacion() { return identificacion; }
        public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
        public String getCelular() { return celular; }
        public void setCelular(String celular) { this.celular = celular; }
    }

    /** Posición de una firma en el PDF: puntos PDF (A4 595 x 842), origen abajo a la izquierda, página desde 1. */
    class PosicionFirma {
        private final int numPage;
        private final double x;
        private final double y;
        private final double width;
        private final double height;

        public PosicionFirma(int numPage, double x, double y, double width, double height) {
            this.numPage = numPage;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getNumPage() { return numPage; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
    }

    /** Dónde firma cada uno: el partícipe y el representante legal de ASOPREP (firma embebida). */
    class PosicionesFirma {
        private final List<PosicionFirma> participe = new ArrayList<>();
        private final List<PosicionFirma> representante = new ArrayList<>();

        public List<PosicionFirma> getParticipe() { return participe; }
        public List<PosicionFirma> getRepresentante() { return representante; }
    }

    /**
     * Respuesta del envío: id del trámite, el id del partícipe en el proveedor (puede venir vacío) y, OPCIONALMENTE, el PDF
     * ya firmado (respuesta inmediata). Sin PDF firmado el trámite queda ENVIADO a la espera de «Actualizar estado».
     */
    class ResultadoEnvio {
        private String idExterno;
        private List<String> idsFirmantes = new ArrayList<>();
        private byte[] pdfFirmado;

        public byte[] getPdfFirmado() { return pdfFirmado; }
        public void setPdfFirmado(byte[] pdfFirmado) { this.pdfFirmado = pdfFirmado; }

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
        /** Motivo del rechazo (solo si el trámite quedó rechazado). */
        private String motivoRechazo;

        public String getMotivoRechazo() { return motivoRechazo; }
        public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
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
