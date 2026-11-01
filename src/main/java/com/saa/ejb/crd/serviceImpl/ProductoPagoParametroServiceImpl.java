package com.saa.ejb.crd.serviceImpl;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.ProductoPagoParametroService;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cxp.ProductoPago;
import com.saa.rubros.Estado;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ProductoPagoParametroServiceImpl implements ProductoPagoParametroService {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private DetalleRubroService detalleRubroService;

    @Override
    public Long resolver(int alternoParametro, PlanCuenta cuentaEsperada, String que, String codigoError)
            throws Throwable {
        Double valor = null;
        try {
            valor = detalleRubroService.selectValorNumericoByRubAltDetAlt(Rubros.CRD_PARAMETROS_CONTABILIDAD, alternoParametro);
        } catch (Throwable e) {
            System.err.println(codigoError + ": no se pudo leer el parámetro " + alternoParametro + " del rubro "
                + Rubros.CRD_PARAMETROS_CONTABILIDAD + ": " + e.getMessage());
        }
        if (valor == null || valor.doubleValue() <= 0.0) {
            throw new IncomeException(codigoError + ": falta el parámetro con el id del producto de pago de la " + que
                + " (rubro " + Rubros.CRD_PARAMETROS_CONTABILIDAD + ", detalle " + alternoParametro
                + ", valor numérico = id de PGS.PRDP).");
        }
        Long idProducto = Long.valueOf(valor.longValue());
        if (cuentaEsperada == null || cuentaEsperada.getCodigo() == null) {
            throw new IncomeException(codigoError + ": la línea de la plantilla de la " + que + " no tiene cuenta contable;"
                + " no se puede verificar el producto de pago " + idProducto + ".");
        }
        ProductoPago producto = em.find(ProductoPago.class, idProducto);
        if (producto == null) {
            throw new IncomeException(codigoError + ": el producto de pago " + idProducto + " (parámetro " + alternoParametro
                + " del rubro " + Rubros.CRD_PARAMETROS_CONTABILIDAD + ") no existe en PGS.PRDP.");
        }
        if (producto.getEstado() == null || producto.getEstado().longValue() != Estado.ACTIVO) {
            throw new IncomeException(codigoError + ": el producto de pago " + idProducto + " «" + producto.getNombre()
                + "» no está ACTIVO. Actívelo en CXP o corrija el parámetro " + alternoParametro + " del rubro "
                + Rubros.CRD_PARAMETROS_CONTABILIDAD + ".");
        }
        if (producto.getGrupoProducto() == null || producto.getGrupoProducto().getPlanCuenta() == null) {
            throw new IncomeException(codigoError + ": el producto de pago " + idProducto + " «" + producto.getNombre()
                + "» no tiene grupo o el grupo no tiene cuenta contable.");
        }
        PlanCuenta cuentaProducto = producto.getGrupoProducto().getPlanCuenta();
        if (!cuentaEsperada.getCodigo().equals(cuentaProducto.getCodigo())) {
            throw new IncomeException(codigoError + ": la cuenta de la plantilla de la " + que + " ("
                + cuentaEsperada.getCuentaContable() + " - " + cuentaEsperada.getNombre() + ") no coincide con la del grupo"
                + " del producto de pago " + idProducto + " «" + producto.getNombre() + "» ("
                + cuentaProducto.getCuentaContable() + " - " + cuentaProducto.getNombre() + "). Corrija el parámetro "
                + alternoParametro + " del rubro " + Rubros.CRD_PARAMETROS_CONTABILIDAD + ".");
        }
        return idProducto;
    }
}
