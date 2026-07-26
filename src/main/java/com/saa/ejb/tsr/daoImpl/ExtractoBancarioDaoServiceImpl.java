/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.ExtractoBancarioDaoService;
import com.saa.model.tsr.ExtractoBancario;
import com.saa.rubros.ASPEstadoCargaExtracto;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion ExtractoBancarioDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class ExtractoBancarioDaoServiceImpl extends EntityDaoImpl<ExtractoBancario>
        implements ExtractoBancarioDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) ExtractoBancario");
        return new String[]{
            "codigo",
            "cuentaBancaria",
            "empresa",
            "archivoNombre",
            "archivoHash",
            "formato",
            "parser",
            "fechaDesde",
            "fechaHasta",
            "saldoInicial",
            "saldoFinal",
            "estadoCarga",
            "observaciones",
            "fechaCreacion",
            "usuarioCreacion",
            "estado"
        };
    }

    @Override
    public ExtractoBancario selectByHash(String hash) throws Throwable {
        System.out.println("Ingresa al metodo selectByHash con hash: " + hash);
        try {
            Query query = em.createNamedQuery("ExtractoBancarioByHash");
            query.setParameter("hash", hash);
            return (ExtractoBancario) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<ExtractoBancario> selectByCuentaYCobertura(Long idCuenta, LocalDate primerDia, LocalDate ultimoDia)
            throws Throwable {
        System.out.println("Ingresa al metodo selectByCuentaYCobertura con idCuenta: " + idCuenta
                + ", primerDia: " + primerDia + ", ultimoDia: " + ultimoDia);
        Query query = em.createQuery(
            " select e from ExtractoBancario e " +
            " where e.cuentaBancaria.codigo = :idCuenta " +
            " and e.fechaDesde <= :ultimoDia " +
            " and e.fechaHasta >= :primerDia " +
            " order by e.fechaDesde");
        query.setParameter("idCuenta", idCuenta);
        query.setParameter("primerDia", primerDia);
        query.setParameter("ultimoDia", ultimoDia);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Long> selectCuentasConCobertura(List<Long> idsCuenta, LocalDate primerDia, LocalDate ultimoDia)
            throws Throwable {
        System.out.println("Ingresa al metodo selectCuentasConCobertura con " + idsCuenta.size()
                + " cuentas, primerDia: " + primerDia + ", ultimoDia: " + ultimoDia);
        if (idsCuenta.isEmpty()) {
            return List.of();
        }
        Query query = em.createQuery(
            " select distinct e.cuentaBancaria.codigo from ExtractoBancario e " +
            " where e.cuentaBancaria.codigo in :idsCuenta " +
            " and e.fechaDesde <= :ultimoDia " +
            " and e.fechaHasta >= :primerDia " +
            " and e.estado = :estadoActivo " +
            " and e.estadoCarga <> :estadoError ");
        query.setParameter("idsCuenta", idsCuenta);
        query.setParameter("primerDia", primerDia);
        query.setParameter("ultimoDia", ultimoDia);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("estadoError", Long.valueOf(ASPEstadoCargaExtracto.ERROR));
        return query.getResultList();
    }
}
