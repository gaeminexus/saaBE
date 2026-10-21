package com.saa.ejb.crd.daoImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.UsuarioAppDaoService;
import com.saa.model.crd.UsuarioApp;
import com.saa.rubros.EstadoUsuarioApp;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class UsuarioAppDaoServiceImpl extends EntityDaoImpl<UsuarioApp>
        implements UsuarioAppDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) UsuarioApp");
        return new String[]{
            "codigo",
            "entidad",
            "identificacion",
            "claveHash",
            "estado",
            "intentosFallidos",
            "bloqueadoHasta",
            "debeCambiarClave",
            "fechaCreacion",
            "fechaUltimoAcceso",
            "usuarioRegistro"
        };
    }

    @Override
    public UsuarioApp selectByIdentificacion(String identificacion) throws Throwable {
        System.out.println("Ingresa al metodo selectByIdentificacion de UsuarioApp - identificacion: "
                + identificacion);
        Query query = em.createQuery(
                " select u from UsuarioApp u " +
                " where  u.identificacion = :identificacion");
        query.setParameter("identificacion", identificacion);
        List<UsuarioApp> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public UsuarioApp selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de UsuarioApp - idEntidad: " + idEntidad);
        Query query = em.createQuery(
                " select u from UsuarioApp u " +
                " where  u.entidad.codigo = :idEntidad");
        query.setParameter("idEntidad", idEntidad);
        List<UsuarioApp> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public UsuarioApp registrarIntentoFallido(Long codigo, int maxIntentos, LocalDateTime ahora,
            long minutosBloqueo) throws Throwable {
        System.out.println("Ingresa al metodo registrarIntentoFallido de UsuarioApp - codigo: " + codigo);
        UsuarioApp usap = em.find(UsuarioApp.class, codigo, LockModeType.PESSIMISTIC_WRITE);
        if (usap == null) {
            return null;
        }

        Long estado = usap.getEstado();
        if (estado != null && estado.intValue() == EstadoUsuarioApp.BLOQUEADO
                && usap.getBloqueadoHasta() != null && ahora.isAfter(usap.getBloqueadoHasta())) {
            // El bloqueo anterior ya venció: se parte de cero antes de sumar este intento.
            usap.setEstado(Long.valueOf(EstadoUsuarioApp.ACTIVO));
            usap.setIntentosFallidos(0L);
            usap.setBloqueadoHasta(null);
        }

        long intentos = (usap.getIntentosFallidos() != null ? usap.getIntentosFallidos() : 0L) + 1;
        usap.setIntentosFallidos(intentos);
        if (intentos >= maxIntentos) {
            usap.setEstado(Long.valueOf(EstadoUsuarioApp.BLOQUEADO));
            usap.setBloqueadoHasta(ahora.plusMinutes(minutosBloqueo));
        }
        return usap;
    }
}
