package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PrestamoDaoServiceImpl extends EntityDaoImpl<Prestamo> implements PrestamoDaoService {

    // Inicializa persistence context
    @PersistenceContext
    EntityManager em;

    @Override
    public Prestamo selectByIdAsoprep(Long idAsoprep) throws Throwable {
        if (idAsoprep == null) {
            return null;
        }
        
        try {
            Query query = em.createQuery("SELECT p FROM Prestamo p WHERE p.idAsoprep = :idAsoprep");
            query.setParameter("idAsoprep", idAsoprep);
            
            @SuppressWarnings("unchecked")
            List<Prestamo> resultados = query.getResultList();
            
            if (resultados != null && !resultados.isEmpty()) {
                return resultados.get(0);
            }
            
            return null;
            
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            System.err.println("Error al buscar préstamo por idAsoprep: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Prestamo> selectByEntidadYProductoActivos(String rolPetroComercial, String codigoPetroProducto) throws Throwable {
        try {
            // Convertir el rolPetroComercial a Long ya que el campo en la entidad es Long
            Long rolPetroLong = null;
            if (rolPetroComercial != null && !rolPetroComercial.trim().isEmpty()) {
                try {
                    rolPetroLong = Long.parseLong(rolPetroComercial);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El ROL Petro debe ser un número válido: " + rolPetroComercial, e);
                }
            }
            
            String jpql = "SELECT p FROM Prestamo p " +
                         "WHERE p.entidad.rolPetroComercial = :rolPetro " +
                         "AND p.producto.codigoPetro = :codigoProducto " +
                         "AND p.idEstado IN (2, 8, 10, 11)";
            
            Query query = em.createQuery(jpql);
            query.setParameter("rolPetro", rolPetroLong);
            query.setParameter("codigoProducto", codigoPetroProducto);
            
            return query.getResultList();
            
        } catch (Exception e) {
            System.err.println("Error al buscar préstamos activos: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Prestamo> selectByEntidadYProductoActivosById(Long codigoEntidad, Long codigoProducto) throws Throwable {
        try {
            String jpql = "SELECT p FROM Prestamo p " +
                         "WHERE p.entidad.codigo = :codigoEntidad " +
                         "AND p.producto.codigo = :codigoProducto " +
                         "AND p.idEstado IN (1, 2, 8, 10, 11)";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoEntidad", codigoEntidad);
            query.setParameter("codigoProducto", codigoProducto);
            
            return query.getResultList();
            
        } catch (Exception e) {
            System.err.println("Error al buscar préstamos activos por IDs: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar lista vacía para no detener el proceso
            // El error se registrará como novedad en el nivel superior
            return new java.util.ArrayList<>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Prestamo> selectByRangoFechas(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
        Query query = em.createQuery(
            "select p from Prestamo p " +
            "where p.fecha >= :fechaInicio and p.fecha <= :fechaFin"
        );
        query.setParameter("fechaInicio", fechaInicio);
        query.setParameter("fechaFin", fechaFin);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Prestamo> selectByEstado(Long estado) throws Throwable {
        Query query = em.createQuery(
            "select p from Prestamo p where p.idEstado = :estado"
        );
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Prestamo> selectVigentesByEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("PrestamoDaoServiceImpl.selectVigentesByEntidad - Entidad: "
            + codigoEntidad);

        if (codigoEntidad == null) {
            return new java.util.ArrayList<>();
        }

        try {
            // idEstado (PRSTIDST) es el estado operativo. NUNCA estadoPrestamo (ESPSCDGO),
            // que es la FK al catálogo CRD.ESPS.
            // Los tres terminales quedan fuera: 3 CANCELADO, 4 CANCELADO_ANTICIPADO,
            // 5 CANCELADO_POR_NOVACION. El idEstado nulo entra: no es un estado terminal, y
            // en un aviso subreportar deuda es peor que sobrereportarla.
            String jpql = "SELECT p FROM Prestamo p " +
                         "WHERE p.entidad.codigo = :codigoEntidad " +
                         "AND (p.idEstado IS NULL OR p.idEstado NOT IN " +
                         "     (:cancelado, :canceladoAnticipado, :canceladoPorNovacion)) " +
                         "ORDER BY p.codigo ASC";

            Query query = em.createQuery(jpql);
            query.setParameter("codigoEntidad", codigoEntidad);
            query.setParameter("cancelado",
                (long) com.saa.rubros.EstadoPrestamo.CANCELADO);
            query.setParameter("canceladoAnticipado",
                (long) com.saa.rubros.EstadoPrestamo.CANCELADO_ANTICIPADO);
            query.setParameter("canceladoPorNovacion",
                (long) com.saa.rubros.EstadoPrestamo.CANCELADO_POR_NOVACION);

            List<Prestamo> resultados = query.getResultList();
            System.out.println("  Préstamos vigentes encontrados: "
                + (resultados != null ? resultados.size() : 0));
            return resultados;

        } catch (Exception e) {
            System.err.println("Error en selectVigentesByEntidad: " + e.getMessage());
            e.printStackTrace();
            // NO lanzar excepción - retornar lista vacía para no detener el proceso.
            // Este método alimenta un aviso informativo: un fallo de consulta no debe
            // romper la pantalla de devolución.
            return new java.util.ArrayList<>();
        }
    }

    @Override
    public List<Prestamo> selectByEntidad(Long codigoEntidad) throws Throwable {
        System.out.println("PrestamoDaoServiceImpl.selectByEntidad - Entidad: " + codigoEntidad);

        if (codigoEntidad == null) {
            return new java.util.ArrayList<>();
        }

        Query query = em.createQuery(
            "SELECT p FROM Prestamo p WHERE p.entidad.codigo = :codigoEntidad ORDER BY p.codigo");
        query.setParameter("codigoEntidad", codigoEntidad);

        List<Prestamo> resultados = query.getResultList();
        System.out.println("  Préstamos encontrados: " + (resultados != null ? resultados.size() : 0));
        return resultados;
    }

    @Override
    public long countVigentesMoraVencidosByEntidad(Long codigoEntidad) throws Throwable {
        Query query = em.createQuery(
            "select count(p) from Prestamo p " +
            "where p.entidad.codigo = :codigoEntidad " +
            "  and p.idEstado in (2, 8, 11)"
        );
        query.setParameter("codigoEntidad", codigoEntidad);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public long countPrestamosConUltimaCuotaEnPeriodoByEntidad(Long codigoEntidad,
            java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
        // Cuenta préstamos cancelados (3) o cancelados anticipados (4) de la entidad
        // cuya última cuota (MAX numeroCuota) tenga fechaVencimiento >= fechaInicio del período
        Query query = em.createQuery(
            "select count(p) from Prestamo p " +
            "where p.entidad.codigo = :codigoEntidad " +
            "  and p.idEstado in (3, 4) " +
            "  and exists (" +
            "    select 1 from DetallePrestamo d " +
            "    where d.prestamo.codigo = p.codigo " +
            "      and d.numeroCuota = (" +
            "          select max(d2.numeroCuota) from DetallePrestamo d2 " +
            "          where d2.prestamo.codigo = p.codigo" +
            "      )" +
            "      and d.fechaVencimiento >= :fechaInicio " +
            "  )"
        );
        query.setParameter("codigoEntidad", codigoEntidad);
        query.setParameter("fechaInicio", fechaInicio);
        return ((Number) query.getSingleResult()).longValue();
    }

}
