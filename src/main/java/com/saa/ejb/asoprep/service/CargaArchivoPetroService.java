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
     * <b>CORREGIDO 2026-09-03 (§10):</b> {@code disponible} NO es {@code SUM(PXCADSDO)} de
     * TODOS los productos del partícipe — es solo la de los productos con novedad BLOQUEANTE.
     * Medido en producción (SANCHEZ PRADO, rol 7508, carga 449): con la regla vieja el tope
     * daba 406,73 (todos los productos) cuando el operador solo tenía bloqueados 298,19
     * (PH+HS); PE y AH los iba a aplicar el flujo automático, y el tope viejo dejaba a la
     * afectación manual consumir esa plata igual. Ver el javadoc de
     * {@code CargaArchivoPetroServiceImpl#disponibleParaTope} (única definición).
     *
     * @param codigoCargaArchivo : ID de la carga (CRD.CRAR)
     * @param codigoPetro        : Rol Petro del partícipe
     * @return : Map con {@code codigoPetro}, {@code disponible} (SUM PXCADSDO SOLO de los
     *           productos con novedad bloqueante), {@code afectado} (SUM AVPCVAFA, todas sus
     *           novedades), {@code exceso} ({@code max(0, afectado - disponible)}) y
     *           {@code restante} ({@code max(0, disponible - afectado)})
     * @throws Throwable : Excepción en caso de error
     */
    Map<String, Object> obtenerTopeAfectacionManual(Long codigoCargaArchivo, Long codigoPetro) throws Throwable;

    /**
     * Prevuelo del tope de afectación manual de TODA la carga, de solo lectura —
     * VALIDACION-TOPE-AFECTACION-MANUAL.md §9/§14. Corre la misma pasada que la validación que
     * bloquea al procesar, pero sin bloquear: el operador la consulta mientras reparte, para
     * corregir antes de intentar procesar.
     *
     * <b>Alcance, actualizado 2026-09-03 (§10):</b> el hueco original de este prevuelo — no
     * detectar lo que el flujo automático aplica encima del tope manual — quedó CERRADO en la
     * fuente: {@code disponible} ahora excluye los productos sin novedad bloqueante (los que
     * resuelve el automático), así que ese exceso sí aparece acá. Lo que sigue sin cubrir es
     * cualquier otra interacción automático/manual todavía no identificada — no reemplaza a
     * {@code DistribucionBandaService#obtenerDiferencia} (la verificación posterior a aplicar).
     *
     * <b>Ampliado 2026-09-03 (§14):</b> además del exceso, ahora también informa el FALTANTE —
     * partícipes con afectación manual que repartieron MENOS que su pozo disponible. Es válido
     * por diseño (el flujo automático no corre para quien ya tiene afectación manual, así que lo
     * no repartido no lo reparte nadie), pero antes de esto era invisible hasta que la carga se
     * frenaba en la red del §11 — medido en producción: 4 partícipes, $35,64 sin repartir,
     * visibles desde antes de arrancar. Solo se informan partícipes que YA tienen alguna
     * afectación manual (si no tiene ninguna, el automático se encarga de todo su pozo y no es
     * un faltante real).
     *
     * @param codigoCargaArchivo : ID de la carga (CRD.CRAR)
     * @return : Map con {@code idCarga}, {@code participesConExceso}, {@code excesoTotal},
     *           {@code detalle} (rolPetro, cédula, nombre, disponible, afectado, exceso, AVPC y
     *           mensaje por cada partícipe con exceso — a estos hay que BAJARLES la afectación),
     *           y {@code participesConFaltante}, {@code faltanteTotal}, {@code detalleFaltante}
     *           (misma forma, {@code faltante} en vez de {@code exceso} — a estos hay que
     *           SUBIRLES/completarles la afectación). Las dos listas van separadas a propósito:
     *           son acciones opuestas para el operador.
     * @throws Throwable : Excepción en caso de error
     */
    Map<String, Object> obtenerPrevueloAfectacionManual(Long codigoCargaArchivo) throws Throwable;

    /**
     * Revalida las novedades de una carga YA CARGADA, con el código de validación ACTUAL —
     * VALIDACION-TOPE-AFECTACION-MANUAL.md §16, pedido del usuario 2026-09-03. NO relee el
     * archivo ni inserta nada: recorre los DTCA/PXCA que ya están en la base y recalcula sus
     * novedades. NO aplica pagos, aportes ni asientos (es validación pura, como el prevuelo).
     *
     * <b>Actualiza, nunca borra:</b> reusa el mismo {@code registrarNovedad} idempotente que ya
     * usan Fase 1/2 — si ya existe una novedad del mismo tipo para una fila, se actualiza (mismo
     * código, las afectaciones AVPC que cuelgan de ella no se pierden), nunca se duplica. Una
     * novedad que el código actual ya no reproduce se desactiva (si no tiene AVPC) o se conserva
     * intacta y se informa aparte (si tiene AVPC colgando — nunca se toca en ese caso).
     *
     * Ver el javadoc de la implementación para el detalle completo (qué SÍ recalcula, qué
     * deliberadamente NO, y el límite conocido de la desactivación: ningún otro endpoint de este
     * backend filtra por {@code estado} todavía).
     *
     * @param codigoCargaArchivo : ID de la carga (CRD.CRAR). La carga no puede estar en estado 3
     *                             (ya procesada) — revalidar novedades de una carga ya aplicada
     *                             no tiene efecto sobre lo que ya se pagó.
     * @return : Map con {@code idCarga}, {@code participesRevisados}, {@code novedadesCreadas},
     *           {@code novedadesActualizadas}, {@code novedadesDesactivadas},
     *           {@code novedadesConservadasPorAvpc} (+ su {@code detalleConservadasPorAvpc}) y
     *           {@code errores} (partícipes que fallaron individualmente, sin abortar el resto)
     * @throws Throwable : Excepción en caso de error
     */
    Map<String, Object> revalidarCarga(Long codigoCargaArchivo) throws Throwable;

}
