package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.GarantiaOrdenPrestamoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * C31 (16-nov-2026, pedido del líder): «si un préstamo se termina, la garantía se debe dar de baja con el
 * monto otorgado, no con el saldo capital».
 *
 * <p><b>Por qué hacía falta.</b> La entrega (plantillas 9, 13, 34) registra D 7.3.01.05 cartera de créditos
 * (orden) / H 7.4.01.05 documentos en garantía por el monto del préstamo (y H 7.4.01.10/15 por el bien).
 * Antes de C31 ningún camino de terminación (pago de la última cuota por ventanilla, CBCR o Petro/Arch,
 * precancelación con o sin aportes, precancelación con préstamo nuevo, acuerdo de pago con condonación)
 * tocaba esas cuentas, y ninguna cuota pagada las reduce: el pagaré quedaba registrado para siempre. Solo
 * la novación lo daba de baja (C30). La plantilla 26 «LEVANTAMIENTO DE HIPOTECA Y ENTREGA DE PAGARE» existía
 * sin consumidor.</p>
 *
 * <p><b>Criterio exacto.</b> Al terminar el préstamo se da de baja todo lo registrado: el pagaré por el
 * monto otorgado ({@code PRSTMNSL}) y, si es prendario o hipotecario con suma asegurada ({@code PRSTVLAS}
 * &gt; 0), el bien por ese valor; la contrapartida (7.3.01.05) por la suma. Como ninguna cuota reduce la
 * orden, «lo que queda del monto otorgado» es el monto otorgado entero y el saldo del préstamo en orden
 * queda en 0. Si el préstamo no tiene monto otorgado, falla con mensaje (igual que C30).</p>
 *
 * <p><b>Idempotencia y reversos.</b> Cada línea lleva la marca {@code [C31 PRST id]}. El neto de las líneas
 * marcadas en la cuenta del pagaré dice si la baja está vigente; llamarlo dos veces no duplica nada. Si la
 * terminación se revierte (reverso de la operación, reproceso de la carga Petro, reapertura al corte), el
 * asiento de baja se anula con la regla estándar de CNT (anula en período abierto, reversa en el primer
 * período abierto si el suyo está cerrado).</p>
 */
@Stateless
public class GarantiaOrdenPrestamoServiceImpl implements GarantiaOrdenPrestamoService {

    private static final double TOLERANCIA = 0.005;

    /** Posiciones aux1 de la plantilla 26 (ver {@link PlantillasCredito#LEVANTAMIENTO_GARANTIA}). */
    private static final int AUX_ORDEN_CARTERA = 1;
    private static final int AUX_DOCUMENTOS_GARANTIA = 2;
    private static final int AUX_BIEN_INMUEBLE = 3;
    private static final int AUX_BIEN_VEHICULO = 4;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private CierreCarteraDaoService cierreCarteraDaoService;

    /** Marca de las líneas del préstamo (ASCII, sin tildes, para que el LIKE no dependa del juego de caracteres). */
    static String marca(Long idPrestamo) {
        return "[C31 PRST " + idPrestamo + "]";
    }

    @Override
    public Long sincronizar(Prestamo prestamo, Long idEmpresa, LocalDate fecha, String usuario, String causa)
            throws Throwable {
        if (prestamo == null || prestamo.getCodigo() == null || prestamo.getIdEstado() == null) {
            return null;
        }
        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("GarantiaOrdenPrestamoService - contabilidad de CRD inactiva: préstamo "
                + prestamo.getCodigo() + " sin movimiento de garantía en cuentas de orden.");
            return null;
        }
        long estado = prestamo.getIdEstado().longValue();
        boolean terminado = estado == EstadoPrestamo.CANCELADO || estado == EstadoPrestamo.CANCELADO_ANTICIPADO;
        boolean vivo = EstadoPrestamo.esVivo(prestamo.getIdEstado());
        if (!terminado && !vivo) {
            return null;
        }

