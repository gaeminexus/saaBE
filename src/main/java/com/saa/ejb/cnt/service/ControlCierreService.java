package com.saa.ejb.cnt.service;

import java.util.List;

import com.saa.ejb.cnt.service.dto.ControlCierre;

import jakarta.ejb.Local;

/**
 * Pase a producción (B-1..B-3) — controles de cierre de mes, el "semáforo" que la pantalla de Cierre de
 * Períodos consulta antes de {@code POST /prdo/cerrar}. Pasa al SAA los controles que hasta ahora vivían como
 * scripts del expediente (cuentas puente D-024, ecuación contable, cartera contra contabilidad, bancos contra
 * extracto, asientos descuadrados, cuadratura B17 ↔ G).
 *
 * <p><b>Solo lectura</b>: no escribe nada ni bloquea el cierre (eso queda como opción documentada en
 * docs/logica-negocio/cnt/CONTROL-CIERRE-MES.md).</p>
 */
@Local
public interface ControlCierreService {

    /**
     * @param idEmpresa : Empresa (obligatoria)
     * @param anio      : Año
     * @param mes       : Mes 1..12
     * @return          : Lista de controles en orden de presentación
     */
    List<ControlCierre> controlar(Long idEmpresa, Long anio, Long mes) throws Throwable;
}
