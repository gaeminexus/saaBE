package com.saa.ejb.asoprep.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetalleCargaArchivo;
import com.saa.model.crd.ParticipeXCargaArchivo;

import jakarta.ejb.Local;

/**
 * Servicio Stateful para procesar archivos Petro con manejo de transacciones
 */
@Local
public interface CargaArchivoPetroService {

    /**
     * Método principal que procesa el archivo Petro y los datos relacionados
     * 
     * @param archivoInputStream : Stream del archivo
     * @param fileName : Nombre del archivo
     * @param cargaArchivo : Registro de CargaArchivo
     * @param detallesCargaArchivos : Lista de detalles de carga
     * @param participesXCargaArchivo : Lista de partícipes por carga
     * @return Ruta donde se almacenó el archivo
     * @throws Throwable : Excepción
     */
    String procesarArchivoPetro(InputStream archivoInputStream, String fileName, 
                               CargaArchivo cargaArchivo, 
                               List<DetalleCargaArchivo> detallesCargaArchivos,
                               List<ParticipeXCargaArchivo> participesXCargaArchivo) throws Throwable;
    
    /**
     * Metodo para validar el archivo de Petro Comercial
     * 
     * @param archivoInputStream : Stream del archivo
     * @param fileName : Nombre del archivo
     * @param cargaArchivo : Registro de CargaArchivo
     * @return Ruta donde se almacenó el archivo
     * @throws Throwable : Excepción
     */
    CargaArchivo validarArchivoPetro(InputStream archivoInputStream, String fileName, CargaArchivo cargaArchivo) throws Throwable;
    
    /**
	 * Actualiza el código Petro de una Entidad dada su ID. Para correccion de errores en la carga de archivos de petro
	 * @param idEntidad: ID de la Entidad a actualizar.
	 * @param codigoPetro: Nuevo código Petro a asignar.
	 * @return: Participe actualizado.
	 * @throws Throwable: Excepción en caso de error.
	 */
    ParticipeXCargaArchivo actualizaCodigoPetroEntidad(Long codigoPetro, Long idParticipeXCarga, Long idEntidad) throws Throwable;
    
    /**
     * Aplica los pagos de un archivo Petro que ya fue validado.
     * Este método se ejecuta DESPUÉS de que el usuario revisa las novedades.
     * Solo procesa los registros que están OK (sin novedades bloqueantes).
     * 
     * @param codigoCargaArchivo : ID del CargaArchivo a procesar
     * @return : Resumen del procesamiento (cantidad de pagos aplicados, errores, etc.)
     * @throws Throwable : Excepción en caso de error
     */
    String aplicarPagosArchivoPetro(Long codigoCargaArchivo) throws Throwable;

    /**
     * Devuelve los registros de la carga cuyo valor descontado NO tiene definido
     * a qué préstamo, cuota o aporte aplicarse.
     *
     * Son los registros con una novedad que impide determinar el destino
     * automáticamente y para los que el usuario todavía no registró la
     * afectación manual (AVPC), o la registró por menos de lo descontado.
     *
     * Mientras esta lista no esté vacía, aplicarPagosArchivoPetro se niega a
     * procesar la carga. El frontend puede consultarla antes de procesar para
     * mostrarle al usuario qué le falta resolver.
     *
     * @param codigoCargaArchivo : ID del CargaArchivo a revisar
     * @return : Lista de registros con valores sin destino (vacía si todo está resuelto)
     * @throws Throwable : Excepción en caso de error
     */
    List<Map<String, Object>> obtenerValoresSinDestino(Long codigoCargaArchivo) throws Throwable;

    /**
     * Tope de afectación manual de UN partícipe en una carga — VALIDACION-TOPE-AFECTACION-
     * MANUAL.md §8. Única fuente de verdad: reutiliza la misma regla que
     * {@code validarTopeAfectacionManualPorParticipe} (la validación que efectivamente bloquea
     * al procesar), para que la pantalla de afectación pueda mostrar el tope mientras el
     * operador trabaja SIN reimplementarla en el frontend.
     *
     * <b>Este método informa, no valida ni bloquea.</b> Que no haya excedente acá no garantiza
     * que la carga procese: el tope se arma entre varias pantallas y sesiones, así que la
     * validación real sigue siendo la de {@code aplicarPagosArchivoPetro}.
     *
     * @param codigoCargaArchivo : ID de la carga (CRD.CRAR)
     * @param codigoPetro        : Rol Petro del partícipe
     * @return : Map con {@code codigoPetro}, {@code disponible} (SUM PXCADSDO, todos los
     *           productos), {@code afectado} (SUM AVPCVAFA, todas sus novedades),
     *           {@code exceso} ({@code max(0, afectado - disponible)}) y {@code restante}
     *           ({@code max(0, disponible - afectado)})
     * @throws Throwable : Excepción en caso de error
     */
    Map<String, Object> obtenerTopeAfectacionManual(Long codigoCargaArchivo, Long codigoPetro) throws Throwable;

    /**
     * Prevuelo del tope de afectación manual de TODA la carga, de solo lectura —
     * VALIDACION-TOPE-AFECTACION-MANUAL.md §9. Corre la misma pasada que la validación que
     * bloquea al procesar, pero sin bloquear: el operador la consulta mientras reparte, para
     * corregir antes de intentar procesar.
     *
     * <b>Alcance:</b> solo ve el exceso de afectaciones MANUALES contra lo descontado; no
     * detecta lo que el flujo automático vaya a aplicar encima (todavía no ocurrió en este
     * punto). No reemplaza a {@code DistribucionBandaService#obtenerDiferencia} — ver el
     * javadoc de la implementación para el contraste completo.
     *
     * @param codigoCargaArchivo : ID de la carga (CRD.CRAR)
     * @return : Map con {@code idCarga}, {@code participesConExceso}, {@code excesoTotal} y
     *           {@code detalle} (rolPetro, cédula, nombre, disponible, afectado, exceso, AVPC
     *           por cada partícipe con exceso)
     * @throws Throwable : Excepción en caso de error
     */
    Map<String, Object> obtenerPrevueloAfectacionManual(Long codigoCargaArchivo) throws Throwable;

}
