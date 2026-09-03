package com.saa.ejb.crd.daoImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.TransferenciaCargaPetroDaoService;
import com.saa.model.crd.TransferenciaCargaPetro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class TransferenciaCargaPetroDaoServiceImpl extends EntityDaoImpl<TransferenciaCargaPetro>
        implements TransferenciaCargaPetroDaoService {

    /** 1 = vigente, 0 = anulada (TRCRIDST). */
    private static final long VIGENTE = 1L;

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) TransferenciaCargaPetro");
        return new String[]{
            "codigo",
            "cargaArchivo",
            "cuentaBancaria",
            "banco",
            "bancoExterno",
            "cuentaOrigen",
            "numero",
            "valor",
            "fecha",
            "observacion",
            "idEstado",
            "usuarioRegistro",
            "fechaRegistro",
            "ipRegistro"
        };
    }

    @Override
    public List<TransferenciaCargaPetro> selectVigentesByCarga(Long idCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectVigentesByCarga de TransferenciaCargaPetro"
                + " - carga: " + idCarga);
        Query query = em.createQuery(
                " select t from TransferenciaCargaPetro t " +
                " where  t.cargaArchivo.codigo = :idCarga " +
                " and    t.idEstado            = :vigente " +
                " order by t.codigo");
        query.setParameter("idCarga", idCarga);
        query.setParameter("vigente", VIGENTE);
        return query.getResultList();
    }

    @Override
    public List<TransferenciaCargaPetro> selectByCarga(Long idCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectByCarga de TransferenciaCargaPetro"
                + " - carga: " + idCarga);
        Query query = em.createQuery(
                " select t from TransferenciaCargaPetro t " +
                " where  t.cargaArchivo.codigo = :idCarga " +
                " order by t.codigo");
        query.setParameter("idCarga", idCarga);
        return query.getResultList();
    }

    @Override
    public double sumaValorVigentesByCarga(Long idCarga) throws Throwable {
        System.out.println("Ingresa al metodo sumaValorVigentesByCarga de TransferenciaCargaPetro"
                + " - carga: " + idCarga);
        double suma = 0.0;
        for (TransferenciaCargaPetro transferencia : selectVigentesByCarga(idCarga)) {
            suma += transferencia.getValor() != null ? transferencia.getValor() : 0.0;
        }
        return BigDecimal.valueOf(suma).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public Long resolverEmpresaByCarga(Long idCarga) throws Throwable {
        return resolverEmpresa(selectVigentesByCarga(idCarga));
    }

    @Override
    public Long resolverEmpresa(List<TransferenciaCargaPetro> vigentes) throws Throwable {
        if (vigentes != null) {
            for (TransferenciaCargaPetro transferencia : vigentes) {
                if (transferencia.getCuentaBancaria() != null
                        && transferencia.getCuentaBancaria().getPlanCuenta() != null
                        && transferencia.getCuentaBancaria().getPlanCuenta().getEmpresa() != null) {
                    return transferencia.getCuentaBancaria().getPlanCuenta().getEmpresa().getCodigo();
                }
            }
        }
        throw new com.saa.basico.util.IncomeException("No se pudo determinar la empresa contable:"
                + " ninguna de las transferencias tiene una cuenta bancaria con cuenta contable y"
                + " empresa asignadas.");
    }
}
