package com.saa.ejb.cxp.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.ReembolsoFacturaCompraDaoService;
import com.saa.model.cxp.ReembolsoFacturaCompra;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ReembolsoFacturaCompraDaoServiceImpl extends EntityDaoImpl<ReembolsoFacturaCompra>
        implements ReembolsoFacturaCompraDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[] {
            "id", "factura", "tipoIdentificacionProveedor", "identificacionProveedor",
            "codPaisPago", "tipoProveedor", "codDoc", "establecimiento", "puntoEmision",
            "secuencial", "fechaEmision", "numeroAutorizacion", "baseImponibleCero",
            "baseImponibleGravada", "tarifaIva", "valorIva", "valorIce", "total",
            "producto", "origen", "estado", "observacion"
        };
    }

    @Override
    public List<ReembolsoFacturaCompra> selectByFactura(Long idFactura) {
        try {
            return em.createNamedQuery("ReembolsoFacturaCompraByFactura", ReembolsoFacturaCompra.class)
                    .setParameter("idFactura", idFactura).getResultList();
        } catch (Exception e) {
            System.out.println("Error en selectByFactura ReembolsoFacturaCompra: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
