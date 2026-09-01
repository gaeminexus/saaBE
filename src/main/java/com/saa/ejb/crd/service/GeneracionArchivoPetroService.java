package com.saa.ejb.crd.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.GeneracionArchivoPetro;

import jakarta.ejb.Local;

/**
 * Interface Service para GeneracionArchivoPetro (GNAP).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface GeneracionArchivoPetroService extends EntityService<GeneracionArchivoPetro>  {

    /**
     * Crea una nueva generación de archivo.
     * 
     * @param generacion Objeto con los datos de la generación
     * @return Generación creada con ID asignado
     * @throws Throwable Si ocurre un error
     */
    GeneracionArchivoPetro crear(GeneracionArchivoPetro generacion) throws Exception;
    
    /**
     * Actualiza una generación existente.
     * 
     * @param generacion Objeto con los datos actualizados
     * @return Generación actualizada
     * @throws Exception Si ocurre un error
     */
    GeneracionArchivoPetro actualizar(GeneracionArchivoPetro generacion) throws Exception;
    
    /**
     * Busca una generación por ID.
     * 
     * @param codigo Código de la generación
     * @return Generación encontrada o null
     * @throws Exception Si ocurre un error
     */
    GeneracionArchivoPetro buscarPorId(Long codigo) throws Exception;
    
    /**
     * Busca una generación por periodo.
     * 
     * @param mes Mes del periodo (1-12)
     * @param anio Año del periodo
     * @param codigoFilial Código de la filial
     * @return Generación encontrada o null
     * @throws Exception Si ocurre un error
     */
    GeneracionArchivoPetro buscarPorPeriodo(Long mes, Long anio, Long codigoFilial) throws Exception;
    
    /**
     * Lista todas las generaciones de una filial.
     * 
     * @param codigoFilial Código de la filial
     * @return Lista de generaciones ordenadas por fecha desc
     * @throws Exception Si ocurre un error
     */
    List<GeneracionArchivoPetro> listarPorFilial(Long codigoFilial) throws Exception;
    
    /**
     * Lista generaciones por estado.
     * 
     * @param estado Estado (1=GENERADO, 2=ENVIADO, 3=PROCESADO)
     * @return Lista de generaciones
     * @throws Exception Si ocurre un error
     */
    List<GeneracionArchivoPetro> listarPorEstado(Long estado) throws Exception;
    
    /**
     * Crea la cabecera de generación validando que no exista duplicado.
     * 
     * @param mes Mes del periodo
     * @param anio Año del periodo
     * @param codigoFilial Código de la filial
     * @param usuario Usuario que crea
     * @return Cabecera creada
     * @throws Exception Si ya existe o hay error
     */
    GeneracionArchivoPetro crearCabeceraGeneracion(Long mes, Long anio, Long codigoFilial, String usuario) throws Exception;
    
    /**
     * Procesa la generación completa: recopila datos, crea detalles y genera archivo.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que procesa
     * @return Mapa con resultados
     * @throws Exception Si hay error
     */
    Map<String, Object> procesarGeneracion(Long codigoGeneracion, String usuario) throws Exception;
    
    /**
     * Anula una generación.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que anula
     * @param motivo Motivo de anulación
     * @throws Exception Si hay error
     */
    void anular(Long codigoGeneracion, String usuario, String motivo) throws Exception;
    
    /**
     * Marca una generación como enviada.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que marca
     * @return Generación actualizada
     * @throws Exception Si hay error
     */
    GeneracionArchivoPetro marcarEnviado(Long codigoGeneracion, String usuario) throws Exception;
    
    /**
     * Marca una generación como procesada.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que marca
     * @return Generación actualizada
     * @throws Exception Si hay error
     */
    GeneracionArchivoPetro marcarProcesado(Long codigoGeneracion, String usuario) throws Exception;
    
    /**
     * Obtiene el detalle completo de una generación.
     * 
     * @param codigoGeneracion ID de la generación
     * @return Mapa con generación y detalles
     * @throws Exception Si hay error
     */
    Map<String, Object> obtenerDetalle(Long codigoGeneracion) throws Exception;
    
    /**
     * Obtiene estadísticas de una generación.
     * 
     * @param codigoGeneracion ID de la generación
     * @return Mapa con estadísticas
     * @throws Exception Si hay error
     */
    Map<String, Object> obtenerEstadisticas(Long codigoGeneracion) throws Exception;
    
    /**
     * Regenera el archivo TXT de una generación.
     *
     * @param codigoGeneracion ID de la generación
     * @return Ruta del archivo regenerado
     * @throws Exception Si hay error
     */
    String regenerarArchivo(Long codigoGeneracion) throws Exception;

    /**
     * Marca la generación como descargada (fecha y usuario de descarga).
     *
     * A partir de este momento la generación ya no se puede eliminar: el
     * archivo salió del sistema y pudo haber sido entregado a Petrocomercial.
     * Si ya estaba marcada, se conserva la marca original.
     *
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que descarga
     * @return Generación actualizada
     * @throws Exception Si hay error
     */
    GeneracionArchivoPetro marcarDescargado(Long codigoGeneracion, String usuario) throws Exception;

    /**
     * Cuánto se espera cobrar de aportes personales (producto AH) de UNA filial en un
     * período, SIN generar ni persistir nada — solo el cálculo (2026-08-31, para el asiento
     * ③ de apertura del cierre de cartera).
     *
     * <p><b>Es la MISMA fuente y la MISMA rama que usa la generación real del archivo</b>:
     * llama al mismo método privado de cálculo que {@code procesarGeneracion} (a través del
     * mismo despachador detrás de {@code ConfiguracionGeneracionAportesService
     * #porFaltanteActiva()}, rubro 242) — nunca puede divergir del archivo que
     * efectivamente se le manda a Petro, porque es literalmente el mismo cálculo, no una
     * copia. Si el rubro 242 está apagado (el caso de hoy), usa {@code HistorialSueldo} ×
     * meses adeudados; si se enciende, usa {@code CRD.VGCN} con el faltante acumulado — este
     * método no necesita cambiar en ninguno de los dos casos.</p>
     *
     * <p>Segundo consumidor (2026-08-31): el asiento ⑥ (neteo) del cierre de cartera, para el
     * MES QUE SE CIERRA — mismo método, mes distinto al del ③. Que el ⑥ y el ③ usen la misma
     * fuente es lo que garantiza que el ⑥ "blanquea" exactamente lo que el ③ del mes anterior
     * abrió, sin residuo que nadie pueda explicar.</p>
     *
     * @param mes          Mes del período, 1 a 12 — el mes que se quiere cobrar
     * @param anio         Año del período
     * @param codigoFilial Filial a calcular
     * @return             {@code {"jubilacion": .., "cesantia": .., "total": ..,
     *                     "participantes": ..}}
     * @throws Exception   Si ocurre un error
     */
    Map<String, Double> calcularAportesEsperados(Long mes, Long anio, Long codigoFilial) throws Exception;

    /**
     * Elimina físicamente una generación y todo su detalle (CXPG, PDGA, DTGA),
     * incluido el archivo TXT del disco, para poder volver a generar el periodo.
     *
     * NO se puede eliminar si el archivo TXT ya fue descargado, ni si la
     * generación está en estado ENVIADO (2) o PROCESADO (3).
     *
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que elimina
     * @return Mapa con el resumen de lo eliminado
     * @throws Exception Si no existe o no cumple las condiciones para eliminarse
     */
    Map<String, Object> eliminarGeneracion(Long codigoGeneracion, String usuario) throws Exception;
}
