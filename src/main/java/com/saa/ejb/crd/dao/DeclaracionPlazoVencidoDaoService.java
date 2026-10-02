package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DeclaracionPlazoVencido;

import jakarta.ejb.Local;

@Local
public interface DeclaracionPlazoVencidoDaoService extends EntityDao<DeclaracionPlazoVencido> {

    /**
     * Declaraciones filtradas por estado y por rango de fecha del memorando (PLVNFCCR), las tres
     * opcionales. Es la consulta de {@code GET /rest/plvn/listar} (contrato §9): estado=1 es la
     * bandeja de Contabilidad, sin filtro es el historial completo de Crédito.
     *
     * @param estado 1 DECLARADA, 2 LIQUIDADA o 3 REVERTIDA; null = todas
     * @param desde  fecha de memorando desde (inclusive); null = sin límite inferior
     * @param hasta  fecha de memorando hasta (inclusive); null = sin límite superior
     * @return declaraciones ordenadas por fecha de corte descendente, luego por código descendente
     * @throws Throwable Si ocurre un error
     */
    List<DeclaracionPlazoVencido> selectByFiltros(Long estado, LocalDate desde, LocalDate hasta) throws Throwable;

    /**
     * La declaración VIVA (estado DECLARADA o LIQUIDADA) de un préstamo, si existe. Un préstamo
     * tiene a lo sumo una — las REVERTIDAS no cuentan (índice {@code UX_PLVN_PRESTAMO_VIVO}): un
     * préstamo revertido puede volver a declararse.
     *
     * @param idPrestamo código del préstamo (CRD.PRST)
     * @return la declaración viva, o null si no tiene ninguna
     * @throws Throwable Si ocurre un error
     */
    DeclaracionPlazoVencido selectVivaByPrestamo(Long idPrestamo) throws Throwable;

    /**
     * Declaración con ese número de memorando, normalizado ({@code UPPER(TRIM(...))}), en TODA la
     * tabla — incluidas las REVERTIDAS: un documento ya emitido no se renumera (D16).
     *
     * @param numeroMemorando número tal como lo escribió el usuario (se normaliza acá)
     * @return la declaración con ese memorando, o null si no existe
     * @throws Throwable Si ocurre un error
     */
    DeclaracionPlazoVencido selectByNumeroMemorandoNormalizado(String numeroMemorando) throws Throwable;

    /**
     * La última declaración grabada (por {@code PLVNFCDC} descendente, luego código descendente),
     * de cualquier estado. Alimenta {@code GET /rest/plvn/ultimoEncabezado} (§4 del contrato): la
     * pantalla precarga PARA/CC con lo de la última vez.
     *
     * @return la última declaración, o null si nunca se declaró ninguna
     * @throws Throwable Si ocurre un error
     */
    DeclaracionPlazoVencido selectUltima() throws Throwable;

    /**
     * Declaraciones VIVAS (DECLARADA o LIQUIDADA — nunca REVERTIDA) de alguno de
     * {@code idsPrestamo}, con {@code fechaDeclaracion} dentro de {@code [desde, hasta]}, en
     * lote. Pólizas de seguro, exclusión por plazo vencido
     * ({@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md} §7): una declaración REVERTIDA no
     * cuenta — el préstamo volvió a su estado normal.
     *
     * @param idsPrestamo préstamos a revisar
     * @param desde       fecha de declaración desde (inclusive)
     * @param hasta       fecha de declaración hasta (inclusive)
     * @return declaraciones vivas en ese rango, con el préstamo ya cargado
     * @throws Throwable Si ocurre un error
     */
    List<DeclaracionPlazoVencido> selectVivasByPrestamosYRango(List<Long> idsPrestamo,
            java.time.LocalDateTime desde, java.time.LocalDateTime hasta) throws Throwable;
}
