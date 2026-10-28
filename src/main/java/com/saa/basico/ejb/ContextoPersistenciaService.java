package com.saa.basico.ejb;

import java.util.Collection;

import org.hibernate.FlushMode;

import jakarta.ejb.Local;

/**
 * Control acotado del contexto de persistencia (la {@code Session} de Hibernate) de la
 * transacción JTA en curso, para procesos masivos que corren en UNA sola transacción.
 *
 * <p><b>Por qué existe</b> (cuadre Superintendencia C4, 2026-10-28, ver
 * {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md} §11 "Rendimiento"): con
 * {@code FlushMode.AUTO}, cada consulta JPQL dispara el auto-flush de Hibernate, que recorre
 * (dirty-check) TODAS las entidades administradas antes de ejecutar la consulta. En una
 * transacción que acumula decenas de miles de entidades (cuotas, pagos, aportes, novedades) el
 * costo por consulta crece con el contexto y el proceso se vuelve cuadrático: medido en el
 * reproceso de la carga 352, la misma llamada pasó de 39 ms a 422 ms en 30 minutos con la JVM
 * en un núcleo y Oracle ocioso.</p>
 *
 * <p>Todos los métodos trabajan sobre el contexto de la transacción ACTUAL (el
 * {@code EntityManager} inyectado es el proxy transaccional del contenedor: es la misma
 * {@code Session} que usan los DAO de {@code SaaPU} dentro de esa transacción). Ninguno abre ni
 * cierra transacciones, ninguno cambia datos: volcar ({@code flush}) solo adelanta SQL que el
 * commit iba a ejecutar de todos modos, y desprender ({@code detach}) solo saca del contexto
 * entidades cuyo estado ya está en la base.</p>
 */
@Local
public interface ContextoPersistenciaService {

	/** Entidades administradas hoy en el contexto de persistencia de la transacción. */
	int tamanio();

	/**
	 * Las {@code top} clases con más instancias administradas, "Clase=N, Clase=N, ...". Recorre
	 * el contexto una vez: usarlo solo en los bordes de fase, no por consulta.
	 */
	String histograma(int top);

	/**
	 * {@code flush()} y luego {@code detach} de TODA instancia administrada cuya clase esté en
	 * {@code clases} (comparación exacta de clase, sin subclases). Primero se vuelca, así que
	 * nada pendiente se pierde: lo desprendido tiene en la base exactamente el estado que tenía
	 * en memoria. El modelo de {@code com.saa.model.crd} no declara cascadas ni colecciones, así
	 * que {@code detach} afecta solo a esa instancia.
	 *
	 * <p><b>Contrato con el llamador:</b> después de esto, cualquier referencia que el llamador
	 * conserve a una instancia desprendida ya no está administrada: modificarla sin volver a
	 * guardarla ({@code save}/{@code merge}) NO se persiste. Llamarlo solo en un borde donde el
	 * llamador no conserva referencias de esas clases que vaya a modificar, o donde todo lo que
	 * modifique pase por {@code save} (que hace {@code merge}).</p>
	 *
	 * @return cuántas instancias se desprendieron
	 */
	int volcarYDesprender(Collection<Class<?>> clases);

	/**
	 * {@link #volcarYDesprender} solo si el contexto supera {@code umbral} entidades; si no,
	 * no hace nada (ni siquiera {@code flush}).
	 *
	 * @return cuántas instancias se desprendieron, o -1 si no hizo falta
	 */
	int volcarYDesprenderSiExcede(int umbral, Collection<Class<?>> clases);

	/**
	 * Vuelca lo pendiente y pone la sesión en {@link FlushMode#MANUAL}: las consultas dejan de
	 * disparar el auto-flush. <b>Solo para fases de SOLO LECTURA</b>: como antes se volcó todo,
	 * la base tiene exactamente el estado del contexto y, si la fase no escribe, ninguna consulta
	 * puede ver algo distinto de lo que vería con AUTO. Siempre en pareja con
	 * {@link #restaurarAutoFlush} en un {@code finally}: con MANUAL, el commit tampoco vuelca.
	 *
	 * @return el modo anterior, para pasarlo a {@link #restaurarAutoFlush}
	 */
	FlushMode suspenderAutoFlush();

	/**
	 * Restaura el modo de flush anterior y verifica que la fase de solo lectura no haya dejado
	 * cambios pendientes ({@code Session.isDirty()}). Si los dejó, los vuelca y deja una
	 * ADVERTENCIA en el log con {@code fase}: significa que una consulta de esa fase pudo no ver
	 * esa escritura, y hay que revisar el supuesto de solo lectura.
	 *
	 * @return true si la fase dejó cambios pendientes (supuesto de solo lectura violado)
	 */
	boolean restaurarAutoFlush(FlushMode anterior, String fase);
}
