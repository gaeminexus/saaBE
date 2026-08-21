package com.saa.model.rhh;

import java.io.Serializable;

/**
 * Resultado de generar un archivo de carga batch del IESS.
 *
 * <p>POJO de transporte, sin @Entity. Lleva el contenido, el nombre sugerido y —lo que
 * justifica que exista en vez de devolver un simple <code>String</code>— <b>un aviso que
 * puede acompañar a un archivo perfectamente generado</b>.</p>
 *
 * <p>El caso que lo motiva: mientras el código de tipo de empleador sea el provisional del
 * ejemplo oficial y no el que el IESS asignó a esta empresa, el archivo se puede generar
 * —hace falta para probar— pero <b>no se puede subir</b>. Sin este aviso, la unica
 * proteccion contra subirlo seria que alguien se acuerde, y eso no es una proteccion.</p>
 */
@SuppressWarnings("serial")
public class ArchivoBatchIess implements Serializable {

    /** Contenido del archivo, listo para escribir. */
    private String contenido;

    /** Nombre sugerido, con extension. */
    private String nombre;

    /** Aviso que hay que enseñar junto al archivo, o null si no hay ninguno. */
    private String aviso;

    /** El archivo se genero pero NO debe subirse al portal. */
    private boolean noSubir;

    /** Numero de registros escritos. */
    private Integer registros;

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAviso() {
        return aviso;
    }

    public void setAviso(String aviso) {
        this.aviso = aviso;
    }

    public boolean isNoSubir() {
        return noSubir;
    }

    public void setNoSubir(boolean noSubir) {
        this.noSubir = noSubir;
    }

    public Integer getRegistros() {
        return registros;
    }

    public void setRegistros(Integer registros) {
        this.registros = registros;
    }

}
