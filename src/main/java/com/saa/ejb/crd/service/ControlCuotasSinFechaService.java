package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.cnt.service.dto.ControlCierre;

import jakarta.ejb.Local;

/**
 * C48: control previo al cierre de cartera — cuotas en CANCELADA_ANTICIPADA (7) sin fecha de pago (DTPRFCPG nula). Las lecturas al corte
 * ({@code NOT (estado IN (4,7) AND NVL(DTPRFCPG, :corteSig) < :corteSig)}) tratan una cuota 7 sin fecha como NO liquidada, para siempre: el libro de devengo y
 * la provisión por cuota, las bandas y la G48 la leen viva. Los motores ya fechan la cuota que cierran (acuerdo, precancelación, sustitución); este control
 * detecta las que quedaron de antes. No depende de ModoCorteCartera ni de la C19b (va también al EAR de producción).
 * Contrato: docs/logica-negocio/crd/C48-CUOTAS-7-CON-FECHA.md.
 */
@Local
public interface ControlCuotasSinFechaService {

    /** Propiedad: si está encendida ({@code true}), cuotas 7 sin fecha bloquea el ejecutar del cierre de cartera. Apagada por defecto. */
    String PROPIEDAD_BLOQUEO = "saa.cartera.bloqueoCuotas7SinFecha";

    /** {@code true} si {@link #PROPIEDAD_BLOQUEO} está encendida. */
    static boolean bloqueoActivo() {
        return Boolean.parseBoolean(System.getProperty(PROPIEDAD_BLOQUEO, "false").trim());
    }

    /** Cantidad de cuotas en estado 7 con DTPRFCPG nula (todos los préstamos: PRST no tiene empresa, como las demás lecturas de cartera). */
    long contar() throws Throwable;

    /** Texto del control con la lista préstamo/cuota (acotada), o {@code null} si no hay ninguna. */
    String textoControl() throws Throwable;

    /** Control del semáforo {@code /prdo/controlCierre}: un control, ERROR si hay cuotas 7 sin fecha, OK si no. */
    List<ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes) throws Throwable;
}
