package com.saa.model.historico;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lote mensual de la carga histórica de nómina (C13, cuadre Superintendencia ASOPREP): el {@code lote.json} que
 * genera {@code 03-trabajo/nomina/scripts/02-genera-lotes.js} (repo cuadreSuper) desde los roles del contador.
 * Los valores son los del Excel: el cargador no recalcula nada. Contrato completo en
 * docs/logica-negocio/rhh/CARGA-HISTORICA-NOMINA.md.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoteHistoricoNomina {

    public String mes;
    public String etiqueta;
    public Long idEmpresa;
    public Long idUsuario;
    public String usuario;
    public boolean soloValidar;
    /** Si es false, los pagos marcados {@code inferida} se rechazan (fecha de pago sin evidencia directa). */
    public boolean aceptarInferidos;
    public String observacion;
    /** Centro de costo (CNT.CNCS.CNCSNMRO) que se impone en las líneas de gasto 4.3.* de los asientos de rol y provisiones. */
    public String centroCosto;
    public String fuente;
    public PeriodoLote periodo;
    public List<EmpleadoLote> empleados = new ArrayList<>();
    public List<RolLote> roles = new ArrayList<>();
    public List<ProvisionLote> provisiones = new ArrayList<>();
    public List<PagoLote> pagos = new ArrayList<>();
    public List<PlanillaIessLote> planillasIess = new ArrayList<>();
    /** Totales de control que calcula el generador; el cargador los compara con lo que graba. */
    public ControlLote control;

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class PeriodoLote {
        public Integer anio;
        public Integer mes;
        public String fechaInicio;
        public String fechaFin;
        /** Fecha de los asientos de rol y provisiones (último día del mes). */
        public String fechaContable;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class EmpleadoLote {
        /** Código interno estable (Ennn) del inventario de roles; es lo único que sale en el resultado. */
        public String ref;
        public String identificacion;
        public String apellidos;
        public String nombres;
        public Long idEmpleadoSaa;
        public String fechaIngreso;
        public String fechaSalida;
        public Double sueldo;
        public String cargo;
        public Long modalidadDecimoTercero;
        public Long modalidadDecimoCuarto;
        public Long modalidadFondosReserva;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class RolLote {
        public String ref;
        public String identificacion;
        public Double dias;
        public Double sueldo;
        public Double totalIngresos;
        public Double totalDescuentos;
        public Double neto;
        public Double baseIess;
        public Double aportePersonal;
        public Double aportePatronal;
        public Double fondosReserva;
        public List<RenglonLote> renglones = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class RenglonLote {
        /** Abreviatura del concepto (RHH.CPNM.CPNMABRV): SUELDO, FRES, APIESS, QUIROG, HIPOTE, APPATR, ... */
        public String concepto;
        public Double valor;
        public String descripcion;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class ProvisionLote {
        public String ref;
        public String identificacion;
        /** D13, D14, VAC, FR. */
        public String tipo;
        public String concepto;
        public Double valor;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class PagoLote {
        public String ref;
        public String fecha;
        public Long idCuentaBancaria;
        public String banco;
        public boolean inferida;
        public String motivo;
        public Double total;
        public List<DetallePagoLote> detalle = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class DetallePagoLote {
        public String ref;
        public String identificacion;
        public Double valor;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class PlanillaIessLote {
        public String ref;
        /** 1 aportes, 2 quirografarios, 3 hipotecarios, 4 fondos de reserva (rubro 330). */
        public Long tipo;
        public String numeroComprobante;
        public String fechaEmision;
        public String fechaMaximaPago;
        public Double valorIess;
        public List<ComponentePlanillaLote> componentes = new ArrayList<>();
        /** Nulo si no hay débito identificado: la planilla se registra sin pago. */
        public PagoPlanillaLote pago;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class ComponentePlanillaLote {
        public String concepto;
        public Long conceptoTipo;
        public Double valor;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class PagoPlanillaLote {
        public String fecha;
        public Long idCuentaBancaria;
        /** Líneas al DEBE (pasivos que se cancelan); el HABER es la cuenta contable de la cuenta bancaria por el total. */
        public List<LineaPagoLote> lineas = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class LineaPagoLote {
        public String cuenta;
        public Double valor;
        public String descripcion;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)

    public static class ControlLote {
        public Integer empleados;
        public Double totalIngresos;
        public Double totalDescuentos;
        public Double neto;
        public Double aportePatronal;
        public Double pagos;
        public Double pagosInferidos;
    }
}
