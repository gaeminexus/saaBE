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

	/**
	 * Igual que {@link #validar(String)} para un {@code Usuario} que llega en un documento (factura, nota, retención…):
	 * usa su nombre; si el cliente solo mandó el código, lo resuelve por código. Devuelve el nombre del usuario.
	 */
	String validar(com.saa.model.scp.Usuario usuario) throws UsuarioInvalidoException;

	/**
	 * Valida un usuario dado por su código ({@code idUsuario}, como llega en los procesos de cxp, tsr y rhh) y devuelve su
	 * nombre. Nulo o inexistente: {@link UsuarioInvalidoException}. No hay valor por defecto: el 0 del frontend sin sesión
	 * tampoco existe.
	 */
	String validarPorId(Long idUsuario) throws UsuarioInvalidoException;

	/**
	 * Valida un usuario por su nombre y devuelve su CÓDIGO ({@code idUsuario}). Es lo que se usa donde un proceso conoce
	 * el nombre ya validado pero el servicio siguiente (órdenes de pago de cxp, tsr, rhh) pide el id: así el id nunca
	 * llega nulo ni en 0 por un campo del cliente.
	 */
	Long idPorNombre(String nombre) throws UsuarioInvalidoException;
}
