package com.saa.ejb.rhh.serviceImpl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.MarcacionesDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.ResumenNominaDaoService;
import com.saa.ejb.rhh.service.ConsolidacionMarcacionesService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.DetalleTurno;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.Marcaciones;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.ResumenNomina;
import com.saa.model.rhh.Turno;
import com.saa.rubros.RhhOrigenMarcacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de ConsolidacionMarcacionesService.</p>
 *
 * <h3>Las tres clases de hora</h3>
 *
 * <table border="1">
 *   <tr><th>Clase</th><th>Cuando</th><th>Recargo</th></tr>
 *   <tr><td>Suplementaria</td><td>Exceso sobre la jornada en dia laborable, hasta las 24h00</td><td><code>PRNMRCSP</code></td></tr>
 *   <tr><td>Extraordinaria</td><td>Entre 24h00 y 06h00, y todo lo trabajado en dia no laborable</td><td><code>PRNMRCEX</code></td></tr>
 *   <tr><td>Recargo nocturno</td><td>Jornada <b>ordinaria</b> entre 19h00 y 06h00</td><td><code>PRNMRCNC</code></td></tr>
 * </table>
 *
 * <p>El recargo nocturno <b>no es una hora extra</b>: es un recargo sobre la hora ordinaria, y
 * por eso se cuenta aparte y no se suma a las suplementarias.</p>
 *
 * <p><b>Ningun valor normativo esta en el codigo</b>, incluidas las horas que delimitan la
 * franja nocturna: salen de <code>PRNMHRIN</code> y <code>PRNMHRFN</code>. Estuvieron un rato
 * como constantes con el argumento de que el Art. 49 es definicion legal y no parametro de
 * empresa, y era un argumento malo: <code>PRNMRCNC</code> --el 25 % del <b>mismo inciso</b>--
 * ya vivia en <code>PRNM</code>, igual que los 15 dias del Art. 69 y el 25 % del Art. 185.
 * Parametrizar el porcentaje y quemar las horas del mismo articulo es una inconsistencia. La
 * regla 1 del maestro no dice "nada que dependa de la empresa": dice que un cambio de
 * normativa se resuelve con un <code>UPDATE</code> y nunca con un despliegue.</p>
 */
@Stateless
public class ConsolidacionMarcacionesServiceImpl implements ConsolidacionMarcacionesService {

    /** Bandera afirmativa. */
    private static final String SI = "S";

    /** Bandera negativa. */
    private static final String NO = "N";

    /** Minutos de una hora, para convertir duraciones. */
    private static final double MINUTOS_POR_HORA = 60D;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private MarcacionesDaoService marcacionesDaoService;

    @EJB
    private ResumenNominaDaoService resumenNominaDaoService;

    @EJB
    private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

