package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

@Local
public interface PrestamoDaoService extends EntityDao<Prestamo> {
    
    /**
     * Busca un préstamo por su idAsoprep.
     * @param idAsoprep ID del asociado préstamo
     * @return Préstamo encontrado o null si no existe
     * @throws Throwable Si ocurre algún error
     */
    Prestamo selectByIdAsoprep(Long idAsoprep) throws Throwable;
    
    /**
     * Busca préstamos activos de una entidad para un producto específico.
     * Usado en FASE 2 para encontrar el préstamo correspondiente a un registro del archivo.
     * @param rolPetroComercial Código Petro de la entidad (String)
     * @param codigoPetroProducto Código Petro del producto
     * @return Lista de préstamos activos que coinciden
     * @throws Throwable Si ocurre algún error
     */
    List<Prestamo> selectByEntidadYProductoActivos(String rolPetroComercial, String codigoPetroProducto) throws Throwable;

    /**
     * Busca préstamos activos por IDs numéricos de entidad y producto.
     * Método más eficiente y confiable que evita problemas con espacios en códigos String.
     * @param codigoEntidad ID numérico de la entidad
     * @param codigoProducto ID numérico del producto
     * @return Lista de préstamos activos que coinciden
     * @throws Throwable Si ocurre algún error
     */
    List<Prestamo> selectByEntidadYProductoActivosById(Long codigoEntidad, Long codigoProducto) throws Throwable;

    /**
     * Busca préstamos cuya fecha esté entre el primer y último día del mes indicado.
     */
    List<Prestamo> selectByRangoFechas(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable;

    /** Retorna todos los préstamos con un estadoPrestamo específico */
    List<Prestamo> selectByEstado(Long estado) throws Throwable;

    /**
     * Préstamos VIGENTES de una entidad: todos los que NO están en uno de los tres estados
     * terminales — 3 CANCELADO, 4 CANCELADO_ANTICIPADO, 5 CANCELADO_POR_NOVACION.
     *
     * <p>Filtra por <b>{@code PRSTIDST}</b> ({@code idEstado}), que es el estado operativo del
     * préstamo. <b>Nunca por {@code ESPSCDGO}</b> ({@code estadoPrestamo}): esa columna es la FK
     * al catálogo {@code CRD.ESPS} y filtrar por ella devuelve resultados vacíos o
     * silenciosamente incorrectos.</p>
     *
     * <p>Los préstamos con {@code idEstado} nulo SÍ entran: un estado sin poblar no es un estado
     * terminal, y este método alimenta un aviso informativo donde subreportar deuda es peor que
     * sobrereportarla.</p>
     *
     * <p>Usado por {@code GET /rest/dvap/deudaVigente/{idEntidad}} para avisar al operador de la
     * deuda del partícipe antes de devolverle sus aportes. Es un AVISO: no bloquea nada.</p>
     *
     * @param codigoEntidad ID numérico de la entidad (partícipe)
     * @return Listado de préstamos no terminales de la entidad; lista VACÍA si no tiene ninguno
     *         o si falla la consulta (el aviso nunca debe romper la pantalla)
     * @throws Throwable Si ocurre algún error
     */
    List<Prestamo> selectVigentesByEntidad(Long codigoEntidad) throws Throwable;

    /**
     * Cuenta los préstamos de una entidad que estén en estado vigente (2), en mora (8) o plazo vencido (11).
     * Usado en la generación del G45 para determinar si una entidad tiene más de un préstamo activo.
     * @param codigoEntidad ID numérico de la entidad
     * @return cantidad de préstamos en esos estados para la entidad
     */
    long countVigentesMoraVencidosByEntidad(Long codigoEntidad) throws Throwable;

    /**
     * Verifica si una entidad tiene algún préstamo cancelado (3) o cancelado anticipado (4)
     * cuya última cuota (MAX numeroCuota) tenga fechaVencimiento >= fechaInicio del período de ejecución.
     * Usado en G45 para excluir entidades con préstamos cancelados en o después del inicio del período.
     * @param codigoEntidad ID numérico de la entidad
     * @param fechaInicio   Primer día del mes de ejecución
     * @return cantidad de préstamos cancelados/cancelados anticipados con última cuota >= fechaInicio
     */
    long countPrestamosConUltimaCuotaEnPeriodoByEntidad(Long codigoEntidad, java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable;
}

