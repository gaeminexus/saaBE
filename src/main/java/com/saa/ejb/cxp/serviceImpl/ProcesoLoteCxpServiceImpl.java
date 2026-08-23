package com.saa.ejb.cxp.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.DescargaXmlDocumentoService;
import com.saa.ejb.cxp.service.MarcadoErrorDocumentoService;
import com.saa.ejb.cxp.service.ProcesoCargaDocumentosService;
import com.saa.ejb.cxp.service.ProcesoLoteCxpService;
import com.saa.model.cxp.CargaArchivoTxt;
import com.saa.model.cxp.DocumentoCxp;
import com.saa.rubros.ResultadoDescargaSri;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación de los lotes de CXP por carga TXT — fase 1: descarga del SRI.
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Stateless
public class ProcesoLoteCxpServiceImpl implements ProcesoLoteCxpService {

    /** Intentos por documento antes de darlo por perdido ante un error de red. */
    private static final int MAX_INTENTOS = 3;

    /** Espera del primer reintento; el segundo espera el doble, y así (§8 regla 5). */
    private static final long ESPERA_BASE_MS = 3000L;

    /** Respiro entre documentos, para no golpear al SRI en ráfaga. */
    private static final long PAUSA_ENTRE_DOCUMENTOS_MS = 400L;

    /**
     * Lista de trabajo del lote de descarga: el documento de esta carga que
     * todavía no tiene XML.
     *
     * <p>
     * Ya no alimenta ningún contador de progreso. El avance lo lleva el
     * orquestador en {@link RegistroLotesCxp} (§11 decisión 16); esta consulta
     * solo decide <b>qué</b> se procesa, no <b>cuánto</b> se lleva.
     * </p>
     *
     * <p>
     * <b>Por qué sale de aquí lo que quedó FUERA_VENTANA</b> (§11 decisión 10).
     * La ventana del SRI es <b>monótona</b>: un documento fuera de rango hoy
     * está más fuera mañana, así que reintentarlo no puede tener éxito nunca.
     * Dejarlo en la lista solo conseguía que la barra terminara corta con el
     * lote ya parado. No se pierde nada al excluirlo, porque su camino es la
     * subida manual del XML — la decisión (c) del usuario en §3.
     * </p>
     *
     * <p>
     * {@code NO_ENCONTRADO}, {@code NO_AUTORIZADO} y {@code ERROR_CONEXION}
     * <b>siguen</b> en la lista: esos sí son reintentables. Que con el SRI caído
     * la barra quede corta es honesto — queda trabajo real por hacer.
     * </p>
     *
     * <p>
     * Quien use esta constante tiene que enlazar los dos parámetros:
     * {@code idCarga} y {@code fueraVentana}.
     * </p>
     */
    private static final String PENDIENTES_XML =
            "  from DocumentoCxp doc "
            + " where exists (select 1 from DetalleCargaTxt d "
            + "                where d.documento = doc and d.cargaTxt.id = :idCarga) "
            + "   and doc.pathXml is null "
            + "   and (doc.estadoDocumento is null or doc.estadoDocumento <> 3) "
            + "   and (doc.resultadoSri is null or doc.resultadoSri <> :fueraVentana) ";

    /**
     * Lista de trabajo del lote de registro: <b>estado 2 con {@code pathXml}, y
     * nada más</b> (§11 decisión 12).
     *
     * <p>
     * <b>Los revertidos (estado 6) quedan fuera a propósito</b>, aunque conserven
     * el XML en disco. El motivo no es técnico sino de negocio: revertir es un
     * acto deliberado, y si el lote los volviera a registrar, la próxima corrida
     * desharía en silencio una decisión que alguien tomó a mano. Su camino sigue
     * siendo el botón de la fila. Tampoco se limpia el {@code pathXml} al
     * revertir: obligaría a volver a bajar el XML del SRI, y un documento
     * revertido con más de un mes encima ya no se puede bajar (§2).
     * </p>
     */
    private static final String TRABAJO_REGISTRO =
            "  from DocumentoCxp doc "
            + " where exists (select 1 from DetalleCargaTxt d "
            + "                where d.documento = doc and d.cargaTxt.id = :idCarga) "
            + "   and doc.estadoDocumento = 2 "
            + "   and doc.pathXml is not null ";

