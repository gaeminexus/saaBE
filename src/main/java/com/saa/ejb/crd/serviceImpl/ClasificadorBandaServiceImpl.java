package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.BandaProductoDaoService;
import com.saa.ejb.crd.dao.ConfiguracionBandaProductoDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.model.crd.BandaProducto;
import com.saa.model.crd.ConfiguracionBandaProducto;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del clasificador de bandas. Ver {@link ClasificadorBandaService} para la
 * regla de negocio.
 *
 * Es de SOLO LECTURA: no escribe nada, y por eso puede llamarse desde cualquier proceso.
 */
@Stateless
public class ClasificadorBandaServiceImpl implements ClasificadorBandaService {

    /** Días que abarca un período de banda. Fijo por definición del modelo (§8). */
    private static final long DIAS_POR_PERIODO = 30L;

    @EJB
    private ConfiguracionBandaProductoDaoService configuracionBandaProductoDaoService;

    @EJB
    private BandaProductoDaoService bandaProductoDaoService;

    @Override
    public ResultadoClasificacionBanda clasificar(Long idProducto, Long idEmpresa,
            Long tipoCartera, Long dias, LocalDate fecha) throws Throwable {
        System.out.println("Ingresa al metodo (clasificar) ClasificadorBandaService"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera + " dias: " + dias + " fecha: " + fecha);

        if (idProducto == null) {
            throw new IncomeException("El producto es obligatorio para clasificar la banda");
        }
        if (idEmpresa == null) {
            throw new IncomeException("La empresa es obligatoria para clasificar la banda");
        }
        validaTipoCartera(tipoCartera);
        if (dias == null) {
            throw new IncomeException("Los dias son obligatorios para clasificar la banda");
        }
        if (dias < 1L) {
            throw new IncomeException("Los dias deben ser mayores o iguales a 1; se recibio: "
                    + dias);
        }

        LocalDate fechaEfectiva = (fecha != null ? fecha : LocalDate.now());

        ConfiguracionBandaProducto configuracion = configuracionBandaProductoDaoService
                .selectVigente(idProducto, idEmpresa, tipoCartera, fechaEfectiva);
        if (configuracion == null) {
            throw new IncomeException("No hay configuracion de bandas vigente al "
                    + fechaEfectiva + " para el producto " + idProducto
                    + ", empresa " + idEmpresa + ", tipo de cartera "
                    + nombreTipoCartera(tipoCartera));
        }

        List<BandaProducto> bandas = bandaProductoDaoService
                .selectByConfiguracion(configuracion.getCodigo());
        if (bandas.isEmpty()) {
            throw new IncomeException("La configuracion de bandas " + configuracion.getCodigo()
                    + " no tiene bandas activas");
        }

        List<BandaProductoDetalle> detalle = derivarRangos(bandas);
        BandaProductoDetalle bandaEncontrada = clasificarEnBandas(detalle, dias);

        ResultadoClasificacionBanda resultado = new ResultadoClasificacionBanda();
        resultado.setIdConfiguracion(configuracion.getCodigo());
        resultado.setIdProducto(idProducto);
        resultado.setIdEmpresa(idEmpresa);
        resultado.setTipoCartera(tipoCartera);
        resultado.setFecha(fechaEfectiva);
        resultado.setDias(dias);
        resultado.setBanda(bandaEncontrada);
        return resultado;
    }