    @EJB
    private ParametroNominaDaoService parametroNominaDaoService;

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ConsolidacionMarcacionesService#consolidar(java.time.LocalDate, java.time.LocalDate, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int consolidar(LocalDate desde, LocalDate hasta, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo consolidar de consolidacionMarcaciones service, rango: "
                + desde + " a " + hasta);

        if (desde == null || hasta == null) {
            throw new IncomeException("La consolidacion exige la fecha desde y la fecha hasta.");
        }
        if (hasta.isBefore(desde)) {
            throw new IncomeException("La fecha hasta (" + hasta + ") es anterior a la fecha desde ("
                    + desde + ").");
        }

        List<Marcaciones> marcaciones = marcacionesDaoService.selectPendientesConsolidar(desde, hasta);
        if (marcaciones == null || marcaciones.isEmpty()) {
            System.out.println("No hay marcaciones pendientes de consolidar en el rango.");
            return 0;
        }

        // Agrupacion por (empleado, dia). LinkedHashMap conserva el orden de la consulta, que
        // ya viene por empleado y hora: eso hace que dentro de cada grupo las marcaciones
        // esten ordenadas sin volver a ordenarlas.
        Map<String, List<Marcaciones>> grupos = new LinkedHashMap<String, List<Marcaciones>>();
        for (Marcaciones marcacion : marcaciones) {
            if (marcacion.getEmpleado() == null || marcacion.getFechaHora() == null) {
                continue;
            }
            String clave = marcacion.getEmpleado().getCodigo() + "|"
                    + marcacion.getFechaHora().toLocalDate();
            List<Marcaciones> grupo = grupos.get(clave);
            if (grupo == null) {
                grupo = new ArrayList<Marcaciones>();
                grupos.put(clave, grupo);
            }
            grupo.add(marcacion);
        }

        int generados = 0;
        for (Map.Entry<String, List<Marcaciones>> entrada : grupos.entrySet()) {
            List<Marcaciones> grupo = entrada.getValue();
            Empleado empleado = grupo.get(0).getEmpleado();
            LocalDate dia = grupo.get(0).getFechaHora().toLocalDate();

            ResumenNomina resumen = localizaResumen(empleado.getCodigo(), dia);
            if (resumen != null && SI.equals(resumen.getProcesado())) {
                // Un dia ya procesado en un periodo cerrado no se recalcula: cambiaria la base
                // de una nomina ya pagada.
                System.out.println("Resumen de " + empleado.getIdentificacion() + " del " + dia
                        + " ya procesado en un periodo cerrado: no se recalcula.");
                continue;
            }
            if (resumen == null) {
                resumen = new ResumenNomina();
                resumen.setEmpleado(empleado);
                resumen.setFecha(dia);
                resumen.setFechaRegistro(LocalDate.now());
                resumen.setUsuarioRegistro(usuario);
            }

            armaResumen(resumen, grupo, empleado, dia, usuario);
            resumenNominaDaoService.save(resumen, resumen.getCodigo());

            // Las marcaciones del grupo quedan marcadas: una reconsolidacion no las cuenta dos
            // veces.
            for (Marcaciones marcacion : grupo) {
                marcacion.setProcesado(SI);
                marcacionesDaoService.save(marcacion, marcacion.getCodigo());
            }
            generados++;
        }

        System.out.println("Consolidacion terminada: " + generados + " resumen(es) diario(s).");
        return generados;
    }

    // =====================================================================
    // Piezas
    // =====================================================================

    /**
     * Calcula el resumen de un dia a partir de sus marcaciones.
     *
     * @param resumen		: Resumen a llenar
     * @param grupo			: Marcaciones del dia, ordenadas por hora
     * @param empleado		: Empleado
     * @param dia			: Dia
     * @param usuario		: Usuario que ejecuta
     * @throws Throwable	: Excepcion
     */
    private void armaResumen(ResumenNomina resumen, List<Marcaciones> grupo, Empleado empleado,
            LocalDate dia, String usuario) throws Throwable {

        LocalDateTime entrada = grupo.get(0).getFechaHora();
        LocalDateTime salida = grupo.get(grupo.size() - 1).getFechaHora();

        resumen.setEntradaReal(entrada);
        resumen.setSalidaReal(salida);
        resumen.setHoraEntrada(entrada.toLocalTime().toString());
        resumen.setHoraSalida(salida.toLocalTime().toString());
        resumen.setFuente(Long.valueOf(RhhOrigenMarcacion.IMPORTACION_ARCHIVO));
        resumen.setProcesado(NO);

        // RSMNASNT y RSMNJSTF son NOT NULL --declarados como CHECK con nombre de sistema, que
        // all_tab_columns no ve-- y ninguna capa los llenaba: la consolidacion moria con
        // ORA-02290 en la primera fila. Comprobado el 2026-08-20 aislando las restricciones una
        // a una: SYS_C009282 es RSMNASNT y SYS_C009283 es RSMNJSTF.
        // Un dia reconstruido desde marcaciones NO es una ausencia, asi que 'N' es el valor
        // correcto y no un relleno. Se siembran solo si estan en nulo, para no borrar una
        // ausencia registrada a mano sobre un resumen que ya existia.
        if (resumen.getAusencia() == null) {
            resumen.setAusencia(NO);
        }
        if (resumen.getJustificado() == null) {
            resumen.setJustificado(NO);
        }

        // Un numero impar de marcaciones deja el dia sin cerrar: no se adivina cual falta.
        boolean impar = grupo.size() % 2 != 0;
        resumen.setInconsistente(impar ? SI : NO);

        // Presencia bruta menos los intervalos intermedios --almuerzo, permisos--, que son los
        // pares (2,3), (4,5)... dentro del dia.
        double minutosPresencia = Duration.between(entrada, salida).toMinutes();
        double minutosFuera = 0D;
        for (int i = 1; i + 1 < grupo.size(); i += 2) {
            LocalDateTime sale = grupo.get(i).getFechaHora();
            LocalDateTime vuelve = grupo.get(i + 1).getFechaHora();
            if (sale != null && vuelve != null && vuelve.isAfter(sale)) {
                minutosFuera += Duration.between(sale, vuelve).toMinutes();
            }
        }
        double minutosTrabajados = Math.max(0D, minutosPresencia - minutosFuera);
        resumen.setHorasTrabajadas(RedondeoNomina.redondeaCantidad(
                Double.valueOf(minutosTrabajados / MINUTOS_POR_HORA)));

        // Turno teorico del dia de la semana.
        DetalleTurno detalle = localizaDetalleTurno(empleado, dia);
        Turno turno = detalle != null ? detalle.getTurno() : null;
        boolean laborable = detalle == null || !NO.equals(detalle.getLaborable());

        LocalTime entradaTeorica = hora(detalle != null ? detalle.getHoraEntrada() : null,
                turno != null ? turno.getHoraEntrada() : null);
        LocalTime salidaTeorica = hora(detalle != null ? detalle.getHoraSalida() : null,
                turno != null ? turno.getHoraSalida() : null);
        int tolerancia = turno != null && turno.getMinutosTolerancia() != null
                ? turno.getMinutosTolerancia().intValue() : 0;

        // Atraso y salida anticipada, ambos con piso en cero.
        int atraso = 0;
        if (entradaTeorica != null) {
            long diferencia = Duration.between(entradaTeorica, entrada.toLocalTime()).toMinutes();
            atraso = (int) Math.max(0L, diferencia - tolerancia);
        }
        resumen.setMinutosTarde(Integer.valueOf(atraso));

        int anticipada = 0;
        if (salidaTeorica != null) {
            long diferencia = Duration.between(salida.toLocalTime(), salidaTeorica).toMinutes();
            anticipada = (int) Math.max(0L, diferencia);
        }
        resumen.setMinutosSalidaAnticipada(Integer.valueOf(anticipada));

        // Descanso no remunerado de la jornada -- el almuerzo. Manda el del dia y el del turno
        // es el respaldo, la misma precedencia que ya tienen la entrada y la salida.
        int minutosDescanso = descanso(detalle, turno);

        // Segunda mitad: si el dia NO trae pares intermedios, el reloj no registro la salida a
        // almorzar y el descanso hay que restarlo de las horas trabajadas tambien.
        //
        // La condicion se resuelve por lo que trae CADA DIA, no por como este configurado el
        // reloj: un dia con cuatro marcaciones usa el almuerzo real --que ya se resto en
        // minutosFuera-- y restar ademas el teorico lo contaria dos veces; un dia con dos usa el
        // teorico. Cubre igual al reloj que solo marca entrada y salida, a la persona que marca
        // cuatro y a la que un martes olvida marcar la salida a almorzar.
        //
        // Solo en dia laborable: en uno no laborable no hay jornada programada, asi que tampoco
        // hay un almuerzo previsto que descontar.
        if (laborable && minutosFuera == 0D && minutosDescanso > 0 && minutosTrabajados > 0D) {
            minutosTrabajados = Math.max(0D, minutosTrabajados - minutosDescanso);
            resumen.setHorasTrabajadas(RedondeoNomina.redondeaCantidad(
                    Double.valueOf(minutosTrabajados / MINUTOS_POR_HORA)));
            System.out.println("El dia " + dia + " de " + empleado.getIdentificacion()
                    + " no registra salida a almorzar: se descuentan los " + minutosDescanso
                    + " minutos de descanso del turno.");
        }

        // Jornada teorica = intervalo del turno MENOS el descanso.
        //
        // El intervalo es bruto: de 08:30 a 17:30 son nueve horas, pero con una hora de almuerzo
        // el trabajador solo puede acumular ocho. Sin restarlo, nadie alcanzaba nunca su jornada
        // y las horas suplementarias no empezaban a contar hasta las 18:30 en vez de las 17:30
        // -- una hora de trabajo extra al dia sin pagar, en silencio.
        double horasJornada = 0D;
        if (laborable && entradaTeorica != null && salidaTeorica != null) {
            double minutosJornada = Duration.between(entradaTeorica, salidaTeorica).toMinutes()
                    - minutosDescanso;
            horasJornada = Math.max(0D, minutosJornada) / MINUTOS_POR_HORA;
        }

        double horasTrabajadas = minutosTrabajados / MINUTOS_POR_HORA;
        double exceso = Math.max(0D, horasTrabajadas - horasJornada);

        // En dia no laborable todo lo trabajado es extraordinario; en laborable, el exceso es
        // suplementario salvo la parte que cae despues de medianoche.
        double extraordinarias;
        double suplementarias;
        if (!laborable) {
            extraordinarias = horasTrabajadas;
            suplementarias = 0D;
        } else {
            double despuesDeMedianoche = horasDespuesDeMedianoche(entrada, salida);
            extraordinarias = Math.min(exceso, despuesDeMedianoche);
            suplementarias = Math.max(0D, exceso - extraordinarias);
        }

        ParametroNomina prnm = parametroDelAnio(empleado, dia);

        // El recargo nocturno se cuenta sobre la jornada ORDINARIA, no sobre el exceso.
        double nocturnas = Math.min(horasTrabajadas - exceso,
                horasEnFranjaNocturna(entrada, salida, prnm));

        aplicaTopes(resumen, suplementarias, extraordinarias, Math.max(0D, nocturnas), prnm,
                empleado, dia);

        if (impar) {
            // Un dia sin cerrar no produce numeros: los pone en cero.
            //
            // No es un dato incompleto, es un dato FALSO. Con una sola marcacion la salida se
            // toma igual a la entrada, y de ahi salia una salida anticipada de 540 minutos --
            // en un informe que sume esa columna, alguien que no se fue antes aparece
            // marchandose nueve horas antes de tiempo.
            //
            // Se anulan tambien el atraso y las horas, y no solo lo derivado de la salida:
            // cuando el numero de marcaciones es impar NO SE SABE cual falta. Si la que falta
            // es la de entrada, la que se tomo como tal es en realidad una salida y el atraso
            // calculado sobre ella tampoco significa nada.
            //
            // Lo que SI se conserva son los hechos observados --entradaReal, salidaReal y sus
            // dos horas de texto--, porque son lo que necesita quien vaya a corregir el dia a
            // mano. La marca de inconsistente ya dice que hay que revisarlo.
            resumen.setHorasTrabajadas(Double.valueOf(0D));
            resumen.setHorasSuplementarias(Double.valueOf(0D));
            resumen.setHorasExtraordinarias(Double.valueOf(0D));
            resumen.setHorasNocturnas(Double.valueOf(0D));
            resumen.setMinutosTarde(Integer.valueOf(0));
            resumen.setMinutosSalidaAnticipada(Integer.valueOf(0));
            resumen.setMinutosExtra(Integer.valueOf(0));
            System.out.println("Dia inconsistente de " + empleado.getIdentificacion() + " el "
                    + dia + " (" + grupo.size() + " marcacion(es), numero impar): los valores"
                    + " calculados quedan en cero hasta que se corrija a mano.");
        }

        resumen.setUsuarioRegistro(usuario);
    }

    /**
     * Asigna las horas extra al resumen, avisando cuando superan el tope diario.
     *
     * <p>El tope <b>no recorta</b> las horas: la hora se trabajo y hay que pagarla. Lo que hace
     * es dejar traza, porque exceder <code>PRNMHRMX</code> es una infraccion laboral que la
     * empresa tiene que ver.</p>
     *
     * @param resumen			: Resumen a llenar
     * @param suplementarias	: Horas al 50 por ciento
     * @param extraordinarias	: Horas al 100 por ciento
     * @param nocturnas			: Horas ordinarias con recargo nocturno
     * @param prnm				: Parametros del anio, puede ser null
     * @param empleado			: Empleado, para el aviso
     * @param dia				: Dia, para el aviso
     */
    private void aplicaTopes(ResumenNomina resumen, double suplementarias, double extraordinarias,
            double nocturnas, ParametroNomina prnm, Empleado empleado, LocalDate dia) {

        resumen.setHorasSuplementarias(RedondeoNomina.redondeaCantidad(Double.valueOf(suplementarias)));
        resumen.setHorasExtraordinarias(RedondeoNomina.redondeaCantidad(Double.valueOf(extraordinarias)));
        resumen.setHorasNocturnas(RedondeoNomina.redondeaCantidad(Double.valueOf(nocturnas)));
        // minutosExtra conserva el total de exceso en minutos, que es lo que la pantalla de
        // asistencia ya mostraba antes de que existieran las tres columnas de horas.
        resumen.setMinutosExtra(Integer.valueOf(
                (int) Math.round((suplementarias + extraordinarias) * MINUTOS_POR_HORA)));

        if (prnm != null && prnm.getMaxHorasDia() != null) {
            double extra = suplementarias + extraordinarias;
            if (extra > prnm.getMaxHorasDia().doubleValue()) {
                System.out.println("Aviso: " + empleado.getIdentificacion() + " el " + dia
                        + " acumula " + extra + " hora(s) extra, por encima del tope diario de "
                        + prnm.getMaxHorasDia() + " (PRNMHRMX). No se recorta: la hora se trabajo.");
            }
        }
    }

    /**
     * Horas trabajadas que caen despues de medianoche, que son extraordinarias.
     *
     * @param entrada	: Entrada real
     * @param salida	: Salida real
     * @return			: Horas despues de las 24h00
     */
    private double horasDespuesDeMedianoche(LocalDateTime entrada, LocalDateTime salida) {
        LocalDateTime medianoche = entrada.toLocalDate().plusDays(1).atStartOfDay();
        if (!salida.isAfter(medianoche)) {
            return 0D;
        }
        LocalDateTime inicio = entrada.isAfter(medianoche) ? entrada : medianoche;
        return Duration.between(inicio, salida).toMinutes() / MINUTOS_POR_HORA;
    }

    /**
     * Horas trabajadas dentro de la franja nocturna que declara la parametria del anio.
     *
     * @param entrada	: Entrada real
     * @param salida	: Salida real
     * @param prnm		: Parametros del anio, de donde salen PRNMHRIN y PRNMHRFN
     * @return			: Horas dentro de la franja, o cero si los limites no estan informados
     */
    private double horasEnFranjaNocturna(LocalDateTime entrada, LocalDateTime salida,
            ParametroNomina prnm) {

        if (prnm == null || prnm.getHoraInicioNocturna() == null
                || prnm.getHoraFinNocturna() == null) {
            // Sin los limites parametrizados no se inventa una franja: se informan cero horas
            // nocturnas y queda la traza. Suponer 19-6 seria volver a quemar el dato.
            System.out.println("PRNMHRIN o PRNMHRFN no estan informadas: no se calculan horas"
                    + " nocturnas. Ejecute el script 16 o complete la parametria del anio.");
            return 0D;
        }
        int inicioNocturna = prnm.getHoraInicioNocturna().intValue();
        int finNocturna = prnm.getHoraFinNocturna().intValue();
        double minutos = 0D;
        LocalDateTime momento = entrada;
        // Se recorre minuto a minuto en tramos de una hora para no tener que resolver a mano
        // el solapamiento de la franja con el cambio de dia. Una jornada no pasa de unas pocas
        // decenas de iteraciones.
        while (momento.isBefore(salida)) {
            LocalDateTime siguiente = momento.plusMinutes(1);
            if (siguiente.isAfter(salida)) {
                siguiente = salida;
            }
            int hora = momento.getHour();
            // OJO: la franja CRUZA LA MEDIANOCHE, asi que la condicion es un OR, no un AND.
            // Escrita como (hora >= inicio && hora < fin) daria SIEMPRE cero horas nocturnas
            // --ninguna hora es a la vez mayor que 19 y menor que 6-- y lo haria en silencio,
            // sin error ni aviso: el recargo del Art. 49 no se pagaria y nadie lo notaria.
            if (hora >= inicioNocturna || hora < finNocturna) {
                minutos += Duration.between(momento, siguiente).toMinutes();
            }
            momento = siguiente;
        }
        return minutos / MINUTOS_POR_HORA;
    }

    /**
     * Localiza el resumen diario de un empleado, si ya existe.
     *
     * @param idEmpleado	: Id del empleado
     * @param dia			: Dia
     * @return				: El resumen, o null
     */
    @SuppressWarnings("unchecked")
    private ResumenNomina localizaResumen(Long idEmpleado, LocalDate dia) {
        List<ResumenNomina> lista = em.createQuery(" select t "
                + " from   ResumenNomina t "
                + " where  t.empleado.codigo = :idEmpleado and t.fecha = :dia ")
                .setParameter("idEmpleado", idEmpleado)
                .setParameter("dia", dia)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    /**
     * Localiza el detalle de turno del empleado para el dia de la semana.
     *
     * @param empleado		: Empleado
     * @param dia			: Dia
     * @return				: El detalle del turno, o null si el contrato no tiene turno
     * @throws Throwable	: Excepcion
     */
    @SuppressWarnings("unchecked")
    private DetalleTurno localizaDetalleTurno(Empleado empleado, LocalDate dia) throws Throwable {
        List<ContratoEmpleado> contratos = contratoEmpleadoDaoService.selectActivosEnPeriodo(
                empleado.getEmpresa() != null ? empleado.getEmpresa().getCodigo() : null, dia, dia);
        Turno turnoContrato = null;
        if (contratos != null) {
            for (ContratoEmpleado contrato : contratos) {
                if (contrato.getEmpleado() != null
                        && empleado.getCodigo().equals(contrato.getEmpleado().getCodigo())) {
                    turnoContrato = contrato.getTurno();
                    break;
                }
            }
        }
        if (turnoContrato == null || turnoContrato.getCodigo() == null) {
            // Sin turno no hay horario teorico: no se inventa uno. El resumen sale con las
            // horas trabajadas y sin atraso, que es lo unico que se puede afirmar.
            return null;
        }
        // getDayOfWeek() da 1 lunes .. 7 domingo, que es la convencion de DTLLDIAA.
        int diaSemana = dia.getDayOfWeek().getValue();
        List<DetalleTurno> lista = em.createQuery(" select t "
                + " from   DetalleTurno t "
                + " where  t.turno.codigo = :idTurno and t.diaSemana = :diaSemana ")
                .setParameter("idTurno", turnoContrato.getCodigo())
                .setParameter("diaSemana", Integer.valueOf(diaSemana))
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    /**
     * Recupera los parametros del anio del dia consolidado.
     *
     * @param empleado	: Empleado
     * @param dia		: Dia
     * @return			: Los parametros, o null si no estan cargados
     */
    private ParametroNomina parametroDelAnio(Empleado empleado, LocalDate dia) {
        try {
            Long idEmpresa = empleado.getEmpresa() != null ? empleado.getEmpresa().getCodigo() : null;
            return parametroNominaDaoService.selectByAnio(idEmpresa,
                    Integer.valueOf(dia.getYear()));
        } catch (Throwable e) {
            // La consolidacion no depende de los parametros: sin ellos solo se pierde el aviso
            // de tope, que es informativo. No vale abortar el lote por eso.
            System.out.println("No se pudieron leer los parametros del anio " + dia.getYear()
                    + ": se consolida sin el control de tope. " + e.getMessage());
            return null;
        }
    }

    /**
     * Convierte a hora el texto del detalle, con respaldo en el del turno.
     *
     * @param delDetalle	: Hora del detalle del dia
     * @param delTurno		: Hora general del turno
     * @return				: La hora, o null si ninguna es legible
     */
    private LocalTime hora(String delDetalle, String delTurno) {
        LocalTime resultado = parseaHora(delDetalle);
        return resultado != null ? resultado : parseaHora(delTurno);
    }

    /**
     * Minutos de descanso aplicables al dia: <b>manda el del detalle y el del turno es el
     * respaldo</b>, la misma precedencia que <code>hora(...)</code> aplica a la entrada y a la
     * salida. Nulo en los dos lados significa jornada continua, sin descanso.
     *
     * @param detalle	: Detalle del dia de la semana, puede ser null
     * @param turno		: Turno del contrato, puede ser null
     * @return			: Minutos de descanso, o cero si no hay ninguno configurado
     */
    private int descanso(DetalleTurno detalle, Turno turno) {
        if (detalle != null && detalle.getMinutosDescanso() != null) {
            return Math.max(0, detalle.getMinutosDescanso().intValue());
        }
        if (turno != null && turno.getMinutosDescanso() != null) {
            return Math.max(0, turno.getMinutosDescanso().intValue());
        }
        return 0;
    }

    /**
     * Lee una hora en formato HH:mm o HH:mm:ss.
     *
     * @param valor	: Texto de la hora
     * @return		: La hora, o null si no es legible
     */
    private LocalTime parseaHora(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(valor.trim());
        } catch (Throwable e) {
            System.out.println("Hora de turno ilegible: '" + valor + "'.");
            return null;
        }
    }
}
