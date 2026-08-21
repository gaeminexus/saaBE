package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.scp.Empresa;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Parametros normativos de nomina vigentes por anio y empresa. Ningun valor normativo se escribe en Java: todos salen de aqui.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRNM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ParametroNominaId", query = "select e from ParametroNomina e where e.codigo=:id"),
    @NamedQuery(name = "ParametroNominaAll", query = "select e from ParametroNomina e")
})
public class ParametroNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del juego de parametros.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PRNMCDGO")
    private Long codigo;

    /**
     * Empresa propietaria del registro (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Anio de vigencia de los parametros.
     */
    @Basic
    @Column(name = "PRNMANOO")
    private Integer anio;

    /**
     * Salario Basico Unificado del anio.
     */
    @Basic
    @Column(name = "PRNMSBUU")
    private Double sbu;

    /**
     * Canasta familiar basica de enero del anio.
     */
    @Basic
    @Column(name = "PRNMCNBS")
    private Double canastaBasica;

    /**
     * Porcentaje de aporte personal al IESS.
     */
    @Basic
    @Column(name = "PRNMAPPR")
    private Double aportePersonal;

    /**
     * Porcentaje de aporte patronal al IESS.
     */
    @Basic
    @Column(name = "PRNMAPPT")
    private Double aportePatronal;

    /**
     * Porcentaje de aporte al IECE.
     */
    @Basic
    @Column(name = "PRNMIECE")
    private Double iece;

    /**
     * Porcentaje de aporte al SECAP.
     */
    @Basic
    @Column(name = "PRNMSCAP")
    private Double secap;

    /**
     * Porcentaje de fondos de reserva.
     */
    @Basic
    @Column(name = "PRNMFNRS")
    private Double fondosReserva;

    /**
     * Porcentaje de la contribucion CCC sobre la masa salarial declarada.
     *
     * <p>No es un aporte del afiliado ni del patrono: es una linea propia del
     * comprobante de la planilla, calculada sobre la suma de sueldos y no sobre cada
     * uno. Por eso no entra en el renglon de nadie.</p>
     */
    @Basic
    @Column(name = "PRNMCCCP")
    private Double contribucionCcc;

    /**
     * Porcentaje del seguro de salud de la jornada parcial.
     *
     * <p>Se aplica sobre la diferencia entre el salario basico unificado y el sueldo
     * real del afiliado a tiempo parcial, y lo paga el patrono. Aparece en el
     * comprobante de la planilla, no en el rol del trabajador.</p>
     */
    @Basic
    @Column(name = "PRNMSSTP")
    private Double seguroSaludTiempoParcial;

    /**
     * Porcentaje de rebaja por gastos personales.
     */
    @Basic
    @Column(name = "PRNMTPGP")
    private Double porcentajeGastosPersonales;

    /**
     * Numero de canastas de tope cuando hay enfermedad catastrofica.
     */
    @Basic
    @Column(name = "PRNMCNCT")
    private Integer canastasCatastrofica;

    /**
     * Porcentaje total de utilidades a repartir.
     */
    @Basic
    @Column(name = "PRNMUTPR")
    private Double utilidadPorcentaje;

    /**
     * Porcentaje de utilidades que se reparte por dias trabajados.
     */
    @Basic
    @Column(name = "PRNMUTDI")
    private Double utilidadDias;

    /**
     * Porcentaje de utilidades que se reparte por cargas familiares.
     */
    @Basic
    @Column(name = "PRNMUTCG")
    private Double utilidadCargas;

    /**
     * Tope de utilidades por trabajador, expresado en numero de SBU.
     */
    @Basic
    @Column(name = "PRNMUTSB")
    private Integer utilidadTopeSbu;

    /**
     * Dias base del mes comercial.
     */
    @Basic
    @Column(name = "PRNMDIAS")
    private Integer diasMes;

    /**
     * Dias base del anio comercial.
     */
    @Basic
    @Column(name = "PRNMDANO")
    private Integer diasAnio;

    /**
     * Horas base del mes, para calcular el valor hora.
     */
    @Basic
    @Column(name = "PRNMHRMS")
    private Integer horasMes;

    /**
     * Horas de la jornada ordinaria diaria.
     */
    @Basic
    @Column(name = "PRNMHRDI")
    private Integer horasDia;

    /**
     * Porcentaje de recargo de las horas suplementarias.
     */
    @Basic
    @Column(name = "PRNMRCSP")
    private Double recargoSuplementaria;

    /**
     * Porcentaje de recargo de las horas extraordinarias.
     */
    @Basic
    @Column(name = "PRNMRCEX")
    private Double recargoExtraordinaria;

    /**
     * Porcentaje de recargo nocturno sobre la hora ordinaria.
     */
    @Basic
    @Column(name = "PRNMRCNC")
    private Double recargoNocturno;

    /**
     * Hora del dia (0-23) en que empieza la jornada nocturna. Art. 49 del Codigo del
     * Trabajo: 19.
     */
    @Basic
    @Column(name = "PRNMHRIN")
    private Long horaInicioNocturna;

    /**
     * Hora del dia (0-23) en que termina la jornada nocturna. Art. 49 del Codigo del
     * Trabajo: 6. <b>La franja cruza la medianoche</b>: es nocturno lo que va de
     * <code>PRNMHRIN</code> a 24h00 y de 00h00 a <code>PRNMHRFN</code>.
     */
    @Basic
    @Column(name = "PRNMHRFN")
    private Long horaFinNocturna;

    /**
     * Maximo de horas extra por dia.
     */
    @Basic
    @Column(name = "PRNMHRMX")
    private Integer maxHorasDia;

    /**
     * Maximo de horas extra por semana.
     */
    @Basic
    @Column(name = "PRNMHRSX")
    private Integer maxHorasSemana;

    /**
     * Dias de vacaciones base por anio cumplido.
     */
    @Basic
    @Column(name = "PRNMDIVC")
    private Integer diasVacaciones;

    /**
     * Anio de servicio a partir del cual se suma un dia adicional.
     */
    @Basic
    @Column(name = "PRNMANVC")
    private Integer anioVacacionAdicional;

    /**
     * Maximo de dias de vacaciones acumulables por periodo.
     */
    @Basic
    @Column(name = "PRNMMXVC")
    private Integer maxDiasVacaciones;

    /**
     * Anios tras los cuales caduca el saldo de vacaciones.
     */
    @Basic
    @Column(name = "PRNMCDVC")
    private Integer aniosCaducidadVacaciones;

    /**
     * Porcentaje de bonificacion por desahucio sobre la ultima remuneracion.
     */
    @Basic
    @Column(name = "PRNMDSPR")
    private Double porcentajeDesahucio;

    /**
     * Meses minimos de indemnizacion por despido intempestivo.
     */
    @Basic
    @Column(name = "PRNMDIMN")
    private Integer indemnizacionMinima;

    /**
     * Meses maximos de indemnizacion por despido intempestivo.
     */
    @Basic
    @Column(name = "PRNMDIMX")
    private Integer indemnizacionMaxima;

    /**
     * Anios de antiguedad bajo los cuales aplica la indemnizacion minima.
     */
    @Basic
    @Column(name = "PRNMDIAN")
    private Integer aniosIndemnizacionMinima;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "PRNMESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PRNMFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "PRNMUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Double getSbu() {
        return sbu;
    }

    public void setSbu(Double sbu) {
        this.sbu = sbu;
    }

    public Double getCanastaBasica() {
        return canastaBasica;
    }

    public void setCanastaBasica(Double canastaBasica) {
        this.canastaBasica = canastaBasica;
    }

    public Double getAportePersonal() {
        return aportePersonal;
    }

    public void setAportePersonal(Double aportePersonal) {
        this.aportePersonal = aportePersonal;
    }

    public Double getAportePatronal() {
        return aportePatronal;
    }

    public void setAportePatronal(Double aportePatronal) {
        this.aportePatronal = aportePatronal;
    }

    public Double getIece() {
        return iece;
    }

    public void setIece(Double iece) {
        this.iece = iece;
    }

    public Double getSecap() {
        return secap;
    }

    public void setSecap(Double secap) {
        this.secap = secap;
    }

    public Double getFondosReserva() {
        return fondosReserva;
    }

    public void setFondosReserva(Double fondosReserva) {
        this.fondosReserva = fondosReserva;
    }

    public Double getPorcentajeGastosPersonales() {
        return porcentajeGastosPersonales;
    }

    public void setPorcentajeGastosPersonales(Double porcentajeGastosPersonales) {
        this.porcentajeGastosPersonales = porcentajeGastosPersonales;
    }

    public Integer getCanastasCatastrofica() {
        return canastasCatastrofica;
    }

    public void setCanastasCatastrofica(Integer canastasCatastrofica) {
        this.canastasCatastrofica = canastasCatastrofica;
    }

    public Double getUtilidadPorcentaje() {
        return utilidadPorcentaje;
    }

    public void setUtilidadPorcentaje(Double utilidadPorcentaje) {
        this.utilidadPorcentaje = utilidadPorcentaje;
    }

    public Double getUtilidadDias() {
        return utilidadDias;
    }

    public void setUtilidadDias(Double utilidadDias) {
        this.utilidadDias = utilidadDias;
    }

    public Double getUtilidadCargas() {
        return utilidadCargas;
    }

    public void setUtilidadCargas(Double utilidadCargas) {
        this.utilidadCargas = utilidadCargas;
    }

    public Integer getUtilidadTopeSbu() {
        return utilidadTopeSbu;
    }

    public void setUtilidadTopeSbu(Integer utilidadTopeSbu) {
        this.utilidadTopeSbu = utilidadTopeSbu;
    }

    public Integer getDiasMes() {
        return diasMes;
    }

    public void setDiasMes(Integer diasMes) {
        this.diasMes = diasMes;
    }

    public Integer getDiasAnio() {
        return diasAnio;
    }

    public void setDiasAnio(Integer diasAnio) {
        this.diasAnio = diasAnio;
    }

    public Integer getHorasMes() {
        return horasMes;
    }

    public void setHorasMes(Integer horasMes) {
        this.horasMes = horasMes;
    }

    public Integer getHorasDia() {
        return horasDia;
    }

    public void setHorasDia(Integer horasDia) {
        this.horasDia = horasDia;
    }

    public Double getRecargoSuplementaria() {
        return recargoSuplementaria;
    }

    public void setRecargoSuplementaria(Double recargoSuplementaria) {
        this.recargoSuplementaria = recargoSuplementaria;
    }

    public Double getRecargoExtraordinaria() {
        return recargoExtraordinaria;
    }

    public void setRecargoExtraordinaria(Double recargoExtraordinaria) {
        this.recargoExtraordinaria = recargoExtraordinaria;
    }

    public Double getRecargoNocturno() {
        return recargoNocturno;
    }

    public void setRecargoNocturno(Double recargoNocturno) {
        this.recargoNocturno = recargoNocturno;
    }

    public Integer getMaxHorasDia() {
        return maxHorasDia;
    }

    public void setMaxHorasDia(Integer maxHorasDia) {
        this.maxHorasDia = maxHorasDia;
    }

    public Integer getMaxHorasSemana() {
        return maxHorasSemana;
    }

    public void setMaxHorasSemana(Integer maxHorasSemana) {
        this.maxHorasSemana = maxHorasSemana;
    }

    public Integer getDiasVacaciones() {
        return diasVacaciones;
    }

    public void setDiasVacaciones(Integer diasVacaciones) {
        this.diasVacaciones = diasVacaciones;
    }

    public Integer getAnioVacacionAdicional() {
        return anioVacacionAdicional;
    }

    public void setAnioVacacionAdicional(Integer anioVacacionAdicional) {
        this.anioVacacionAdicional = anioVacacionAdicional;
    }

    public Integer getMaxDiasVacaciones() {
        return maxDiasVacaciones;
    }

    public void setMaxDiasVacaciones(Integer maxDiasVacaciones) {
        this.maxDiasVacaciones = maxDiasVacaciones;
    }

    public Integer getAniosCaducidadVacaciones() {
        return aniosCaducidadVacaciones;
    }

    public void setAniosCaducidadVacaciones(Integer aniosCaducidadVacaciones) {
        this.aniosCaducidadVacaciones = aniosCaducidadVacaciones;
    }

    public Double getPorcentajeDesahucio() {
        return porcentajeDesahucio;
    }

    public void setPorcentajeDesahucio(Double porcentajeDesahucio) {
        this.porcentajeDesahucio = porcentajeDesahucio;
    }

    public Integer getIndemnizacionMinima() {
        return indemnizacionMinima;
    }

    public void setIndemnizacionMinima(Integer indemnizacionMinima) {
        this.indemnizacionMinima = indemnizacionMinima;
    }

    public Integer getIndemnizacionMaxima() {
        return indemnizacionMaxima;
    }

    public void setIndemnizacionMaxima(Integer indemnizacionMaxima) {
        this.indemnizacionMaxima = indemnizacionMaxima;
    }

    public Integer getAniosIndemnizacionMinima() {
        return aniosIndemnizacionMinima;
    }

    public void setAniosIndemnizacionMinima(Integer aniosIndemnizacionMinima) {
        this.aniosIndemnizacionMinima = aniosIndemnizacionMinima;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Long getHoraInicioNocturna() {
        return horaInicioNocturna;
    }

    public void setHoraInicioNocturna(Long horaInicioNocturna) {
        this.horaInicioNocturna = horaInicioNocturna;
    }

    public Long getHoraFinNocturna() {
        return horaFinNocturna;
    }

    public void setHoraFinNocturna(Long horaFinNocturna) {
        this.horaFinNocturna = horaFinNocturna;
    }

    public Double getContribucionCcc() {
        return contribucionCcc;
    }

    public void setContribucionCcc(Double contribucionCcc) {
        this.contribucionCcc = contribucionCcc;
    }

    public Double getSeguroSaludTiempoParcial() {
        return seguroSaludTiempoParcial;
    }

    public void setSeguroSaludTiempoParcial(Double seguroSaludTiempoParcial) {
        this.seguroSaludTiempoParcial = seguroSaludTiempoParcial;
    }
}