    /** Grupo 1 del orden obligatorio: crean la cuenta por pagar. */
    private static final String TIPO_FACTURA      = "Factura";
    private static final String TIPO_LIQUIDACION  = "Liquidación de compra";

    /** Grupo 2: modifican una factura de compra que ya tiene que existir. */
    private static final String TIPO_NOTA_CREDITO = "Nota de Crédito";
    private static final String TIPO_NOTA_DEBITO  = "Nota de Débito";

    /** Grupo 3: necesitan la factura de venta del sustento. */
    private static final String TIPO_RETENCION    = "Comprobante de Retención";
    private static final String TIPO_RETENCION_V2 = "Comprobante de Retención electrónica versión 2.0";

    @PersistenceContext
    private EntityManager em;

    // Bean DISTINTO, con REQUIRES_NEW, inyectado con @EJB: es la única forma de
    // que cada documento tenga su propia transacción. Una llamada interna no
    // pasaría por el proxy del contenedor y el lote entero quedaría en una sola
    // transacción — el primer documento que fallara se llevaría por delante a
    // los cuarenta y nueve anteriores. Ver §8 regla 1.
    @EJB private DescargaXmlDocumentoService  descargaXmlDocumentoService;
    @EJB private MarcadoErrorDocumentoService marcadoErrorDocumentoService;
    @EJB private RegistroLotesCxp             registroLotesCxp;

    // El lote de registro NO estrena bean por documento: reutiliza el mismo
    // registrarDocumentoBD que usa el botón de la fila, y así los dos caminos no
    // pueden divergir. Le basta con ser otro bean invocado por proxy — es
    // @Stateless sin atributo explícito, o sea REQUIRED, y como el orquestador
    // corre en NOT_SUPPORTED, cada documento abre su propia transacción.
    @EJB private ProcesoCargaDocumentosService procesoCargaDocumentosService;

    // =========================================================
    // Arranque del lote de descarga (§6.1)
    // =========================================================

    @Override
    public Map<String, Object> reservarDescargaLote(Long idCargaTxt) throws Throwable {

        System.out.println("=== reservarDescargaLote idCargaTxt=" + idCargaTxt);

        CargaArchivoTxt carga = em.find(CargaArchivoTxt.class, idCargaTxt);
        if (carga == null)
            throw new IncomeException("CargaArchivoTxt no encontrada: " + idCargaTxt);

        if (!registroLotesCxp.reservar(idCargaTxt, RegistroLotesCxp.LOTE_DESCARGA)) {
            Map<String, Object> conflicto = new HashMap<>();
            conflicto.put("conflicto", Boolean.TRUE);
            return conflicto;
        }

        Object[] conteo = (Object[]) em.createQuery(
                "select count(doc), "
                + "       sum(case when doc.pathXml is not null or doc.estadoDocumento = 3 "
                + "                then 1 else 0 end) "
                + "  from DocumentoCxp doc "
                + " where exists (select 1 from DetalleCargaTxt d "
                + "                where d.documento = doc and d.cargaTxt.id = :idCarga)")
                .setParameter("idCarga", idCargaTxt)
                .getSingleResult();

        long total     = numero(conteo[0]);
        long yaConXml  = numero(conteo[1]);
        long aProcesar = total - yaConXml;

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("idCargaTxt", idCargaTxt);
        resultado.put("total", total);
        resultado.put("aProcesar", aProcesar);
        resultado.put("yaConXml", yaConXml);
        resultado.put("mensaje", "Descarga iniciada.");

        System.out.println("=== reservarDescargaLote total=" + total
                + " aProcesar=" + aProcesar + " yaConXml=" + yaConXml);
        return resultado;
    }

    @Override
    public void liberarLote(Long idCargaTxt) {
        registroLotesCxp.liberar(idCargaTxt);
    }

