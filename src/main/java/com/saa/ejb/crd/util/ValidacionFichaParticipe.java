package com.saa.ejb.crd.util;

import com.saa.basico.util.IncomeException;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Participe;
import com.saa.rubros.ModalidadLaboral;
import com.saa.rubros.SexoParticipe;

/**
 * Validación de los campos de la ficha del partícipe que tienen un CHECK en la base (sql/377): sexo solo M/F/null y
 * modalidad laboral solo 1/2/null. Se llama desde TODOS los guardados de Entidad y Participe, para devolver un error claro
 * en vez de un ORA-02290. Normaliza: el sexo se guarda en mayúscula y un texto vacío se guarda como null.
 */
public final class ValidacionFichaParticipe {

    public static final String ERR_SEXO_INVALIDO = "SEXO_INVALIDO";
    public static final String ERR_MODALIDAD_INVALIDA = "MODALIDAD_LABORAL_INVALIDA";

    private ValidacionFichaParticipe() {
    }

    public static void validar(Entidad entidad) {
        if (entidad == null) {
            return;
        }
        String sexo = entidad.getSexo() == null ? null : entidad.getSexo().trim().toUpperCase();
        if (sexo != null && sexo.isEmpty()) {
            sexo = null;
        }
        if (sexo != null && !SexoParticipe.MASCULINO.equals(sexo) && !SexoParticipe.FEMENINO.equals(sexo)) {
            throw new IncomeException(ERR_SEXO_INVALIDO + ": el sexo debe ser M o F (recibido: " + entidad.getSexo() + ")");
        }
        entidad.setSexo(sexo);
    }

    public static void validar(Participe participe) {
        if (participe == null) {
            return;
        }
        Long m = participe.getModalidadLaboral();
        if (m != null && m != ModalidadLaboral.NOMBRAMIENTO && m != ModalidadLaboral.CONTRATO) {
            throw new IncomeException(ERR_MODALIDAD_INVALIDA
                + ": la modalidad laboral debe ser 1 (nombramiento) o 2 (contrato) (recibido: " + m + ")");
        }
    }
}
