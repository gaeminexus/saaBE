package com.saa.model.crd;

import java.io.Serializable;

import com.saa.model.cnt.PlanCuenta;
import com.saa.model.scp.Empresa;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CTAP (CuentaTipoAporte).
 *
 * Cuentas contables por tipo de aporte y empresa — fuente ÚNICA de las cuentas del asiento de
 * RECLASIFICACIÓN de la devolución de aportes (opción C, decisión del usuario 2026-08-31,
 * {@code DevolucionAporteServiceImpl}). Reemplaza al diseño anterior (plantilla 27 por aux1),
 * que solo alcanzaba a tres tipos de aporte fijos: el usuario confirmó que se devuelve
 * CUALQUIER tipo, y son ~16 tipos con cuenta real repartidos en un puñado de cuentas — la
 * misma forma de problema que las bandas de cartera, que ya se resuelve con una tabla de
 * configuración ({@code CRD.BNDP}), no con auxiliares de plantilla. Ver
 * {@code docs/logica-negocio/crd/MAPEO-CUENTAS-TIPO-APORTE.md} §2.
 *
 * <b>Un tipo de aporte SIN fila acá no se puede devolver contablemente</b>: el proceso falla
 * con {@code IncomeException} clara (qué tipo falta, que se configura acá), nunca adivina una
 * cuenta ni la deja en null. Es el comportamiento correcto — un asiento con la cuenta
 * equivocada cuadra igual y el error no se nota (MAPEO-CUENTAS-TIPO-APORTE.md §3).
 *
 * <b>Una fila por (tipo de aporte, empresa)</b> — {@code UK_CTAP_TPAP_PJRQ}. Sin vigencia
 * histórica a propósito: a diferencia de las bandas de cartera, un cambio de cuenta contable
 * de un tipo de aporte no es un evento normativo recurrente; si hiciera falta, se agrega
 * después siguiendo el mismo patrón de {@code ConfiguracionBandaProducto}.
 *
 * DDL en {@code docs/logica-negocio/crd/sql/94_CUENTAS_POR_TIPO_APORTE.sql} — sin ejecutar
 * todavía.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTAP", schema = "CRD")
@SequenceGenerator(name = "SQ_CTAPCDGO", sequenceName = "CRD.SQ_CTAPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "CuentaTipoAporteAll",
                query = "select e from CuentaTipoAporte e"),
    @NamedQuery(name = "CuentaTipoAporteId",
                query = "select e from CuentaTipoAporte e where e.codigo = :id")
})
public class CuentaTipoAporte implements Serializable {

    /** Código de la fila. PK. */
    @Id
    @Basic
    @Column(name = "CTAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CTAPCDGO")
    private Long codigo;

    /** FK - Tipo de aporte (CRD.TPAP) al que aplica esta configuración. */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** FK - Empresa (nodo SCP.PJRQ de nivel empresa). Las cuentas son por empresa. */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * FK - Cuenta de PASIVO del aporte ({@code 2.1.01.xx}/{@code 2.1.02.xx}). Es el DEBE de
     * la reclasificación: baja lo que el fondo le debe al socio.
     */
    @ManyToOne
    @JoinColumn(name = "CTAPPLNP", referencedColumnName = "PLNNCDGO")
    private PlanCuenta cuentaPasivo;

    /**
     * FK - Cuenta de LIQUIDACIÓN por pagar ({@code 2.3.01.xx}). Es el HABER de la
     * reclasificación: nace la obligación de pagarle. Es también la cuenta que CXP debita al
     * pagar, vía el grupo del producto de pago ({@code CRD.TPAP.TPAPPRDP}).
     */
    @ManyToOne
    @JoinColumn(name = "CTAPPLNL", referencedColumnName = "PLNNCDGO")
    private PlanCuenta cuentaLiquidacion;

    /** Estado: 1 = activo, 0 = inactivo. Ver {@link com.saa.rubros.Estado}. */
    @Basic
    @Column(name = "CTAPESTD")
    private Long estado;

    public CuentaTipoAporte() {
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public TipoAporte getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(TipoAporte tipoAporte) {
        this.tipoAporte = tipoAporte;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public PlanCuenta getCuentaPasivo() {
        return cuentaPasivo;
    }

    public void setCuentaPasivo(PlanCuenta cuentaPasivo) {
        this.cuentaPasivo = cuentaPasivo;
    }

    public PlanCuenta getCuentaLiquidacion() {
        return cuentaLiquidacion;
    }

    public void setCuentaLiquidacion(PlanCuenta cuentaLiquidacion) {
        this.cuentaLiquidacion = cuentaLiquidacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