    // =========================================================
    // El lote (§6.1) — asíncrono, sin transacción propia
    // =========================================================

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void descargarXmlLote(Long idCargaTxt, Long idEmpresa, Long idUsuario) {

        System.out.println("========================================");
        System.out.println("LOTE DESCARGA SRI - carga=" + idCargaTxt
                + " empresa=" + idEmpresa + " usuario=" + idUsuario);
        System.out.println("========================================");

        int descargados = 0, sinResultado = 0, conError = 0, omitidos = 0;

        try {
            List<Long> ids = documentosPendientesDeXml(idCargaTxt);
            System.out.println("LOTE DESCARGA SRI - documentos a procesar: " + ids.size());

            // El avance lo lleva el orquestador, que es el único que sabe cuántos
            // va a tocar. Ver §11 decisión 16.
            registroLotesCxp.iniciarAvance(idCargaTxt, ids.size());

            for (Long idDocumento : ids) {
                String resultado = procesarUnDocumento(idDocumento, idEmpresa, idUsuario);
                registroLotesCxp.sumarProcesado(idCargaTxt);

                if (ResultadoDescargaSri.DESCARGADO.equals(resultado))      descargados++;
                else if ("OMITIDO".equals(resultado))                        omitidos++;
                else if ("ERROR".equals(resultado))                          conError++;
                else                                                         sinResultado++;

                // Respiro entre documentos. Thread.sleep en un EJB no es ortodoxo,
                // pero esto corre en el hilo del ejecutor asíncrono, no en uno de
                // petición, y espaciar las llamadas es justamente lo que pide la
                // regla 5: en serie y sin ráfagas.
                esperar(PAUSA_ENTRE_DOCUMENTOS_MS);
            }

        } catch (Throwable t) {
            // @Asynchronous void: el contenedor se traga lo que se propague, así
            // que aquí se termina el registro del fallo o no queda en ninguna parte.
            System.err.println("⚠ LOTE DESCARGA SRI abortado en la carga " + idCargaTxt
                    + ": " + t.getMessage());
            t.printStackTrace();

        } finally {
            registroLotesCxp.liberar(idCargaTxt);
            System.out.println("========================================");
            System.out.println("LOTE DESCARGA SRI TERMINADO - carga=" + idCargaTxt
                    + " | descargados=" + descargados
                    + " sinResultadoDelSri=" + sinResultado
                    + " conError=" + conError
                    + " omitidos=" + omitidos);
            System.out.println("========================================");
        }
    }

