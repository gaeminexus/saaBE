package com.saa.basico.util;

/**
 * Marca una entidad cuyos campos de auditoria puede sellar el DAO generico.
 *
 * <p>
 * Es <b>opt-in</b>: {@code EntityDaoImpl.save} solo sella la entidad que declara
 * implementar una de las dos subinterfaces. Una entidad que no las implemente se
 * persiste exactamente igual que antes, sin ningun cambio de conducta. Es
 * deliberado: el CRUD generico lo comparten los nueve modulos del sistema y no
 * hay pruebas automatizadas, asi que la unica forma segura de introducir el
 * sellado es aditiva, modulo por modulo.
 * </p>
 *
 * <p>
 * <b>Por que hay dos subinterfaces y no una sola.</b> {@code fechaRegistro} no
 * tiene un unico tipo en el sistema: unas entidades la declaran
 * {@code LocalDate} y otras {@code LocalDateTime} —en RHH son 23 y 30
 * respectivamente—. El getter admite una sola firma por covarianza, pero el
 * setter no: los tipos de parametro son invariantes en Java. Separarlas permite
 * que una entidad se sume escribiendo unicamente {@code implements ...} en su
 * declaracion, sin tocar ni un getter ni un setter.
 * </p>
 *
 * <p>
 * <b>El usuario no lo sella el DAO.</b> Se declara aqui porque forma parte del
 * contrato de auditoria y porque un interceptor con contexto de sesion podria
 * llenarlo mas adelante, pero el DAO generico no tiene al usuario en el alcance
 * y no se lo inventa: hoy lo trae el JSON de la peticion, o lo pone el servicio
 * de proceso. Escribir ahi un valor fabricado seria peor que dejarlo en nulo,
 * porque la auditoria dejaria de distinguir lo que se sabe de lo que no.
 * </p>
 *
 * @see EntidadAuditableFecha
 * @see EntidadAuditableFechaHora
 */
public interface EntidadAuditable {

    /**
     * Usuario que registro la fila. El DAO generico lo lee, nunca lo escribe.
     *
     * @return : Usuario de auditoria, o nulo si no viene informado
     */
    String getUsuarioRegistro();
}
