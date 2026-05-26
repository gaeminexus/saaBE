package com.saa.ejb.crd.service;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Aporte;

import jakarta.ejb.Local;

@Local
public interface AporteService extends EntityService<Aporte> {

    /** G42 Grupo 1 — Rendimiento: SUM por entidad donde tipoAporte.estado=1 y codigoSBS='RE', fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaRendimientoPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G42 Grupo 2 — Patronal: SUM por entidad donde tipoAporte.estado=1 y codigo IN (3,13,14), fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaPatronalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G42 Grupo 3 — Personal: SUM por entidad donde tipoAporte.estado=1, excluyendo grupos 1 y 2, fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaPersonalPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 — Imposiciones acumuladas: COUNT de aportes con tipoAporte.codigo IN (9, 11), fechaTransaccion <= fechaCorte */
    List<Object[]> selectCountImposicionesJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;

    /** G44 — Saldo de cuenta: SUM del campo valor de aportes con tipoAporte.codigo = 23, fechaTransaccion <= fechaCorte */
    List<Object[]> selectSumaSaldoCuentaJubilacionPorEntidad(LocalDateTime fechaCorte) throws Throwable;
}
