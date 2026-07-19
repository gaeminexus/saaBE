package com.saa.ejb.cxp.service;

import java.util.List;
import java.util.Map;
import com.saa.model.cxp.DetalleCargaTxt;
import com.saa.model.cxp.DocumentoCxp;
import jakarta.ejb.Local;

/**
 * Servicio principal para el proceso de carga de documentos recibidos del SRI.
 *
 * FLUJO:
 * 1. cargarArchivoTxt()     → Lee el TXT, crea/actualiza DocumentoCxp (único por claveAcceso)
 *                             y registra una DetalleCargaTxt (línea) por cada aparición en el archivo.
 * 2. cargarXmlDocumento()   → Sube el XML del DocumentoCxp. estadoDocumento=2.
 * 3. registrarDocumentoBD() → Parsea el XML y crea registros en tablas destino. estadoDocumento=3.
 * 4. resolverNovedad()      → REEMPLAZAR o MANTENER el documento con novedad.
 * 5. revertirDocumento()    → Elimina registros de tablas destino. estadoDocumento=6.
 */
@Local
public interface ProcesoCargaDocumentosService {

    /**
     * FASE 1: Lee el archivo TXT del SRI.
     * Por cada línea:
     *   - Busca DocumentoCxp por claveAcceso.
     *   - Si NO existe: crea DocumentoCxp (estadoDocumento=1) y línea con resultado=NUEVO.
     *   - Si existe sin diferencias: crea línea con resultado=DUPLICADO (no toca el DocumentoCxp).
     *   - Si existe CON diferencias: actualiza DocumentoCxp (estadoDocumento=5, novedad) y línea resultado=NOVEDAD.
     *
     * @return Mapa con: idCargaTxt, totalRegistros, nuevos, duplicados, novedades, detalles
     */
    Map<String, Object> cargarArchivoTxt(String contenidoTxt, String nombreArchivo,
                                          Long idEmpresa, Long idUsuario) throws Throwable;

    /**
     * FASE 2: Registra el XML de un DocumentoCxp específico. estadoDocumento=2.
     *
     * @param idDocumentoCxp ID del DocumentoCxp
     * @return DocumentoCxp actualizado
     */
    DocumentoCxp cargarXmlDocumento(Long idDocumentoCxp, String contenidoXml,
                                     String pathDestino, Long idUsuario) throws Throwable;

    /**
     * FASE 3: Parsea el XML y registra en las tablas destino CXP. estadoDocumento=3.
     *
     * @param idDocumentoCxp ID del DocumentoCxp (debe tener estadoDocumento=2)
     * @return Mapa con: idDocumentoBD, tipoTablaDestino, mensaje
     */
    Map<String, Object> registrarDocumentoBD(Long idDocumentoCxp,
                                              Long idEmpresa, Long idUsuario) throws Throwable;

    /**
     * FASE 4: Resuelve una novedad en un DocumentoCxp.
     * accion=REEMPLAZAR → revierte registros previos, carga nuevo XML y re-registra.
     * accion=MANTENER   → marca estadoNovedad=3 sin cambios.
     *
     * @param idDocumentoCxp ID del DocumentoCxp (debe tener estadoDocumento=5)
     */
    Map<String, Object> resolverNovedad(Long idDocumentoCxp, String accion,
                                         String contenidoXml, String pathDestino,
                                         Long idUsuario) throws Throwable;

    /**
     * FASE 5: Revierte un DocumentoCxp, eliminando registros de tablas destino. estadoDocumento=6.
     *
     * @param idDocumentoCxp ID del DocumentoCxp (debe tener estadoDocumento=3)
     */
    Map<String, Object> revertirDocumento(Long idDocumentoCxp, Long idUsuario) throws Throwable;

    /**
     * Obtiene el resumen de una carga (CargaArchivoTxt) con sus líneas (DetalleCargaTxt).
     */
    Map<String, Object> obtenerResumenCarga(Long idCargaTxt) throws Throwable;

    /**
     * Obtiene un DocumentoCxp por su ID.
     */
    DocumentoCxp obtenerDocumentoPorId(Long id) throws Throwable;

    /**
     * Obtiene un DetalleCargaTxt (línea) por su ID.
     */
    DetalleCargaTxt obtenerDetallePorId(Long id) throws Throwable;

    /**
     * Obtiene todos los DocumentoCxp con novedad pendiente de una empresa.
     */
    List<DocumentoCxp> obtenerNovedadesPendientes(Long idEmpresa) throws Throwable;

    /**
     * FASE 3b: Crea productos faltantes y luego registra el documento en BD.
     * Se llama cuando registrarDocumentoBD devuelve {requiereProductos: true}.
     */
    Map<String, Object> crearProductosYRegistrar(Long idDocumentoCxp, Long idEmpresa, Long idUsuario,
            List<Map<String, Object>> productosConGrupo) throws Throwable;
}