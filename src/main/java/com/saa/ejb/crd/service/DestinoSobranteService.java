package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.DestinoSobrante;

import jakarta.ejb.Local;

/**
 * Destino del sobrante por partícipe en una carga Petro/ARCH (CRD.DSSB, B8). Toda escritura exige un usuario válido,
 * que se valida LO PRIMERO con {@code UsuarioValidacionService}: sin usuario válido no se graba nada.
 */
@Local
public interface DestinoSobranteService extends EntityService<DestinoSobrante> {

    /**
     * Registra o reemplaza el destino del sobrante de un partícipe en una carga. Si el partícipe ya tenía un destino
     * activo en la carga se anula y queda el nuevo (un solo activo por carga y partícipe).
     *
     * @param destino   carga, entidad, tipoDestino y, según el tipo, prestamo (opcional, destino 1) o tipoAporte
     *                  (obligatorio, destino 2: 9 jubilación, 11 cesantía); destino 3 no lleva ninguno
     * @param usuario   quien registra; se valida primero
     * @throws com.saa.basico.util.UsuarioInvalidoException si el usuario falta o no existe
     * @throws com.saa.basico.util.IncomeException          si la carga ya fue procesada o los datos no cuadran
     */
    DestinoSobrante registrar(DestinoSobrante destino, String usuario) throws Throwable;

    /**
     * Anula un destino que todavía no se aplicó (carga sin procesar y sin valor aplicado).
     *
     * @param usuario quien anula; se valida primero
     */
    DestinoSobrante anular(Long idDestino, String usuario) throws Throwable;

    /** Todos los destinos de la carga, activos y anulados. */
    List<DestinoSobrante> selectByCarga(Long idCarga) throws Throwable;
}
