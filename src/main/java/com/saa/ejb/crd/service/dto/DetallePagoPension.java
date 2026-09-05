package com.saa.ejb.crd.service.dto;

/**
 * Un renglón del detalle de {@link ResultadoGeneracionPagosPension} — un jubilado evaluado en
 * la corrida de {@code PagoPensionComplementariaService#generarPagosDelMes}.
 *
 * Ver API-PAGO-PENSION-COMPLEMENTARIA.md §1 y PLAN-PAGO-JUBILADOS.md §3/§4.
 */
public class DetallePagoPension {

    private Long idEntidad;
    private String nombre;
    private Long idPago;
    private Double valorPension;
    private Double valorSeguroSalud;

    /**
     * §4bis del contrato, pedido del usuario 2026-09-04: la pensión y el seguro médico van a
     * cuentas contables distintas (plantilla alterno 35: pensión a 2.3.01.10.03, seguro a
     * 2.3.90.90.06), y la pantalla necesita el desglose, no solo el total.
     *
     * ⛔ {@code valorPensionMensual + valorSeguroMensual == VPPC.valorPagar}. {@code valorPagar}
     * YA INCLUYE el seguro — no se suman aparte, se resta (mismo criterio que ya usa
     * {@code PagoPensionComplementariaServiceImpl}: {@code valorPension = valorTotal - valorSeguro}).
     */
    private double valorPensionMensual;

    /** El seguro médico de UN mes ({@code VPPC.valorSeguro}), no acumulado. */
    private double valorSeguroMensual;

    /**
     * Pensión acumulada de lo que esta llamada PROCESÓ de verdad, mes a mes (pagada al banco o
     * retenida por falta de certificado — nunca lo no-cruzado-ni-procesado).
     *
     * ⛔⛔ Corrección 2026-09-05, decisión del usuario: ya NO es un reparto proporcional con el
     * seguro. La prioridad pasa a ser cruce → seguro → PENSIÓN (la única que puede quedar
     * corta) — ver el cálculo en {@code generarMesesRetroactivos}. Antes de esto (6abf436,
     * 2026-09-04) se repartía proporcional a la mensualidad; ese criterio quedó superado en
     * menos de un día por el pedido del usuario de separar el seguro como pago a un proveedor.
     * Ojo: NO es el devengado; el devengo del asiento sigue siendo el nominal completo del mes
     * ({@code PGPC.valorPension}).
     */
    private double totalPension;

    /**
     * ⚠️⚠️ Corrección 2026-09-05: idéntico a {@link #valorSeguroInterno} ahora que el seguro
     * SIEMPRE se separa (ya no sólo sin certificado) — quedaron duplicados por el mismo motivo
     * que {@code valorPension}/{@code totalPension} (ver la memoria del repo,
     * "pendiente-limpieza-valorPension-duplicado"): dos campos que nacieron con significados
     * distintos y una decisión de negocio los volvió sinónimos. NO se resuelve ahora — congelado
     * hasta después de la corrida de agosto, igual que el otro par.
     */
    private double totalSeguro;

    /**
     * ⚠️ Nombre heredado de 6abf436 (2026-09-04): ya NO es "lo traspasado por no haber
     * certificado", es TODO el seguro médico del mes, siempre — corrección 2026-09-05, decisión
     * del usuario: el seguro se descuenta del aporte 23 con la MISMA prioridad que el préstamo
     * (cruce, luego seguro, luego pensión), sin mirar el certificado. Ya no es un subconjunto de
     * {@link #totalSeguro} — desde este cambio son EL MISMO NÚMERO (ver la nota de arriba).
     * Propuesto renombrar a {@code valorSeguroProveedor} — no aplicado, requiere coordinar con
     * el frontend.
     */
    private double valorSeguroInterno;

    /** Cuánto de la pensión del mes se cruzó contra deuda de préstamo vigente (0 si no aplica). */
    private Double valorCruzadoAPrestamo;

