package com.saa.ejb.crd.serviceImpl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.SumaAseguradaService;
import com.saa.ejb.crd.service.dto.FilaResultadoSumaAsegurada;
import com.saa.ejb.crd.service.dto.ResultadoCargaSumaAsegurada;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.TipoSeguro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see SumaAseguradaService
 */
@Stateless
public class SumaAseguradaServiceImpl implements SumaAseguradaService {

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @Override
    public Double actualizar(Long idPrestamo, Double valor, String usuario) throws Throwable {
        System.out.println("SumaAseguradaServiceImpl.actualizar - idPrestamo: " + idPrestamo
            + " - valor: " + valor + " - usuario: " + usuario);

        if (idPrestamo == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idPrestamo es obligatorio");
        }
        if (valor == null || valor <= 0.0) {
            throw new IncomeException(ERR_VALOR_INVALIDO + ": el valor debe ser mayor a 0");
        }

        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException(ERR_PRESTAMO_NO_ENCONTRADO + ": no existe el préstamo " + idPrestamo);
        }
        validarHipotecarioOPrendario(prestamo);

        Double anterior = prestamo.getValorAsegurado();
        prestamo.setValorAsegurado(redondear(valor));
        prestamoDaoService.save(prestamo, prestamo.getCodigo());
        return anterior;
    }

    @Override
    public ResultadoCargaSumaAsegurada cargarDesdeExcel(InputStream archivo, boolean confirmar, String usuario)
            throws Throwable {
        System.out.println("SumaAseguradaServiceImpl.cargarDesdeExcel - confirmar: " + confirmar
            + " - usuario: " + usuario);

        if (archivo == null) {
            throw new IncomeException(ERR_ARCHIVO_INVALIDO + ": no se recibió ningún archivo");
        }

        List<FilaResultadoSumaAsegurada> filas = new ArrayList<>();
        Set<Long> idAsoprepVistos = new HashSet<>();
        int ok = 0;
        int conError = 0;

        try (Workbook workbook = WorkbookFactory.create(archivo)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Cell primeraCelda = row.getCell(0);
                if (primeraCelda == null || primeraCelda.getCellType() == CellType.BLANK) {
                    continue;
                }

                FilaResultadoSumaAsegurada fila = new FilaResultadoSumaAsegurada();
                fila.setFila(i + 1); // 1-based; la fila 1 es el encabezado, la primera de datos es la 2

                Double idAsoprepDouble = getCellValueAsDouble(primeraCelda);
                Long idAsoprep = idAsoprepDouble != null ? idAsoprepDouble.longValue() : null;
                fila.setIdAsoprep(idAsoprep);

                Double valor = getCellValueAsDouble(row.getCell(1));
                fila.setValor(valor);

                if (idAsoprep == null) {
                    fila.setResultado("VALOR_INVALIDO");
                    filas.add(fila);
                    conError++;
                    continue;
                }
                if (!idAsoprepVistos.add(idAsoprep)) {
                    fila.setResultado("DUPLICADO_EN_ARCHIVO");
                    filas.add(fila);
                    conError++;
                    continue;
                }
                if (valor == null || valor <= 0.0) {
                    fila.setResultado("VALOR_INVALIDO");
                    filas.add(fila);
                    conError++;
                    continue;
                }

                Prestamo prestamo = prestamoDaoService.selectByIdAsoprep(idAsoprep);
                if (prestamo == null) {
                    fila.setResultado("NO_EXISTE");
                    filas.add(fila);
                    conError++;
                    continue;
                }
                fila.setIdPrestamo(prestamo.getCodigo());

                Long tipoPrestamo = tipoPrestamoDe(prestamo);
                if (tipoPrestamo == null || (tipoPrestamo != TipoSeguro.INCENDIO
                        && tipoPrestamo != TipoSeguro.PRENDARIO)) {
                    fila.setResultado("NO_ES_HIPOTECARIO_NI_PRENDARIO");
                    filas.add(fila);
                    conError++;
                    continue;
                }

                fila.setValorAnterior(prestamo.getValorAsegurado());
                fila.setResultado("OK");
                filas.add(fila);
                ok++;

                if (confirmar) {
                    prestamo.setValorAsegurado(redondear(valor));
                    prestamoDaoService.save(prestamo, prestamo.getCodigo());
                }
            }
        } catch (IOException e) {
            throw new IncomeException(ERR_ARCHIVO_INVALIDO + ": no se pudo leer el archivo — " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof IncomeException) {
                throw e;
            }
            throw new IncomeException(ERR_ARCHIVO_INVALIDO + ": el archivo no tiene un formato de Excel válido — "
                + e.getMessage());
        }

        ResultadoCargaSumaAsegurada resultado = new ResultadoCargaSumaAsegurada();
        resultado.setTotal(filas.size());
        resultado.setOk(ok);
        resultado.setConError(conError);
        resultado.setFilas(filas);
        System.out.println("  SumaAseguradaServiceImpl.cargarDesdeExcel - total: " + resultado.getTotal()
            + " - ok: " + ok + " - conError: " + conError + " - confirmar: " + confirmar);
        return resultado;
    }

    private void validarHipotecarioOPrendario(Prestamo prestamo) throws Throwable {
        Long tipoPrestamo = tipoPrestamoDe(prestamo);
        if (tipoPrestamo == null || (tipoPrestamo != TipoSeguro.INCENDIO && tipoPrestamo != TipoSeguro.PRENDARIO)) {
            throw new IncomeException(ERR_NO_ES_HIPOTECARIO_NI_PRENDARIO + ": el préstamo "
                + prestamo.getCodigo() + " no es hipotecario ni prendario");
        }
    }

    /** CRD.TPPR.TPPRCDGO — 1 quirografario, 2 hipotecario, 3 prendario (ÍTEM 0b, mismos códigos que Petro). */
    private Long tipoPrestamoDe(Prestamo prestamo) {
        return prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
            ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String valor = cell.getStringCellValue().trim();
                    return valor.isEmpty() ? null : Double.parseDouble(valor);
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
