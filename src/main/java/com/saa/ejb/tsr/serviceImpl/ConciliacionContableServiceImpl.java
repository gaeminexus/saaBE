/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.tsr.dao.ConciliacionContableDaoService;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionAsientoDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionContableDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionExtractoDaoService;
import com.saa.ejb.tsr.service.ConciliacionContableService;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.model.cnt.Periodo;
import com.saa.model.tsr.ConciliacionContable;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoConciliacionContable;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de ConciliacionContableService.</p>
 */
@Stateless
public class ConciliacionContableServiceImpl implements ConciliacionContableService {

    @EJB
    private ConciliacionContableDaoService conciliacionContableDaoService;

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private PeriodoService periodoService;

    @EJB
    private GrupoConciliacionContableDaoService grupoConciliacionContableDaoService;

    @EJB
    private GrupoConciliacionExtractoDaoService grupoConciliacionExtractoDaoService;

    @EJB
    private GrupoConciliacionAsientoDaoService grupoConciliacionAsientoDaoService;

    @EJB
    private ControlExtractoBancarioService controlExtractoBancarioService;

    @Override
    public ConciliacionContable obtenerOCrear(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo obtenerOCrear con idCuentaBancaria: " + idCuentaBancaria
                + ", idPeriodo: " + idPeriodo);
        ConciliacionContable existente = conciliacionContableDaoService
                .selectByCuentaYPeriodo(idCuentaBancaria, idPeriodo);
        if (existente != null) {
            return existente;
        }

        CuentaBancaria cuenta = cuentaBancariaDaoService.recuperaBancoCuenta(idCuentaBancaria);
        if (cuenta == null) {
            throw new IncomeException("No se encontro la cuenta bancaria con id " + idCuentaBancaria);
        }
        Periodo periodo = periodoService.selectById(idPeriodo);
        if (periodo == null) {
            throw new IncomeException("No se encontro el periodo contable con id " + idPeriodo);
        }

        ConciliacionContable nueva = new ConciliacionContable();
        nueva.setCuentaBancaria(cuenta);
        nueva.setPeriodo(periodo);
        nueva.setEstadoRevision((long) EstadoConciliacionContable.PENDIENTE);
        nueva.setTotalGrupos(0L);
        nueva.setTotalPendientesExtracto(0L);
        nueva.setTotalPendientesAsiento(0L);
        nueva.setFechaCreacion(LocalDateTime.now());
        nueva.setEstado((long) Estado.ACTIVO);
        return conciliacionContableDaoService.save(nueva, null);
    }

    @Override
    public void recalcularContadores(Long idConciliacionContable) throws Throwable {
        System.out.println("Ingresa al metodo recalcularContadores con idConciliacionContable: "
                + idConciliacionContable);
        ConciliacionContable conciliacion = conciliacionContableDaoService
                .selectById(idConciliacionContable, NombreEntidadesTesoreria.CONCILIACION_CONTABLE);
        if (conciliacion == null) {
            throw new IncomeException("No se encontro la ConciliacionContable con id " + idConciliacionContable);
        }

        long totalGrupos = grupoConciliacionContableDaoService
                .selectActivosByConciliacion(idConciliacionContable).size();
        long pendientesExtracto = grupoConciliacionExtractoDaoService.contarPendientes(
                conciliacion.getCuentaBancaria().getCodigo(), conciliacion.getPeriodo().getCodigo());
        long pendientesAsiento = grupoConciliacionAsientoDaoService.contarPendientes(
                conciliacion.getCuentaBancaria().getPlanCuenta().getCodigo(),
                conciliacion.getPeriodo().getEmpresa().getCodigo(),
                conciliacion.getPeriodo().getPrimerDia(),
                conciliacion.getPeriodo().getUltimoDia());

        conciliacion.setTotalGrupos(totalGrupos);
        conciliacion.setTotalPendientesExtracto(pendientesExtracto);
        conciliacion.setTotalPendientesAsiento(pendientesAsiento);
        // Un cambio en los pendientes invalida una verificacion previa - si ya
        // no cuadra "todo pendiente resuelto", vuelve a Pendiente.
        if ((pendientesExtracto > 0 || pendientesAsiento > 0)
                && Long.valueOf(EstadoConciliacionContable.VERIFICADO).equals(conciliacion.getEstadoRevision())) {
            conciliacion.setEstadoRevision((long) EstadoConciliacionContable.PENDIENTE);
        }
        conciliacionContableDaoService.save(conciliacion, conciliacion.getCodigo());
    }

    @Override
    public void verificar(Long idConciliacionContable, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo verificar con idConciliacionContable: " + idConciliacionContable
                + ", usuario: " + usuario);
        ConciliacionContable conciliacion = conciliacionContableDaoService
                .selectById(idConciliacionContable, NombreEntidadesTesoreria.CONCILIACION_CONTABLE);
        if (conciliacion == null) {
            throw new IncomeException("No se encontro la ConciliacionContable con id " + idConciliacionContable);
        }
        Periodo periodo = conciliacion.getPeriodo();
        if (controlExtractoBancarioService.estaCerrado(periodo.getEmpresa().getCodigo(), periodo.getCodigo())) {
            throw new IncomeException("El periodo '" + periodo.getNombre()
                    + "' ya esta cerrado para conciliacion bancaria. No se puede verificar una cuenta.");
        }
        if ((conciliacion.getTotalPendientesExtracto() != null && conciliacion.getTotalPendientesExtracto() > 0)
                || (conciliacion.getTotalPendientesAsiento() != null
                        && conciliacion.getTotalPendientesAsiento() > 0)) {
            throw new IncomeException("No se puede verificar: aun quedan movimientos sin conciliar en esta cuenta/periodo");
        }
        conciliacion.setEstadoRevision((long) EstadoConciliacionContable.VERIFICADO);
        conciliacion.setUsuarioVerifica(usuario);
        conciliacion.setFechaVerificacion(LocalDateTime.now());
        conciliacionContableDaoService.save(conciliacion, conciliacion.getCodigo());
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        ConciliacionContable conciliacionContable = new ConciliacionContable();
        for (Long registro : id) {
            conciliacionContableDaoService.remove(conciliacionContable, registro);
        }
    }

    @Override
    public void save(List<ConciliacionContable> lista) throws Throwable {
        for (ConciliacionContable registro : lista) {
            conciliacionContableDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public ConciliacionContable saveSingle(ConciliacionContable conciliacionContable) throws Throwable {
        return conciliacionContableDaoService.save(conciliacionContable, conciliacionContable.getCodigo());
    }

    @Override
    public List<ConciliacionContable> selectAll() throws Throwable {
        return conciliacionContableDaoService.selectAll(NombreEntidadesTesoreria.CONCILIACION_CONTABLE);
    }

    @Override
    public ConciliacionContable selectById(Long id) throws Throwable {
        return conciliacionContableDaoService.selectById(id, NombreEntidadesTesoreria.CONCILIACION_CONTABLE);
    }

    @Override
    public List<ConciliacionContable> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        return conciliacionContableDaoService.selectByCriteria(datos, NombreEntidadesTesoreria.CONCILIACION_CONTABLE);
    }
}
