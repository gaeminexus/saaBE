package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.AfectacionValoresParticipeCarga;

import jakarta.ejb.Local;

@Local
public interface AfectacionValoresParticipeCargaService extends EntityService<AfectacionValoresParticipeCarga> {

    /**
     * Cuánto le falta o le sobra al reparto de una novedad CON EXCEDENTE para cuadrar exacto
     * (PLAN-EXCEDENTE-PETRO-A-APORTES.md §5, decisión del usuario 2026-08-30: "ni más ni
     * menos, o si no no hay cómo procesar"). Suma las AVPC ACTIVAS de la novedad —de préstamo
     * y de aporte por igual, XOR entre sí pero no entre ellas para este cálculo— y la resta
     * del {@code montoDiferencia} (excedente) de la novedad.
     *
     * <b>Único lugar donde vive esta regla</b> — la llaman {@code AfectacionValoresParticipeCargaRest
     * .postBatch} (para avisarle al operador al guardar) y {@code CargaArchivoPetroServiceImpl}
     * (como control bloqueante antes de procesar la carga). Si la regla se escribiera dos
     * veces, el día que cambie la tolerancia divergirían y el proceso aceptaría lo que la
     * pantalla rechaza.
     *
     * @return 0.0 si la novedad no tiene excedente (no aplica este control), o si el reparto
     *         ya cuadra dentro de la tolerancia de $0.01; positivo si falta repartir, negativo
     *         si se repartió de más.
     * @throws Throwable {@code IncomeException} si no existe la novedad
     */
    double diferenciaReparto(Long idNovedad) throws Throwable;

}
