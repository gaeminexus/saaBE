package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ContratoDaoService;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.dao.VigenciaContratoDaoService;
import com.saa.ejb.crd.service.VigenciaContratoService;
import com.saa.ejb.crd.service.dto.SolicitudVigenciaContrato;
import com.saa.ejb.crd.service.dto.VigenciaDTO;
import com.saa.model.crd.Contrato;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Participe;
import com.saa.model.crd.TipoAporte;
import com.saa.model.crd.VigenciaContrato;
import com.saa.rubros.CrdModoVigenciaContrato;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see VigenciaContratoService
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Stateless
public class VigenciaContratoServiceImpl implements VigenciaContratoService {

    /** Código CRD.TPAP de jubilación. Verificado contra la base el 2026-08-27. */
    private static final Long JUBILACION = 9L;
    /** Código CRD.TPAP de cesantía. Verificado contra la base el 2026-08-27. */
    private static final Long CESANTIA = 11L;

    @EJB
    private VigenciaContratoDaoService vigenciaContratoDaoService;

    @EJB
    private ContratoDaoService contratoDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private ParticipeDaoService participeDaoService;

    @Override
    public VigenciaContrato selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById VigenciaContrato con id: " + id);
        return vigenciaContratoDaoService.selectById(id, NombreEntidadesCredito.VIGENCIA_CONTRATO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de VigenciaContratoService");
        VigenciaContrato vigencia = new VigenciaContrato();
        for (Long registro : id) {
            vigenciaContratoDaoService.remove(vigencia, registro);
        }
    }