    /** Cuánto salió como orden de pago hacia tesorería (puede ser 0 si el cruce se llevó todo). */
    private Double valorOrdenPago;

    /**
     * {@code false} con {@code valorCruzadoAPrestamo > 0} NO es un error: es el caso en que la
     * deuda se llevó toda la pensión del mes. El pago existe, se contabilizó, y no hubo salida
     * de dinero (PLAN-PAGO-JUBILADOS.md §3, punto ⛔).
     */
    private boolean generoOrdenPago;

    /** Asiento de devengo generado en CRD (D cuenta del jubilado, H por pagar) — null si la
     * contabilidad de CRD está inactiva. */
    private Long idAsientoDevengo;

    /**
     * "GENERADO" | "YA_EXISTIA" | "ERROR" | "SIN_ANCLA" | "AL_DIA" (PLAN-PAGO-RETROACTIVO-
     * JUBILADOS.md): las tres últimas son finales NORMALES de un jubilado con préstamo, nunca
     * "ERROR" — el operador necesita distinguir "terminó bien" de "se rompió".
     */
    private String estado;

    private String mensaje;

    /**
     * Cuántos períodos (PGPC) NUEVOS generó esta llamada — 1 en el circuito normal sin
     * préstamo, 0..N en el retroactivo. Campo nuevo y opcional: el frontend actual sigue
     * funcionando sin leerlo.
     */
    private int mesesAplicados;

    /**
     * Cuál de las condiciones de corte terminó el bucle retroactivo: "MES_CORRIDA_ALCANZADO" |
     * "SALDO_AGOTADO" | "SIN_PRESTAMO_SIN_CERTIFICADO_SIN_SEGURO". {@code null} fuera del
     * circuito retroactivo.
     *
     * ⛔ "SIN_PRESTAMO_SIN_CERTIFICADO" (a secas) YA NO es alcanzable: desde la ampliación del
     * 2026-09-04 tener seguro médico también desbloquea, así que el corte exige los TRES en
     * cero. Ningún consumidor lo leía —verificado con grep en backend, frontend y docs— por eso
     * se renombró en vez de agregar un segundo literal.
     *
     * ⛔ "PRESTAMO_AL_DIA" YA NO es alcanzable (corrección 2026-09-05, post-D4): que el préstamo
     * quede al día ya no corta el bucle — el jubilado sigue cobrando en efectivo los meses que
     * le falten. Antes de D4 sí cortaba, porque sin préstamo no había retroactivo; después de
     * D4 todos pasan por el mismo bucle y cortar ahí dejaba sin pagar el resto de los meses.
     */
    private String motivoCorte;

    /**
     * "COMPLETA" | "SOLO_CRUCE" | "BLOQUEADO" | "AL_DIA" — API-PAGO-PENSION-COMPLEMENTARIA.md
     * §6. {@code COMPLETA}: hubo cruce (si tenía préstamo) y no quedó remanente retenido —
     * incluye tanto "salió dinero al banco" como "el cruce absorbió el 100%, no había nada
     * que sacar". {@code SOLO_CRUCE}: quedó un remanente de PENSIÓN que no se pudo entregar
     * (sin cuenta o sin certificado) — desde la ampliación del 2026-09-04 se lee como PARCIAL y
     * ya no implica que haya habido cruce: un jubilado sin préstamo, sin certificado y con
     * seguro médico cae acá con {@code valorCruzadoAPrestamo = 0}, porque su seguro sí se
     * traspasó. El literal NO cambió, a propósito, para no romper al frontend (§6).
     * {@code BLOQUEADO}: no participó en absoluto — sin préstamo, sin certificado Y sin seguro
     * (no hay cruce, no puede salir dinero y no hay seguro que traspasar), o cualquier otro
     * motivo que impidió generar el pago ({@code mensaje} dice cuál).
     * {@code AL_DIA}: no debe ningún mes a este período — NO es error ni bloqueo, es un final
     * normal (corrección 2026-09-05: antes esto quedaba en {@code null} y el frontend lo
     * mostraba idéntico a un bloqueo real, "Sin novedad" sin explicación).
     *
     * ⛔⛔ Corrección 2026-09-05: el DEFAULT de este campo es {@code "BLOQUEADO"}, no
     * {@code null}. Ningún camino de {@code generarPagoIndividual}/{@code previsualizarJubilado}
     * puede dejarlo sin setear explícitamente — si alguien agrega un return nuevo y se olvida
     * de setearlo, el default lo hace VISIBLE como bloqueado en vez de invisible como "sin
     * novedad" (el defecto real que motivó esta corrección: un jubilado sin VPPC activa, o con
     * valorPagar en $0, salía con participacion null y se veía igual que uno al que la corrida
     * no le aplica).
     */
    private String participacion = "BLOQUEADO";

