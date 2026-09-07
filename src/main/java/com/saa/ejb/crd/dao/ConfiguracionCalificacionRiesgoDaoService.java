package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;
import com.saa.model.crd.Producto;

import jakarta.ejb.Local;

/** DAO de CRD.CFCR — PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md. */
@Local
public interface ConfiguracionCalificacionRiesgoDaoService extends EntityDao<ConfiguracionCalificacionRiesgo> {

    /**
     * La configuración vigente de un producto a una fecha. {@code idEmpresa} es tolerante a
     * {@code null} en la fila de CFCR (aplica a cualquier empresa) — la carga inicial
     * (sql/177) quedó así para todos los productos.
     *
     * @return la configuración vigente, o {@code null} si no hay ninguna
     */
    ConfiguracionCalificacionRiesgo selectVigentePorProducto(Long idProducto, Long idEmpresa, LocalDate fecha)
            throws Throwable;

    /**
     * Productos de {@code CRD.PRDC} que NO tienen ninguna {@code ConfiguracionCalificacionRiesgo}
     * vigente a la fecha dada — para que el G48 falle una sola vez con el listado completo
     * (PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md §5), no producto por producto.
     */
    List<Producto> selectProductosSinConfiguracionVigente(LocalDate fecha) throws Throwable;

    /**
     * Todas las configuraciones vigentes a una fecha, tolerantes al mismo {@code null} = "cualquier
     * empresa" que {@link #selectVigentePorProducto}. Alimenta {@code /cfcr/listado}: una sola
     * consulta para todos los productos, en vez de una por producto.
     *
     * Orden {@code fechaInicio desc, codigo desc}: si por un defecto de parametrización hubiera
     * dos vigentes del mismo producto (una universal y una de empresa específica — ver el
     * comentario de {@link #selectVigentePorProducto} sobre esa ambigüedad, que P22 no resuelve
     * para datos históricos, solo la bloquea hacia adelante vía {@code selectSolapadas}), la
     * primera en este orden es la que gana al agrupar en Java.
     *
     * @param idEmpresa : Código de la empresa (SCP.PJRQ); {@code null} trae solo las universales
     * @param fecha     : Fecha a la que se evalúa la vigencia
     * @return          : Configuraciones vigentes de todos los productos; VACÍA si no hay ninguna
     * @throws Throwable : Excepcion
     */
    List<ConfiguracionCalificacionRiesgo> selectVigentesPorEmpresa(Long idEmpresa, LocalDate fecha)
            throws Throwable;

    /**
     * Historial completo (vigentes y cerradas) de un producto, de la más reciente a la más
     * antigua. Para auditoría y reprocesos — mismo criterio que
     * {@code ConfiguracionBandaProductoDaoService#selectHistorial}.
     *
     * @param idProducto : Código del producto (CRD.PRDC)
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ); {@code null} trae solo las universales
     * @return           : Historial; VACÍO si nunca se parametrizó
     * @throws Throwable : Excepcion
     */
    List<ConfiguracionCalificacionRiesgo> selectHistorial(Long idProducto, Long idEmpresa)
            throws Throwable;

    /**
     * Configuraciones ACTIVAS de un producto cuya vigencia se solapa con [desde, hasta]. Lo usa
     * la validación de unicidad del guardado.
     *
     * ⚠️ Tratamiento de {@code idEmpresa}, DISTINTO del de bandas (que exige empresa real vía FK):
     * como {@code null} en una fila de {@code CRD.CFCR} significa "aplica a cualquier empresa",
     * dos configuraciones del mismo producto se consideran en conflicto si sus vigencias se
     * solapan Y (cualquiera de las dos es universal, O las dos son de la misma empresa
     * específica) — nunca alcanza con comparar el código de empresa por igualdad simple. Es la
     * misma semántica null-tolerante de {@link #selectVigentePorProducto}, aplicada a la
     * validación de unicidad.
     *
     * @param idProducto : Código del producto (CRD.PRDC)
     * @param idEmpresa  : Código de la empresa de la configuración que se pretende grabar;
     *                     {@code null} = universal
     * @param desde      : Inicio de la vigencia que se pretende grabar
     * @param hasta      : Fin de la vigencia que se pretende grabar; {@code null} = abierta
     * @return           : Configuraciones que se solapan; VACÍA si no hay conflicto
     * @throws Throwable : Excepcion
     */
    List<ConfiguracionCalificacionRiesgo> selectSolapadas(Long idProducto, Long idEmpresa,
            LocalDate desde, LocalDate hasta) throws Throwable;
}