    @Override
    public void save(List<VigenciaContrato> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de VigenciaContratoService");
        for (VigenciaContrato registro : lista) {
            vigenciaContratoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<VigenciaContrato> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll VigenciaContratoService");
        List<VigenciaContrato> result = vigenciaContratoDaoService.selectAll(NombreEntidadesCredito.VIGENCIA_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total VigenciaContrato no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public VigenciaContrato saveSingle(VigenciaContrato vigencia) throws Throwable {
        System.out.println("saveSingle - VigenciaContrato");
        if (vigencia.getIdEstado() == null) {
            vigencia.setIdEstado(Long.valueOf(Estado.ACTIVO));
        }
        return vigenciaContratoDaoService.save(vigencia, vigencia.getCodigo());
    }

    @Override
    public List<VigenciaContrato> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria VigenciaContratoService");
        List<VigenciaContrato> result = vigenciaContratoDaoService.selectByCriteria(datos, NombreEntidadesCredito.VIGENCIA_CONTRATO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio VigenciaContrato no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<VigenciaDTO> selectByContrato(Long idContrato) throws Throwable {
        System.out.println("selectByContrato VigenciaContratoService - idContrato: " + idContrato);
        List<VigenciaDTO> resultado = new ArrayList<>();
        for (VigenciaContrato vigencia : vigenciaContratoDaoService.selectByContrato(idContrato)) {
            resultado.add(aDTO(vigencia));
        }
        return resultado;
    }

    @Override
    public VigenciaDTO crear(SolicitudVigenciaContrato solicitud) throws Throwable {
        System.out.println("crear VigenciaContratoService - idContrato: "
            + (solicitud != null ? solicitud.getIdContrato() : null)
            + " - idTipoAporte: " + (solicitud != null ? solicitud.getIdTipoAporte() : null));

        if (solicitud == null || solicitud.getIdContrato() == null || solicitud.getIdTipoAporte() == null
                || solicitud.getFechaInicio() == null || solicitud.getModo() == null || solicitud.getMonto() == null) {
            throw new IncomeException("idContrato, idTipoAporte, fechaInicio, modo y monto son obligatorios");
        }
        if (!JUBILACION.equals(solicitud.getIdTipoAporte()) && !CESANTIA.equals(solicitud.getIdTipoAporte())) {
            throw new IncomeException("idTipoAporte invalido: solo se admiten vigencias de tipo "
                + JUBILACION + " (jubilacion) u " + CESANTIA + " (cesantia)");
        }
        if (solicitud.getMonto() < 0) {
            throw new IncomeException("El monto de la vigencia no puede ser negativo");
        }
        long modo = solicitud.getModo().longValue();
        if (modo != CrdModoVigenciaContrato.CALCULADO && modo != CrdModoVigenciaContrato.FIJO) {
            throw new IncomeException("modo invalido: solo se admite "
                + CrdModoVigenciaContrato.CALCULADO + " (CALCULADO) o " + CrdModoVigenciaContrato.FIJO + " (FIJO)");
        }

        Contrato contrato = contratoDaoService.selectById(solicitud.getIdContrato(), NombreEntidadesCredito.CONTRATO);
        TipoAporte tipoAporte = tipoAporteDaoService.selectById(solicitud.getIdTipoAporte(), NombreEntidadesCredito.TIPO_APORTE);

        Double porcentaje = null;
        Double remuneracion = null;
        if (modo == CrdModoVigenciaContrato.CALCULADO) {
            if (solicitud.getPorcentaje() == null) {
                throw new IncomeException("El modo CALCULADO requiere el porcentaje a aplicar");
            }
            List<Participe> participes = participeDaoService.selectByEntidad(contrato.getEntidad().getCodigo());
            Double remuneracionUnificada = (!participes.isEmpty()) ? participes.get(0).getRemuneracionUnificada() : null;
            if (remuneracionUnificada == null) {
                throw new IncomeException("No se puede crear una vigencia CALCULADA: el participe de este"
                    + " contrato no tiene remuneracion unificada registrada (PRTCRMUN)");
            }
            porcentaje = solicitud.getPorcentaje();
            remuneracion = remuneracionUnificada;
        }
        // Modo FIJO: porcentaje/remuneracion quedan null aunque el request traiga un
        // porcentaje — D8 dice que en FIJO el monto es el que manda, sin recalculo.

        VigenciaContrato abierta = vigenciaContratoDaoService.selectAbierta(contrato.getCodigo(), tipoAporte.getCodigo());
        if (abierta != null) {
            if (!solicitud.getFechaInicio().isAfter(abierta.getFechaInicio())) {
                throw new IncomeException("La nueva vigencia debe iniciar despues de la vigencia abierta"
                    + " actual (que inicio el " + abierta.getFechaInicio() + ")");
            }
            abierta.setFechaFin(solicitud.getFechaInicio().minusDays(1));
            vigenciaContratoDaoService.save(abierta, abierta.getCodigo());
        }

        VigenciaContrato nueva = new VigenciaContrato();
        nueva.setContrato(contrato);
        nueva.setTipoAporte(tipoAporte);
        nueva.setFechaInicio(solicitud.getFechaInicio());
        nueva.setFechaFin(null);
        nueva.setMonto(solicitud.getMonto());
        nueva.setPorcentaje(porcentaje);
        nueva.setRemuneracion(remuneracion);
        nueva.setModo(solicitud.getModo());
        nueva.setIdHistorialSueldo(null);
        nueva.setObservacion(solicitud.getObservacion());
        nueva.setIdEstado(Long.valueOf(Estado.ACTIVO));
        nueva.setUsuarioRegistro(solicitud.getUsuario());
        nueva.setFechaRegistro(LocalDateTime.now());
        nueva = vigenciaContratoDaoService.save(nueva, null);

        actualizarEspejo(contrato);

        return aDTO(nueva);
    }

    @Override
    public void anular(Long idVigencia, String usuario) throws Throwable {
        System.out.println("anular VigenciaContratoService - idVigencia: " + idVigencia + " - usuario: " + usuario);
        VigenciaContrato vigencia = vigenciaContratoDaoService.selectById(idVigencia, NombreEntidadesCredito.VIGENCIA_CONTRATO);
        if (vigencia.getFechaFin() != null) {
            throw new IncomeException("Solo se puede anular la vigencia abierta; esta vigencia ya"
                + " esta cerrada (fechaFin=" + vigencia.getFechaFin() + ")");
        }
        if (Long.valueOf(Estado.INACTIVO).equals(vigencia.getIdEstado())) {
            throw new IncomeException("La vigencia ya esta anulada");
        }
        vigencia.setIdEstado(Long.valueOf(Estado.INACTIVO));
        vigenciaContratoDaoService.save(vigencia, vigencia.getCodigo());
        actualizarEspejo(vigencia.getContrato());
    }

    @Override
    public double esperado(Long idContrato, Long idTipoAporte, LocalDate mes) throws Throwable {
        if (idContrato == null || idTipoAporte == null || mes == null) {
            return 0.0;
        }
        LocalDate ultimoDiaMes = YearMonth.from(mes).atEndOfMonth();
        VigenciaContrato vigente = vigenciaContratoDaoService.selectVigenteEnFecha(idContrato, idTipoAporte, ultimoDiaMes);
        return (vigente != null && vigente.getMonto() != null) ? vigente.getMonto() : 0.0;
    }

    @Override
    public double esperadoPorEntidad(Long idEntidad, Long idTipoAporte, LocalDate mes) throws Throwable {
        if (idEntidad == null) {
            return 0.0;
        }
        Contrato contrato = contratoDaoService.selectActivoPorEntidad(idEntidad);
        if (contrato == null) {
            return 0.0;
        }
        return esperado(contrato.getCodigo(), idTipoAporte, mes);
    }

    @Override
    public Map<String, Double> esperadoEnLotePorFilial(Long codigoFilial, LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("esperadoEnLotePorFilial VigenciaContratoService - codigoFilial: " + codigoFilial
            + " - desde: " + desde + " - hasta: " + hasta);

        Map<String, Double> resultado = new HashMap<>();
        if (codigoFilial == null || desde == null || hasta == null) {
            return resultado;
        }

        // Agrupa los rangos de vigencia por (entidad,tipo) para resolver la cobertura de
        // cada mes en memoria, sin volver a tocar la base.
        Map<String, List<Object[]>> rangosPorEntidadTipo = new HashMap<>();
        for (Object[] fila : vigenciaContratoDaoService.selectVigentesPorFilial(codigoFilial)) {
            Long idEntidad = (Long) fila[0];
            Long idTipoAporte = (Long) fila[1];
            if (idEntidad == null || idTipoAporte == null) {
                continue;
            }
            rangosPorEntidadTipo.computeIfAbsent(idEntidad + "|" + idTipoAporte, k -> new ArrayList<>()).add(fila);
        }

        LocalDate limite = YearMonth.from(hasta).atDay(1);
        for (Map.Entry<String, List<Object[]>> entrada : rangosPorEntidadTipo.entrySet()) {
            LocalDate mes = YearMonth.from(desde).atDay(1);
            while (!mes.isAfter(limite)) {
                LocalDate ultimoDiaMes = YearMonth.from(mes).atEndOfMonth();
                // Misma regla que selectVigenteEnFecha: fechaInicio <= fecha AND
                // (fechaFin es null O fechaFin >= fecha), evaluada contra el ultimo dia del mes.
                //
                // CORRECCIONES 2026-09-02 (mismo criterio que esperadoDesdeCache en
                // CargaArchivoPetroServiceImpl, ver CORRECCION-TORMENTA-CONSULTAS-VIGENCIA.md):
                // este es el camino del LOTE, el que resuelve la MAYORÍA de los casos — tener la
                // protección solo en el fallback (minoritario) daba una sensación de cobertura
                // que no existía. Si hay más de una vigencia ACTIVA superpuesta del mismo tipo
                // para el mismo contrato, falla en vez de elegir la primera en silencio.
                Object[] filaEncontrada = null;
                for (Object[] fila : entrada.getValue()) {
                    LocalDate fechaInicio = (LocalDate) fila[2];
                    LocalDate fechaFin = (LocalDate) fila[3];
                    if (fechaInicio != null && !ultimoDiaMes.isBefore(fechaInicio)
                            && (fechaFin == null || !ultimoDiaMes.isAfter(fechaFin))) {
                        if (filaEncontrada != null) {
                            Long idContrato = (Long) fila[5];
                            Long idTipoAporteConflicto = (Long) fila[1];
                            throw new IncomeException("Hay más de una vigencia ACTIVA del tipo de"
                                + " aporte " + idTipoAporteConflicto + " cubriendo el " + ultimoDiaMes
                                + " para el contrato " + idContrato + " — vigencias "
                                + filaEncontrada[6] + " y " + fila[6] + " en conflicto. No se puede"
                                + " decidir cuál vale.");
                        }
                        filaEncontrada = fila;
                    }
                }
                double monto = filaEncontrada != null && filaEncontrada[4] != null
                    ? (Double) filaEncontrada[4] : 0.0;
                resultado.put(entrada.getKey() + "|" + mes, monto);
                mes = mes.plusMonths(1);
            }
        }

        return resultado;
    }

    /**
     * Actualiza CNTR.CNTRMNAJ/CNTRMNAC/CNTRPRAJ/CNTRPRAI con la vigencia ABIERTA de cada
     * tipo (null si no hay ninguna abierta de ese tipo). Es el único punto que escribe el
     * espejo — ver la trampa documentada en CNTRPRAI (cesantía) / CNTRPRAJ (jubilación).
     */
    private void actualizarEspejo(Contrato contrato) throws Throwable {
        VigenciaContrato jubilacion = vigenciaContratoDaoService.selectAbierta(contrato.getCodigo(), JUBILACION);
        VigenciaContrato cesantia = vigenciaContratoDaoService.selectAbierta(contrato.getCodigo(), CESANTIA);

        contrato.setMontoAporteJubilacion(jubilacion != null ? jubilacion.getMonto() : null);
        contrato.setPorcentajeAporteJubilacion(jubilacion != null ? jubilacion.getPorcentaje() : null);
        contrato.setMontoAporteCesantia(cesantia != null ? cesantia.getMonto() : null);
        contrato.setPorcentajeAporteIndividual(cesantia != null ? cesantia.getPorcentaje() : null);

        contratoDaoService.save(contrato, contrato.getCodigo());
    }

    private VigenciaDTO aDTO(VigenciaContrato vigencia) {
        VigenciaDTO dto = new VigenciaDTO();
        dto.setIdVigencia(vigencia.getCodigo());
        dto.setIdContrato(vigencia.getContrato() != null ? vigencia.getContrato().getCodigo() : null);
        dto.setIdTipoAporte(vigencia.getTipoAporte() != null ? vigencia.getTipoAporte().getCodigo() : null);
        dto.setNombreTipoAporte(vigencia.getTipoAporte() != null ? vigencia.getTipoAporte().getNombre() : null);
        dto.setFechaInicio(vigencia.getFechaInicio());
        dto.setFechaFin(vigencia.getFechaFin());
        dto.setMonto(vigencia.getMonto());
        dto.setPorcentaje(vigencia.getPorcentaje());
        dto.setRemuneracion(vigencia.getRemuneracion());
        dto.setModo(vigencia.getModo());
        if (vigencia.getModo() != null && vigencia.getModo().longValue() == CrdModoVigenciaContrato.CALCULADO) {
            dto.setModoTexto("CALCULADO");
        } else if (vigencia.getModo() != null && vigencia.getModo().longValue() == CrdModoVigenciaContrato.FIJO) {
            dto.setModoTexto("FIJO");
        }
        dto.setEstado(vigencia.getIdEstado());
        dto.setObservacion(vigencia.getObservacion());
        return dto;
    }
}