    public DetallePagoPension() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getIdPago() {
        return idPago;
    }

    public void setIdPago(Long idPago) {
        this.idPago = idPago;
    }

    public Double getValorPension() {
        return valorPension;
    }

    public void setValorPension(Double valorPension) {
        this.valorPension = valorPension;
    }

    public Double getValorSeguroSalud() {
        return valorSeguroSalud;
    }

    public void setValorSeguroSalud(Double valorSeguroSalud) {
        this.valorSeguroSalud = valorSeguroSalud;
    }

    public double getValorPensionMensual() {
        return valorPensionMensual;
    }

    public void setValorPensionMensual(double valorPensionMensual) {
        this.valorPensionMensual = valorPensionMensual;
    }

    public double getValorSeguroMensual() {
        return valorSeguroMensual;
    }

    public void setValorSeguroMensual(double valorSeguroMensual) {
        this.valorSeguroMensual = valorSeguroMensual;
    }

    public double getTotalPension() {
        return totalPension;
    }

    public void setTotalPension(double totalPension) {
        this.totalPension = totalPension;
    }

    public double getTotalSeguro() {
        return totalSeguro;
    }

    public double getValorSeguroInterno() {
        return valorSeguroInterno;
    }

    public void setValorSeguroInterno(double valorSeguroInterno) {
        this.valorSeguroInterno = valorSeguroInterno;
    }

    public void setTotalSeguro(double totalSeguro) {
        this.totalSeguro = totalSeguro;
    }

    public Double getValorCruzadoAPrestamo() {
        return valorCruzadoAPrestamo;
    }

    public void setValorCruzadoAPrestamo(Double valorCruzadoAPrestamo) {
        this.valorCruzadoAPrestamo = valorCruzadoAPrestamo;
    }

    public Double getValorOrdenPago() {
        return valorOrdenPago;
    }

    public void setValorOrdenPago(Double valorOrdenPago) {
        this.valorOrdenPago = valorOrdenPago;
    }

    public boolean isGeneroOrdenPago() {
        return generoOrdenPago;
    }

    public void setGeneroOrdenPago(boolean generoOrdenPago) {
        this.generoOrdenPago = generoOrdenPago;
    }

    public Long getIdAsientoDevengo() {
        return idAsientoDevengo;
    }

    public void setIdAsientoDevengo(Long idAsientoDevengo) {
        this.idAsientoDevengo = idAsientoDevengo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public int getMesesAplicados() {
        return mesesAplicados;
    }

    public void setMesesAplicados(int mesesAplicados) {
        this.mesesAplicados = mesesAplicados;
    }

    public String getMotivoCorte() {
        return motivoCorte;
    }

    public void setMotivoCorte(String motivoCorte) {
        this.motivoCorte = motivoCorte;
    }

    public String getParticipacion() {
        return participacion;
    }

    public void setParticipacion(String participacion) {
        this.participacion = participacion;
    }
}