    @Override
    public List<BandaProductoDetalle> derivarRangos(List<BandaProducto> bandas) throws Throwable {
        System.out.println("Ingresa al metodo (derivarRangos) ClasificadorBandaService"
                + " - bandas: " + (bandas != null ? bandas.size() : 0));

        List<BandaProductoDetalle> resultado = new ArrayList<BandaProductoDetalle>();
        if (bandas == null || bandas.isEmpty()) {
            return resultado;
        }

        // Acumulado de periodos de las bandas ANTERIORES. La lista llega ordenada por
        // numero: recorrerla en otro orden cambia los rangos en silencio.
        long acumulado = 0L;
        for (BandaProducto banda : bandas) {
            BandaProductoDetalle item = new BandaProductoDetalle();
            item.setIdBanda(banda.getCodigo());
            item.setNumero(banda.getNumero());
            item.setPeriodos(banda.getPeriodos());
            item.setEstado(banda.getEstado());
            if (banda.getPlanCuenta() != null) {
                item.setIdPlanCuenta(banda.getPlanCuenta().getCodigo());
                item.setCuentaContable(banda.getPlanCuenta().getCuentaContable());
                item.setNombreCuenta(banda.getPlanCuenta().getNombre());
            }

            long diaInicio = acumulado * DIAS_POR_PERIODO + 1L;
            item.setDiaInicio(Long.valueOf(diaInicio));

            if (banda.getPeriodos() == null) {
                // Banda abierta: se lleva todo lo que exceda la banda anterior.
                item.setDiaFin(null);
                item.setEtiqueta("mas de " + (diaInicio - 1L) + " (resto)");
            } else {
                acumulado = acumulado + banda.getPeriodos().longValue();
                long diaFin = acumulado * DIAS_POR_PERIODO;
                item.setDiaFin(Long.valueOf(diaFin));
                item.setEtiqueta(diaInicio + " - " + diaFin);
            }
            resultado.add(item);
        }
        return resultado;
    }

    @Override
    public BandaProductoDetalle clasificarEnBandas(List<BandaProductoDetalle> bandas, Long dias)
            throws Throwable {
        if (dias == null) {
            throw new IncomeException("Los dias son obligatorios para clasificar la banda");
        }
        if (dias < 1L) {
            throw new IncomeException("Los dias deben ser mayores o iguales a 1; se recibio: "
                    + dias);
        }
        if (bandas == null || bandas.isEmpty()) {
            throw new IncomeException("No hay bandas contra las cuales clasificar");
        }

        for (BandaProductoDetalle banda : bandas) {
            // Primera banda cuyo dia final alcanza los dias buscados. La abierta
            // (diaFin nulo) captura todo lo que llegue hasta ella.
            if (banda.getDiaFin() == null || dias <= banda.getDiaFin().longValue()) {
                return banda;
            }
        }

        // Solo se llega aqui si la configuracion no tiene banda abierta: es un defecto de
        // parametrizacion, no un caso de negocio.
        BandaProductoDetalle ultima = bandas.get(bandas.size() - 1);
        throw new IncomeException("Ninguna banda cubre " + dias
                + " dias: la configuracion termina en el dia " + ultima.getDiaFin()
                + " y no tiene banda abierta");
    }

    /**
     * Valida que el tipo de cartera sea uno de los dos del catálogo.
     *
     * @param tipoCartera : Tipo de cartera recibido
     */
    private void validaTipoCartera(Long tipoCartera) {
        if (tipoCartera == null) {
            throw new IncomeException("El tipo de cartera es obligatorio");
        }
        if (tipoCartera.longValue() != TipoCarteraBanda.POR_VENCER
                && tipoCartera.longValue() != TipoCarteraBanda.VENCIDO) {
            throw new IncomeException("Tipo de cartera invalido: " + tipoCartera
                    + ". Valores permitidos: " + TipoCarteraBanda.POR_VENCER
                    + " = por vencer, " + TipoCarteraBanda.VENCIDO + " = vencido");
        }
    }

    /**
     * Etiqueta legible del tipo de cartera, para los mensajes de error.
     *
     * @param tipoCartera : Tipo de cartera
     * @return            : "POR VENCER" / "VENCIDO"
     */
    private String nombreTipoCartera(Long tipoCartera) {
        if (tipoCartera != null && tipoCartera.longValue() == TipoCarteraBanda.VENCIDO) {
            return "VENCIDO";
        }
        return "POR VENCER";
    }
}