    /**
     * Procesa un documento con reintento y espera creciente.
     *
     * <p>
     * <b>Dónde se atrapa, y por qué importa.</b> El {@code catch} está aquí,
     * fuera del bean {@code REQUIRES_NEW}: cuando ese bean lanza, el contenedor
     * ya hizo el rollback y soltó el candado de la fila <b>antes</b> de que el
     * control vuelva a este método, así que {@code marcarError} puede escribir
     * sin tropezarse con la transacción que acaba de fallar. Confiar en el
     * {@code catch} interno de {@code ProcesoCargaDocumentosServiceImpl} no
     * serviría: ese corre todavía dentro de la transacción condenada. Ver §8
     * regla 9 y §11 decisión 3.
     * </p>
     *
     * @param idDocumento : Id del DocumentoCxp
     * @param idEmpresa   : Id de la empresa contable
     * @param idUsuario   : Id del usuario del lote
     * @return            : El resultado del SRI, o OMITIDO / ERROR
     */
    private String procesarUnDocumento(Long idDocumento, Long idEmpresa, Long idUsuario) {

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                Map<String, Object> r = descargaXmlDocumentoService
                        .descargarXmlDocumento(idDocumento, idEmpresa, idUsuario);

                if (Boolean.TRUE.equals(r.get(DescargaXmlDocumentoService.CLAVE_OMITIDO)))
                    return "OMITIDO";

                // Un error de red ya quedó grabado como ERROR_CONEXION por el bean,
                // que retornó normalmente para que ese dato se confirme. Reintentar
                // es volver a llamarlo; si el último intento también falla, en el
                // documento queda ERROR_CONEXION, que es lo correcto.
                boolean reintentable = Boolean.TRUE.equals(
                        r.get(DescargaXmlDocumentoService.CLAVE_REINTENTABLE));
                if (!reintentable || intento == MAX_INTENTOS)
                    return String.valueOf(r.get(DescargaXmlDocumentoService.CLAVE_RESULTADO));

                long espera = ESPERA_BASE_MS * intento;
                System.out.println("   ↻ documento " + idDocumento + ": reintento "
                        + (intento + 1) + "/" + MAX_INTENTOS + " en " + espera + " ms");
                esperar(espera);

            } catch (Throwable t) {
                // Fallo imprevisto: la transacción del documento ya se revirtió.
                System.err.println("⚠ Documento " + idDocumento + " falló en la descarga: "
                        + t.getMessage());
                marcadoErrorDocumentoService.marcarError(idDocumento,
                        "Error al descargar el XML del SRI: " + t.getMessage());
                return "ERROR";
            }
        }
        return "ERROR";
    }

    // =========================================================
    // Arranque del lote de registro (§6.2)
    // =========================================================

    @Override
    public Map<String, Object> reservarRegistroLote(Long idCargaTxt) throws Throwable {

        System.out.println("=== reservarRegistroLote idCargaTxt=" + idCargaTxt);

        CargaArchivoTxt carga = em.find(CargaArchivoTxt.class, idCargaTxt);
        if (carga == null)
            throw new IncomeException("CargaArchivoTxt no encontrada: " + idCargaTxt);

        // Un solo lote por carga, sea del tipo que sea: descargar y registrar a la
        // vez sobre los mismos documentos es pedir una carrera.
        if (!registroLotesCxp.reservar(idCargaTxt, RegistroLotesCxp.LOTE_REGISTRO)) {
            Map<String, Object> conflicto = new HashMap<>();
            conflicto.put("conflicto", Boolean.TRUE);
            return conflicto;
        }

        Object[] conteo = (Object[]) em.createQuery(
                "select sum(case when doc.estadoDocumento = 2 and doc.pathXml is not null "
                + "                then 1 else 0 end), "
                // sinXml: sin archivo, así que este lote no los puede tocar
                + "       sum(case when doc.pathXml is null then 1 else 0 end), "
                + "       sum(case when doc.estadoDocumento = 3 then 1 else 0 end) "
                + "  from DocumentoCxp doc "
                + " where exists (select 1 from DetalleCargaTxt d "
                + "                where d.documento = doc and d.cargaTxt.id = :idCarga)")
                .setParameter("idCarga", idCargaTxt)
                .getSingleResult();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("idCargaTxt", idCargaTxt);
        resultado.put("aProcesar", numero(conteo[0]));
        resultado.put("sinXml", numero(conteo[1]));
        resultado.put("yaRegistrados", numero(conteo[2]));
        resultado.put("mensaje", "Registro iniciado.");

        System.out.println("=== reservarRegistroLote aProcesar=" + numero(conteo[0])
                + " sinXml=" + numero(conteo[1]) + " yaRegistrados=" + numero(conteo[2]));
        return resultado;
    }

    // =========================================================
    // El lote de registro (§6.2) — asíncrono, sin transacción propia
    // =========================================================

    @Override
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void registrarLote(Long idCargaTxt, Long idEmpresa, Long idUsuario) {

        System.out.println("========================================");
        System.out.println("LOTE REGISTRO CXP - carga=" + idCargaTxt
                + " empresa=" + idEmpresa + " usuario=" + idUsuario);
        System.out.println("========================================");

        int registrados = 0, bloqueados = 0, pendientes = 0, omitidos = 0, conError = 0;

        try {
            // Lo que dejó la corrida anterior ya no describe el estado actual: el
            // usuario clasificó productos o configuró cuentas entre una y otra.
            registroLotesCxp.limpiarBloqueantes(idCargaTxt);

            List<Long> ids = documentosParaRegistrar(idCargaTxt);
            System.out.println("LOTE REGISTRO CXP - documentos a procesar: " + ids.size());

            // El avance lo lleva el orquestador. Ver §11 decisión 16.
            registroLotesCxp.iniciarAvance(idCargaTxt, ids.size());

            for (Long idDocumento : ids) {
                String desenlace = registrarUnDocumento(idCargaTxt, idDocumento,
                        idEmpresa, idUsuario);
                // Todo desenlace suma, omitidos incluidos: el lote los atendió y
                // decidió no tocarlos, que también es haberlos resuelto.
                registroLotesCxp.sumarProcesado(idCargaTxt);

                if ("REGISTRADO".equals(desenlace))      registrados++;
                else if ("BLOQUEADO".equals(desenlace))  bloqueados++;
                else if ("PENDIENTE".equals(desenlace))  pendientes++;
                else if ("OMITIDO".equals(desenlace))    omitidos++;
                else                                     conError++;

                // Sin pausa entre documentos: aquí no se llama a nadie de fuera,
                // solo se escribe en la base. El respiro del lote de descarga
                // existía para no golpear al SRI en ráfaga.
            }

        } catch (Throwable t) {
            // @Asynchronous void: el contenedor se traga lo que se propague.
            System.err.println("⚠ LOTE REGISTRO CXP abortado en la carga " + idCargaTxt
                    + ": " + t.getMessage());
            t.printStackTrace();

        } finally {
            registroLotesCxp.liberar(idCargaTxt);
            System.out.println("========================================");
            System.out.println("LOTE REGISTRO CXP TERMINADO - carga=" + idCargaTxt
                    + " | registrados=" + registrados
                    + " bloqueados=" + bloqueados
                    + " pendientesDeAtencion=" + pendientes
                    + " omitidos=" + omitidos
                    + " conError=" + conError);
            System.out.println("========================================");
        }
    }

    /**
     * Registra un documento en su propia transacción y traduce el desenlace.
     *
     * <p>
     * <b>El catch va aquí, fuera de la transacción del documento</b>, y eso es lo
     * que hace que el marcado funcione: cuando {@code registrarDocumentoBD}
     * lanza, el contenedor ya deshizo su transacción y soltó el candado de la
     * fila antes de devolvernos el control, así que {@code marcarError} escribe
     * sin chocar con el ORA-00054. Es el escenario para el que se diseñó ese
     * bean. El {@code catch} interno de {@code ProcesoCargaDocumentosServiceImpl}
     * no sirve para esto: corre todavía dentro de la transacción condenada.
     * Ver §11 decisión 3.
     * </p>
     *
     * <p>
     * Un documento que falla <b>no detiene el lote</b>: queda en estado 4 con el
     * motivo y se sigue con el siguiente.
     * </p>
     *
     * @param idCargaTxt  : Id de la carga, para indexar los bloqueantes
     * @param idDocumento : Id del DocumentoCxp
     * @param idEmpresa   : Id de la empresa contable
     * @param idUsuario   : Id del usuario del lote
     * @return            : REGISTRADO, BLOQUEADO, PENDIENTE, OMITIDO o ERROR
     */
    private String registrarUnDocumento(Long idCargaTxt, Long idDocumento,
            Long idEmpresa, Long idUsuario) {
        try {
            // Documento que ya tiene su fila destino viva: se omite sin tocarlo,
            // y conserva su estado 2, su observación y su sitio en
            // requierenAtencion. Es el caso de la factura de reembolso sin
            // sustentos: la FCTC ya está creada y lo que necesita es
            // contabilizarReembolso, no otro registro. Sin esta guarda,
            // registrarDocumentoBD la rechazaría con IncomeException y el catch
            // de abajo la mandaría a estado 4 en la segunda corrida del lote,
            // borrando su diagnóstico. Ver §11 decisión 17.
            //
            // No confundir con el bloqueado por productos sin clasificar: ese
            // también está en estado 2 con observación, pero NO tiene fila
            // destino —el bloqueante corta antes de grabar— y sí debe
            // reintentarse, porque el usuario pudo clasificar entre corridas.
            if (procesoCargaDocumentosService.tieneRegistroVigente(idDocumento)) {
                System.out.println("   ↷ documento " + idDocumento + " omitido: su registro "
                        + "en la tabla destino sigue vigente; se conserva tal como está.");
                return "OMITIDO";
            }

            Map<String, Object> r = procesoCargaDocumentosService
                    .registrarDocumentoBD(idDocumento, idEmpresa, idUsuario);

            // Bloqueado por validaciones previas (productos sin clasificar, grupo
            // sin cuenta, proveedor sin cuenta, tipo de asiento sin configurar).
            // Retorno NORMAL, no excepción: la transacción se confirmó y el
            // documento quedó en estado 2 con observación.
            if (Boolean.TRUE.equals(r.get("pendienteClasificacion"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> bloqueantes =
                        (List<Map<String, Object>>) r.get("bloqueantes");
                registroLotesCxp.guardarBloqueantes(idCargaTxt, idDocumento, bloqueantes);
                System.out.println("   ⚠ documento " + idDocumento + " bloqueado: "
                        + (bloqueantes != null ? bloqueantes.size() : 0) + " condición(es)");
                return "BLOQUEADO";
            }

            // Factura de reembolso sin sustentos o descuadrada: la FCTC quedó
            // creada pero el documento vuelve a estado 2 con observación,
            // esperando el ingreso manual de los sustentos. No hay bloqueantes
            // estructurados que cachear; el motivo va en la observación.
            if (Boolean.TRUE.equals(r.get("contabilizacionPendiente"))) {
                System.out.println("   ⚠ documento " + idDocumento + " pendiente: "
                        + r.get("motivoContabilizacionPendiente"));
                return "PENDIENTE";
            }

            System.out.println("   ✓ documento " + idDocumento + " registrado en "
                    + r.get("tipoTablaDestino") + " id=" + r.get("idDocumentoBD")
                    + (r.get("asiento") != null ? " | asiento " + r.get("asiento") : ""));
            return "REGISTRADO";

        } catch (Throwable t) {
            System.err.println("⚠ Documento " + idDocumento + " falló en el registro: "
                    + t.getMessage());
            marcadoErrorDocumentoService.marcarError(idDocumento,
                    "Error al registrar en BD: " + t.getMessage());
            return "ERROR";
        }
    }

    /**
     * Lista de trabajo del lote de registro, <b>en el orden obligatorio</b>.
     *
     * <p>
     * Factura y Liquidación primero, después Nota de Crédito y Nota de Débito, y
     * al final las Retenciones. Es un requisito de negocio: una NC o una ND
     * necesitan que la factura de compra que afectan ya exista, y una retención
     * necesita la factura de venta del sustento. Si el TXT trae una factura y su
     * nota de crédito, la factura va primero o la nota falla. Con la carga de uno
     * en uno esto lo ordenaba el usuario sin darse cuenta.
     * </p>
     *
     * <p>
     * La consulta ya viene ordenada por {@code fechaEmision} y luego por
     * {@code id}, y el reparto en grupos conserva ese orden, así que dos corridas
     * sobre los mismos datos procesan en la misma secuencia. Un tipo de
     * comprobante desconocido va al final: {@code registrarDocumentoBD} lo va a
     * rechazar, y no tiene por qué retrasar al resto.
     * </p>
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Ids de DocumentoCxp en orden de proceso
     */
    private List<Long> documentosParaRegistrar(Long idCargaTxt) {

        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createQuery(
                "select doc.id, doc.tipoComprobante " + TRABAJO_REGISTRO
                + " order by doc.fechaEmision, doc.id")
                .setParameter("idCarga", idCargaTxt)
                .getResultList();

        // Cuatro cubetas, se concatenan al final. Repartir en Java y no con un
        // CASE en el ORDER BY mantiene la clasificación de tipos en un solo sitio.
        List<List<Long>> grupos = new ArrayList<>();
        for (int i = 0; i < 4; i++) grupos.add(new ArrayList<Long>());

        for (Object[] fila : filas)
            grupos.get(grupoDeOrden((String) fila[1])).add((Long) fila[0]);

        List<Long> ordenados = new ArrayList<>();
        for (List<Long> grupo : grupos) ordenados.addAll(grupo);

        System.out.println("LOTE REGISTRO CXP - orden: facturas/liquidaciones="
                + grupos.get(0).size() + " notas=" + grupos.get(1).size()
                + " retenciones=" + grupos.get(2).size()
                + " tipoDesconocido=" + grupos.get(3).size());
        return ordenados;
    }

    /**
     * A qué cubeta del orden obligatorio pertenece un tipo de comprobante.
     *
     * @param tipoComprobante : Tipo tal como vino del TXT del SRI
     * @return                : 0 facturas y liquidaciones · 1 notas · 2 retenciones
     *                          · 3 desconocido
     */
    private int grupoDeOrden(String tipoComprobante) {
        if (TIPO_FACTURA.equalsIgnoreCase(tipoComprobante)
                || TIPO_LIQUIDACION.equalsIgnoreCase(tipoComprobante)) return 0;
        if (TIPO_NOTA_CREDITO.equalsIgnoreCase(tipoComprobante)
                || TIPO_NOTA_DEBITO.equalsIgnoreCase(tipoComprobante)) return 1;
        if (TIPO_RETENCION.equalsIgnoreCase(tipoComprobante)
                || TIPO_RETENCION_V2.equalsIgnoreCase(tipoComprobante)) return 2;
        return 3;
    }

    // =========================================================
    // Progreso (§6.3)
    // =========================================================

    @Override
    public Map<String, Object> progresoLote(Long idCargaTxt) throws Throwable {

        System.out.println("=== progresoLote idCargaTxt=" + idCargaTxt);

        // Sin comprobar que la carga exista: §6.3 pide 200 siempre, y una carga
        // inexistente sale sola en ceros con documentos[] vacío. El frontend solo
        // consulta cargas de su propia lista, y un 404 cada 2 segundos dentro del
        // polling no le diría nada útil.

        String tipoLote = registroLotesCxp.tipoLote(idCargaTxt);

        // Un solo COUNT sobre DCXP para todos los contadores: siete expresiones
        // agregadas sobre el mismo conjunto de filas, así que salen coherentes
        // entre sí y de un viaje. Sin tabla de lotes — §5.1.
        //
        // Los cinco primeros contadores PARTICIONAN la carga: cada documento cae
        // en uno y solo uno, y la suma da `totalCarga`. Por eso el reparto se hace por
        // estadoDocumento y no por combinaciones de estado y pathXml — así no
        // puede quedar ni un solapamiento ni un hueco. `fueraVentana` es la
        // excepción declarada: es un subconjunto de sinXml, no una categoría
        // aparte, y por eso va fuera de la partición.
        Object[] c = (Object[]) em.createQuery(
                "select count(doc), "
                // sinXml: todo lo que no llegó a estado 2, 3 ni 4 — estado 1, 6 o nulo
                + "  sum(case when doc.estadoDocumento is null "
                + "            or (doc.estadoDocumento <> 2 and doc.estadoDocumento <> 3 "
                + "                and doc.estadoDocumento <> 4) then 1 else 0 end), "
                // conXml: estado 2 limpio
                + "  sum(case when doc.estadoDocumento = 2 and doc.observacion is null "
                + "            then 1 else 0 end), "
                // registrados
                + "  sum(case when doc.estadoDocumento = 3 then 1 else 0 end), "
                // requierenAtencion: estado 2 con observación — se lo resta a conXml
                + "  sum(case when doc.estadoDocumento = 2 and doc.observacion is not null "
                + "            then 1 else 0 end), "
                // conError
                + "  sum(case when doc.estadoDocumento = 4 then 1 else 0 end), "
                // fueraVentana (subconjunto de sinXml)
                + "  sum(case when doc.resultadoSri = :fueraVentana then 1 else 0 end) "
                + "  from DocumentoCxp doc "
                + " where exists (select 1 from DetalleCargaTxt d "
                + "                where d.documento = doc and d.cargaTxt.id = :idCarga)")
                .setParameter("idCarga", idCargaTxt)
                .setParameter("fueraVentana", ResultadoDescargaSri.FUERA_VENTANA)
                .getSingleResult();

        Map<String, Object> contadores = new LinkedHashMap<>();
        contadores.put("sinXml",            numero(c[1]));
        contadores.put("conXml",            numero(c[2]));
        contadores.put("registrados",       numero(c[3]));
        contadores.put("requierenAtencion", numero(c[4]));
        contadores.put("conError",          numero(c[5]));
        contadores.put("fueraVentana",      numero(c[6]));

        // Los tres números del avance (§11 decisión 16):
        //   totalCarga  documentos de la carga TXT, siempre poblado
        //   total       tamaño de la lista de trabajo del lote en curso, 0 en reposo
        //   procesados  cuántos de esa lista llevan desenlace, 0 en reposo
        // total y procesados los lleva el orquestador; no se derivan de la base.
        // Derivarlos obligaba a una consulta de "lo que falta" que en el lote de
        // registro no puede coincidir con la lista de trabajo, y la barra marcaba
        // 100 % desde el primer segundo al reintentar.
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("idCargaTxt", idCargaTxt);
        resultado.put("enCurso", Boolean.valueOf(tipoLote != null));
        resultado.put("tipoLote", tipoLote);
        resultado.put("procesados", registroLotesCxp.procesadosDelLote(idCargaTxt));
        resultado.put("total", registroLotesCxp.totalDelLote(idCargaTxt));
        resultado.put("totalCarga", numero(c[0]));
        resultado.put("contadores", contadores);
        resultado.put("documentos", documentosDeLaCarga(idCargaTxt));
        return resultado;
    }

    /**
     * Las filas de la grilla, con las claves exactas de §6.3. Se arma a mano en
     * vez de devolver la entidad: el contrato del frontend está cerrado y una
     * entidad JPA arrastraría el resto de las columnas y las relaciones.
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Lista de mapas, uno por documento
     */
    private List<Map<String, Object>> documentosDeLaCarga(Long idCargaTxt) {

        // Los bloqueantes salen del caché del @Singleton, de un solo vistazo para
        // toda la carga. Recalcularlos aquí obligaría a correr las validaciones de
        // registrarBD sobre las 50 filas cada 2 segundos. Ver §11 decisión 8.
        Map<Long, List<Map<String, Object>>> bloqueantesPorDocumento =
                registroLotesCxp.bloqueantesDeLaCarga(idCargaTxt);

        @SuppressWarnings("unchecked")
        List<DocumentoCxp> documentos = em.createQuery(
                "select doc from DocumentoCxp doc "
                + " where exists (select 1 from DetalleCargaTxt d "
                + "                where d.documento = doc and d.cargaTxt.id = :idCarga) "
                + " order by doc.id")
                .setParameter("idCarga", idCargaTxt)
                .getResultList();

        List<Map<String, Object>> filas = new ArrayList<>();
        for (DocumentoCxp doc : documentos) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", doc.getId());
            fila.put("serieComprobante", doc.getSerieComprobante());
            fila.put("razonSocialEmisor", doc.getRazonSocialEmisor());
            fila.put("tipoComprobante", doc.getTipoComprobante());
            fila.put("estadoDocumento", doc.getEstadoDocumento());
            fila.put("esReembolso", doc.getEsReembolso());
            fila.put("resultadoSri", doc.getResultadoSri());
            fila.put("mensajeSri", doc.getMensajeSri());
            fila.put("observacion", doc.getObservacion());
            // Forma [{tipo, detalle, productos?, grupos?}], la misma que devuelve
            // el 422 de registrarBD. Vacío mientras no haya corrido un lote de
            // registro sobre esta carga, o si WildFly se reinició desde entonces.
            List<Map<String, Object>> bloqueantes = bloqueantesPorDocumento.get(doc.getId());
            fila.put("bloqueantes", bloqueantes != null ? bloqueantes : Collections.emptyList());
            filas.add(fila);
        }
        return filas;
    }

    // =========================================================
    // Utilitarios privados
    // =========================================================

    /**
     * Documentos de la carga que todavía no tienen XML y no están registrados.
     * Es la lista de trabajo del lote y la garantía de idempotencia: una segunda
     * corrida solo ve lo que quedó pendiente en la primera (§8 regla 6).
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Ids de DocumentoCxp, en orden
     */
    private List<Long> documentosPendientesDeXml(Long idCargaTxt) {
        @SuppressWarnings("unchecked")
        List<Long> ids = em.createQuery("select doc.id " + PENDIENTES_XML + " order by doc.id")
                .setParameter("idCarga", idCargaTxt)
                .setParameter("fueraVentana", ResultadoDescargaSri.FUERA_VENTANA)
                .getResultList();
        return ids;
    }

    /** Los agregados de JPQL vuelven como Long o BigDecimal según el dialecto. */
    private long numero(Object valor) {
        return valor instanceof Number ? ((Number) valor).longValue() : 0L;
    }

    private void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
