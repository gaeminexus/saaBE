package com.saa.basico.ejb;

import com.saa.basico.util.UsuarioInvalidoException;

import jakarta.ejb.Local;

/**
 * Helper ÚNICO para validar el usuario con el que se graba (U-2, DISENO-USUARIO-OBLIGATORIO.md): todo proceso que
 * grabe lo llama como PRIMERA instrucción, antes de tocar la base. Nunca se rellena con «SISTEMA»: un texto que no
 * corresponde a un usuario del sistema no pasa.
 */
@Local
public interface UsuarioValidacionService {

	/**
	 * Valida el usuario y devuelve su nombre recortado.
	 *
	 * <p>«Válido» = no nulo, no vacío y existe en {@code com.saa.model.scp.Usuario} (SCP.PJRQ). La entidad no mapea
	 * ninguna columna de estado, así que «activo» hoy se limita a «existe»; cuando esa columna esté mapeada se
	 * agrega acá y lo heredan todos los procesos.</p>
	 *
	 * @param nombre nombre de usuario tal como lo manda la sesión
	 * @return el nombre recortado
	 * @throws UsuarioInvalidoException si falta, está vacío o no existe (con el motivo)
	 */
	String validar(String nombre) throws UsuarioInvalidoException;
}
