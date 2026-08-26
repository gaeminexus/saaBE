package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del control "no se cierra un mes sin su archivo Petro" (decisión D13,
 * 2026-08-25).
 *
 * <h3>Por qué existe</h3>
 * <p>
 * El lado aportes del neteo (⑥) calcula <b>esperado − registrado</b>. Lo registrado son las
 * filas de {@code CRD.APRT} de tipos 9 y 11 del mes, y esas filas <b>las crea la fase 3 de
 * la carga del archivo Petro</b>. Si el archivo del mes no se cargó, no hay aportes
 * registrados, y el neteo reversa como "no cobrado" un dinero que sí se cobró.
 * </p>
 * <p>
 * No es hipotético: al 2026-08-25 el aporte esperado era 121.160,97 y agosto tenía 5.499,75
 * registrados porque el archivo del mes 8 todavía no estaba cargado. Cerrar en ese momento
 * habría reversado 115.661,22 de más.
 * </p>
 *
 * <h3>Cómo se comporta</h3>
 * <ul>
 * <li><b>Previsualizar:</b> nunca bloquea. Deja el aviso en {@code advertencias} con el
 * importe en juego, para que se pueda ver el estado del mes.</li>
 * <li><b>Ejecutar:</b> bloquea con {@code IncomeException}, salvo omisión explícita.</li>
 * </ul>
 *
 * <h3>La omisión</h3>
 * <p>
 * Un mes en que legítimamente no hubo archivo se cierra pidiendo
 * {@code omitirControlArchivoPetro} con un motivo. La omisión queda en {@code omitido} y
 * {@code motivoOmision}, se copia a la observación de la corrida y sale en las
 * advertencias. <b>Nunca es silenciosa.</b>
 * </p>
 */
public class ControlArchivoPetro {

    /** Año de afectación que se exigió. */
    private Long anio;

    /** Mes de afectación que se exigió, 1 a 12. */
    private Long mes;

    /** {@code true} si existe al menos una carga PROCESADA de ese mes de afectación. */
    private Boolean existe;

    /** Cuántas cargas procesadas se encontraron. Normalmente 1. */
    private Long cargasEncontradas;

    /** Código de la carga encontrada (CRD.CRAR). Nulo si no hay. */
    private Long idCarga;

    /** Nombre del archivo cargado. Nulo si no hay. */
    private String nombreArchivo;

    /** Fecha en que se subió el archivo — NO es el mes de afectación. Nula si no hay. */
    private LocalDate fechaCarga;

    /** Estado de la carga. 3 = PROCESADO; ver {@link com.saa.rubros.CrdEstadoCargaArchivo}. */
    private Long estadoCarga;

    /** Filiales cuyo archivo del mes está procesado. Vacía si no hay carga. */
    private List<Long> filiales = new ArrayList<Long>();

    /**
     * {@code true} si este control impide ejecutar el cierre: no hay archivo y no se pidió
     * omitir. En la previsualización nunca detiene nada, pero avisa igual.
     */
    private Boolean bloquea;

    /** {@code true} si el usuario pidió omitir el control explícitamente. */
    private Boolean omitido;

    /** Motivo de la omisión. Obligatorio cuando {@code omitido} es {@code true}. */
    private String motivoOmision;

    public ControlArchivoPetro() {
    }

    public Long getAnio() {
        return anio;
    }

    public void setAnio(Long anio) {
        this.anio = anio;
    }

    public Long getMes() {
        return mes;
    }

    public void setMes(Long mes) {
        this.mes = mes;
    }

    public Boolean getExiste() {
        return existe;
    }

    public void setExiste(Boolean existe) {
        this.existe = existe;
    }

    public Long getCargasEncontradas() {
        return cargasEncontradas;
    }

    public void setCargasEncontradas(Long cargasEncontradas) {
        this.cargasEncontradas = cargasEncontradas;
    }

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public LocalDate getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDate fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public Long getEstadoCarga() {
        return estadoCarga;
    }

    public void setEstadoCarga(Long estadoCarga) {
        this.estadoCarga = estadoCarga;
    }

    public List<Long> getFiliales() {
        return filiales;
    }

    public void setFiliales(List<Long> filiales) {
        this.filiales = filiales;
    }

    public Boolean getBloquea() {
        return bloquea;
    }

    public void setBloquea(Boolean bloquea) {
        this.bloquea = bloquea;
    }

    public Boolean getOmitido() {
        return omitido;
    }

    public void setOmitido(Boolean omitido) {
        this.omitido = omitido;
    }

    public String getMotivoOmision() {
        return motivoOmision;
    }

    public void setMotivoOmision(String motivoOmision) {
        this.motivoOmision = motivoOmision;
    }
}