        Long empresa = resolverEmpresa(prestamo, idEmpresa);
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.LEVANTAMIENTO_GARANTIA, empresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno " + PlantillasCredito.LEVANTAMIENTO_GARANTIA
                + " (levantamiento de garantía) para la empresa " + empresa
                + ": no se puede mover la garantía del préstamo " + prestamo.getCodigo() + " en cuentas de orden.");
        }
        DetallePlantilla lineaDocumentos = lineaPlantilla(idPlantilla, AUX_DOCUMENTOS_GARANTIA);

        List<Object[]> lineas = cierreCarteraDaoService.selectLineasBajaGarantia(prestamo.getCodigo(),
            lineaDocumentos.getPlanCuenta().getCodigo(), marca(prestamo.getCodigo()));
        double neto = 0.0;
        Long ultimaBajaActiva = null;
        for (Object[] fila : lineas) {
            double valor = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
            neto += valor;
            Long estadoAsiento = (Long) fila[1];
            if (valor > TOLERANCIA && estadoAsiento != null && estadoAsiento.intValue() == EstadoAsiento.ACTIVO) {
                ultimaBajaActiva = (Long) fila[0];
            }
        }
        boolean bajaVigente = redondear(neto) > TOLERANCIA;

        if (terminado && !bajaVigente) {
            return darDeBaja(prestamo, empresa, idPlantilla, lineaDocumentos, fecha, usuario, causa);
        }
        if (vivo && bajaVigente) {
            if (ultimaBajaActiva == null) {
                throw new IncomeException("El préstamo " + prestamo.getCodigo() + " volvió a estar vivo y su garantía"
                    + " figura dada de baja en cuentas de orden, pero no se encontró el asiento de baja activo que anular.");
            }
            if (usuario == null || usuario.trim().isEmpty()) {
                throw new IncomeException("Falta el usuario para anular la baja de garantía del préstamo "
                    + prestamo.getCodigo() + ".");
            }
            asientoService.anulaAsiento(ultimaBajaActiva, usuario, "C31: el préstamo " + prestamo.getCodigo()
                + " volvió a estar vigente (" + (causa != null ? causa : "reapertura") + "): se anula la baja de su garantía");
            System.out.println("  ↩️ C31 baja de garantía anulada - préstamo " + prestamo.getCodigo()
                + " - asiento " + ultimaBajaActiva);
            return ultimaBajaActiva;
        }
        return null;
    }

    private Long darDeBaja(Prestamo prestamo, Long empresa, Long idPlantilla, DetallePlantilla lineaDocumentos,
            LocalDate fecha, String usuario, String causa) throws Throwable {
        double monto = redondear(prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0);
        if (monto <= TOLERANCIA) {
            throw new IncomeException("El préstamo " + prestamo.getCodigo() + " no tiene monto entregado (PRSTMNSL):"
                + " no se puede dar de baja su garantía en cuentas de orden.");
        }
        String familia = familia(prestamo);
        double bien = redondear(prestamo.getValorAsegurado() != null ? prestamo.getValorAsegurado() : 0.0);
        Integer auxBien = null;
        if (bien > TOLERANCIA) {
            if ("HIPOTECARIO".equals(familia)) {
                auxBien = AUX_BIEN_INMUEBLE;
            } else if ("PRENDARIO".equals(familia)) {
                auxBien = AUX_BIEN_VEHICULO;
            }
        }
        if (auxBien == null) {
            bien = 0.0;
        }

        String texto = "Baja de garantía préstamo " + prestamo.getCodigo()
            + (prestamo.getIdAsoprep() != null ? " (N° " + prestamo.getIdAsoprep() + ")" : "")
            + (causa != null ? " - " + causa : "");
        if (texto.length() > 120) {
            texto = texto.substring(0, 120);
        }
        // La marca va AL PRINCIPIO: la descripción se trunca a 200 y la marca no puede perderse.
        String prefijo = marca(prestamo.getCodigo()) + " " + texto;

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(linea(lineaPlantilla(idPlantilla, AUX_ORDEN_CARTERA), redondear(monto + bien), false,
            prefijo + " - cartera de créditos (orden)"));
        lineas.add(linea(lineaDocumentos, monto, true, prefijo + " - pagaré por el monto otorgado"));
        if (auxBien != null) {
            lineas.add(linea(lineaPlantilla(idPlantilla, auxBien), bien, true, prefijo + " - bien en garantía"));
        }

        LocalDate fechaAsiento = fecha != null ? fecha : LocalDate.now();
        Asiento asiento = asientoContableService.generarAsiento(empresa, TipoAsientos.CREDITOS, fechaAsiento,
            "Baja de garantía en cuentas de orden - préstamo " + prestamo.getCodigo()
                + (causa != null ? " (" + causa + ")" : ""),
            usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
        System.out.println("  ✅ C31 baja de garantía - préstamo " + prestamo.getCodigo() + " - pagaré $" + monto
            + (bien > 0 ? " - bien $" + bien : "") + " - asiento " + asiento.getCodigo() + " al " + fechaAsiento);
        return asiento.getCodigo();
    }

    /** Empresa contable: la recibida; si no, la del asiento de entrega; si no, la única con la plantilla 26. */
    private Long resolverEmpresa(Prestamo prestamo, Long idEmpresa) throws Throwable {
        if (idEmpresa != null) {
            return idEmpresa;
        }
        if (prestamo.getIdAsientoEntrega() != null) {
            Asiento entrega = asientoService.selectById(prestamo.getIdAsientoEntrega());
            if (entrega != null && entrega.getEmpresa() != null) {
                return entrega.getEmpresa().getCodigo();
            }
        }
        List<Long> empresas = cierreCarteraDaoService.selectEmpresasConPlantilla(PlantillasCredito.LEVANTAMIENTO_GARANTIA);
        if (empresas.size() == 1) {
            return empresas.get(0);
        }
        throw new IncomeException("No se pudo determinar la empresa contable para mover la garantía del préstamo "
            + prestamo.getCodigo() + " en cuentas de orden (" + empresas.size() + " empresas con la plantilla "
            + PlantillasCredito.LEVANTAMIENTO_GARANTIA + ").");
    }

    private String familia(Prestamo prestamo) {
        if (prestamo.getProducto() == null || prestamo.getProducto().getTipoPrestamo() == null
                || prestamo.getProducto().getTipoPrestamo().getNombre() == null) {
            return "";
        }
        return prestamo.getProducto().getTipoPrestamo().getNombre().trim().toUpperCase();
    }

    private DetallePlantilla lineaPlantilla(Long idPlantilla, int aux1) throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.LEVANTAMIENTO_GARANTIA
                + " no tiene la línea aux1=" + aux1 + ".");
        }
        return linea;
    }

    private DetalleAsiento linea(DetallePlantilla plantilla, double valor, boolean debe, String descripcion) {
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion.length() > 200 ? descripcion.substring(0, 200) : descripcion);
        detalle.setValorDebe(debe ? redondear(valor) : 0.0);
        detalle.setValorHaber(debe ? 0.0 : redondear(valor));
        return detalle;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
