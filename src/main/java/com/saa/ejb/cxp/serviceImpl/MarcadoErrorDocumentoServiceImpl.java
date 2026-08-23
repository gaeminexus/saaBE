package com.saa.ejb.cxp.serviceImpl;

import java.nio.charset.StandardCharsets;

import com.saa.ejb.cxp.service.MarcadoErrorDocumentoService;
import com.saa.rubros.EstadoDocumentoCxp;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación del marcado de error sobre PGS.DCXP en transacción propia.
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Stateless
public class MarcadoErrorDocumentoServiceImpl implements MarcadoErrorDocumentoService {

    /** Largo de PGS.DCXP.DCXPOBSR — VARCHAR2(2000). */
    private static final int LARGO_OBSERVACION = 2000;

    @PersistenceContext
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void marcarError(Long idDocumentoCxp, String mensaje) {

        System.out.println("=== marcarError idDocumentoCxp=" + idDocumentoCxp
                + " mensaje=" + mensaje);

        if (idDocumentoCxp == null) {
            System.err.println("⚠ marcarError sin idDocumentoCxp — no hay documento que marcar.");
            return;
        }

        try {
            // Se escribe con SQL nativo y no con la entidad a propósito: esta
            // transacción nace con su propio contexto de persistencia y solo
            // debe tocar dos columnas. Cargar el DocumentoCxp completo y dejar
            // que el flush lo reescriba entero arrastraría el resto de la fila
            // tal como está en la base, que es justo lo que no interesa aquí.

            // Toma del candado con NOWAIT antes de escribir.
            //
            // El llamador está dentro de su propio catch: su transacción sigue
            // viva y, si ya alcanzó a hacer flush de un cambio sobre esta misma
            // fila (por ejemplo el paso a estado 2 o 3 previo al fallo), tiene
            // el registro bloqueado y no lo va a soltar hasta que retornemos.
            // Un UPDATE normal se quedaría esperando ese candado hasta el
            // timeout de la transacción — Oracle no lo detecta como interbloqueo
            // porque el otro lado no está esperando a la base, sino a este hilo.
            // Con NOWAIT el intento falla en el acto (ORA-00054) y el error de
            // negocio original llega al usuario sin que la petición se cuelgue.
            em.createNativeQuery(
                    "select DCXPCDGO from PGS.DCXP where DCXPCDGO = ?1 for update nowait")
                    .setParameter(1, idDocumentoCxp)
                    .getSingleResult();

            int filas = em.createNativeQuery(
                    "update PGS.DCXP set DCXPESTD = ?1, DCXPOBSR = ?2 where DCXPCDGO = ?3")
                    .setParameter(1, EstadoDocumentoCxp.ERROR)
                    .setParameter(2, recortarObservacion(mensaje))
                    .setParameter(3, idDocumentoCxp)
                    .executeUpdate();

            if (filas == 0)
                System.err.println("⚠ marcarError: no existe el DocumentoCxp " + idDocumentoCxp
                        + " — el error no se pudo estampar.");
            else
                System.out.println("✓ DocumentoCxp " + idDocumentoCxp + " marcado en estado "
                        + EstadoDocumentoCxp.ERROR + " (ERROR).");

        } catch (Throwable t) {
            // Nunca se propaga: este método corre dentro del catch del proceso
            // de registro y lanzar aquí taparía el error de negocio original.
            System.err.println("⚠ No se pudo marcar en ERROR el DocumentoCxp " + idDocumentoCxp
                    + ": " + t.getMessage()
                    + " | Si es ORA-00054, la transacción que falló tenía la fila bloqueada"
                    + " por un cambio de estado previo; el documento queda con el estado que"
                    + " tenía antes del intento.");
        }
    }

    /**
     * Recorta el mensaje al largo de DCXPOBSR. El recorte se verifica en bytes
     * UTF-8 y no solo en caracteres: la columna se declaró {@code VARCHAR2(2000)}
     * sin semántica CHAR explícita, así que en una base con NLS_LENGTH_SEMANTICS
     * en BYTE cada tilde de un mensaje de error ocupa dos. Pasarse de largo haría
     * fallar el UPDATE, que es exactamente el fallo que este bean existe para evitar.
     *
     * @param mensaje : Texto del error
     * @return        : Texto que cabe en la columna, o null si no había mensaje
     */
    private String recortarObservacion(String mensaje) {
        if (mensaje == null)
            return null;

        String texto = mensaje.length() > LARGO_OBSERVACION
                ? mensaje.substring(0, LARGO_OBSERVACION)
                : mensaje;

        while (!texto.isEmpty()
                && texto.getBytes(StandardCharsets.UTF_8).length > LARGO_OBSERVACION)
            texto = texto.substring(0, texto.length() - 1);

        return texto;
    }
}
