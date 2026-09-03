package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.VigenciaContrato;

import jakarta.ejb.Local;

@Local
public interface VigenciaContratoDaoService extends EntityDao<VigenciaContrato> {

    /**
     * Historial completo de vigencias de un contrato, más reciente primero (por fecha de
     * inicio; a igual fecha, por código descendente).
     *
     * @param idContrato : Código del contrato
     * @return           : Vigencias del contrato, activas y anuladas
     * @throws Throwable : Excepcion
     */
    List<VigenciaContrato> selectByContrato(Long idContrato) throws Throwable;

    /**
     * La vigencia ABIERTA (VGCNFCFN IS NULL) y activa (VGCNIDST = 1) de un contrato y tipo
     * de aporte. Como máximo existe una por contrato+tipo (UK_VGCN_ABIERTA). Null si no hay
     * ninguna abierta.
     *
     * @param idContrato    : Código del contrato
     * @param idTipoAporte  : Código del tipo de aporte (9 jubilación, 11 cesantía)
     * @return               : La vigencia abierta, o null
     * @throws Throwable    : Excepcion
     */
    VigenciaContrato selectAbierta(Long idContrato, Long idTipoAporte) throws Throwable;

    /**
     * La vigencia activa (VGCNIDST = 1) de un contrato y tipo de aporte cuyo rango cubre la
     * fecha dada (VGCNFCIN &lt;= fecha AND (VGCNFCFN IS NULL OR VGCNFCFN &gt;= fecha)). Null si
     * ninguna vigencia cubre esa fecha.
     *
     * @param idContrato    : Código del contrato
     * @param idTipoAporte  : Código del tipo de aporte (9 jubilación, 11 cesantía)
     * @param fecha         : Fecha a cubrir (se usa el último día del mes para "esperado")
     * @return               : La vigencia vigente en esa fecha, o null
     * @throws Throwable    : Excepcion
     */
    VigenciaContrato selectVigenteEnFecha(Long idContrato, Long idTipoAporte, LocalDate fecha) throws Throwable;

    /**
     * Vigencias ACTIVAS (VGCNIDST = 1) del contrato ACTIVO de cada entidad ACTIVO/
     * ACTIVO_EN_MORA de una filial, en UNA sola consulta — pensado para procesos batch
     * (generación de archivos) que necesitan resolver "esperado" para toda una filial sin
     * una consulta por partícipe. Mismo desempate que {@code ContratoDaoServiceImpl.selectActivoPorEntidad}
     * si una entidad tuviera más de un contrato activo (anomalía de datos): el de mayor código.
     *
     * @param codigoFilial : Código de la filial (CRD.FLLL)
     * @return             : Filas {@code Object[]{Long idEntidad, Long idTipoAporte, LocalDate fechaInicio, LocalDate fechaFin, Double monto, Long idContrato, Long idVigencia}}
     *                       — los dos últimos se agregaron el 2026-09-02 para poder identificar
     *                       vigencias ACTIVAS superpuestas (mismo tipo, mismo contrato,
     *                       mismo mes) sin tener que volver a la base.
     * @throws Throwable   : Excepcion
     */
    List<Object[]> selectVigentesPorFilial(Long codigoFilial) throws Throwable;

    /**
     * Códigos de entidad de una filial que pasan el MISMO filtro que {@link #selectVigentesPorFilial}
     * (contrato ACTIVO, entidad ACTIVO/ACTIVO_EN_MORA) — pero INDEPENDIENTE de que tengan
     * alguna vigencia. Es la mitad que faltaba para desambiguar el {@code null} del lote de
     * {@code VigenciaContratoService#esperadoEnLotePorFilial}: una entidad puede pasar este
     * filtro y no aparecer en {@code selectVigentesPorFilial} porque no tiene NINGUNA vigencia
     * ACTIVA para ningún tipo de aporte — ausencia de dato legítima (0.0 esperado siempre),
     * no una entidad fuera del lote (CORRECCION-TORMENTA-CONSULTAS-VIGENCIA.md §3.1).
     *
     * @param codigoFilial : Código de la filial (CRD.FLLL)
     * @return             : Códigos de entidad (CRD.ENTD.ENTDCDGO) cubiertos por el lote
     * @throws Throwable   : Excepcion
     */
    Set<Long> selectEntidadesConContratoActivoPorFilial(Long codigoFilial) throws Throwable;

}
