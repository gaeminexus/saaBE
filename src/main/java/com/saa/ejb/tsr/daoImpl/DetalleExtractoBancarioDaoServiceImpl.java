/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.DetalleExtractoBancarioDaoService;
import com.saa.model.tsr.DetalleExtractoBancario;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion DetalleExtractoBancarioDaoService.
 */
@Stateless
public class DetalleExtractoBancarioDaoServiceImpl extends EntityDaoImpl<DetalleExtractoBancario>
        implements DetalleExtractoBancarioDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DetalleExtractoBancario");
        return new String[]{
            "codigo",
            "extractoBancario",
            "cuentaBancaria",
            "fechaTransaccion",
            "fechaContable",
            "descripcion",
            "referencia",
            "codigoMovimiento",
            "debito",
            "credito",
            "saldo",
            "hash",
            "numeroFila",
            "filaCruda",
            "movimientoConciliado",
            "estadoRevision",
            "fechaCreacion",
            "usuarioCreacion",
            "estado"
        };
    }

    @Override
    public DetalleExtractoBancario selectByCuentaYHash(Long idCuenta, String hash) throws Throwable {
        System.out.println("Ingresa al metodo selectByCuentaYHash con idCuenta: " + idCuenta + ", hash: " + hash);
        try {
            Query query = em.createQuery(
                " select e from DetalleExtractoBancario e " +
                " where e.cuentaBancaria.codigo = :idCuenta " +
                " and e.hash = :hash");
            query.setParameter("idCuenta", idCuenta);
            query.setParameter("hash", hash);
            return (DetalleExtractoBancario) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
