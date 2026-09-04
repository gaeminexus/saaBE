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
     * Pensión acumulada de todos los meses generados en esta llamada. NOMINAL: suma
     * {@code mesesAplicados × valorPensionMensual}, el mismo criterio que ya usa
     * {@code PGPC.valorPension} en cada fila (siempre el devengado completo del mes, sin
     * importar si ese mes quedó topado por saldo o deuda exigible — ítem 7 del reporte del
     * retroactivo). Si el último mes quedó parcial, este total NO lo prorratea: sigue
     * contando ese mes completo, igual que ya hace la fila PGPC de ese mes.
     */
    private double totalPension;

    /** Seguro médico acumulado — mismo criterio NOMINAL que {@link #totalPension}. */
    private double totalSeguro;

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
     * "SALDO_AGOTADO" | "SIN_PRESTAMO_SIN_CERTIFICADO". {@code null} fuera del circuito
     * retroactivo.
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
     * que sacar". {@code SOLO_CRUCE}: tiene préstamo, canceló deuda, pero quedó un remanente
     * que no se pudo entregar (sin cuenta o sin certificado). {@code BLOQUEADO}: no participó
     * en absoluto — sin préstamo y sin certificado (no hay cruce posible y no puede salir
     * dinero), o cualquier otro motivo que impidió generar el pago ({@code mensaje} dice cuál).
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
