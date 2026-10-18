package com.rentaherramientas.domain.model.aggregates;

import com.rentaherramientas.domain.exceptions.ReglaDeNegocioException;
import com.rentaherramientas.domain.model.valueobject.Dinero;
import com.rentaherramientas.domain.model.valueobject.EstadoIncidencia;
import com.rentaherramientas.domain.model.valueobject.SeveridadDano;

/**
 * Daño encontrado en una herramienta al recibir la devolución. El PROVEEDOR lo reporta y el ADMIN
 * lo gestiona como incidencia (ABIERTO → EN_REVISION → RESUELTO).
 */
public class ReporteDano {

    private final Long id;
    private final Long devolucionId;
    private final Long herramientaId;
    private final String descripcion;
    private final SeveridadDano severidad;
    private final Dinero costoEstimado;
    private EstadoIncidencia estado;
    private final Long reportadoPor;

    private ReporteDano(Long id, Long devolucionId, Long herramientaId, String descripcion, SeveridadDano severidad,
                        Dinero costoEstimado, EstadoIncidencia estado, Long reportadoPor) {
        if (devolucionId == null || herramientaId == null || severidad == null || estado == null || reportadoPor == null) {
            throw new ReglaDeNegocioException("El reporte requiere devolución, herramienta, severidad y quién lo reporta");
        }
        this.id = id;
        this.devolucionId = devolucionId;
        this.herramientaId = herramientaId;
        this.descripcion = ReglaDeNegocioException.requerido(descripcion, "descripcion");
        this.severidad = severidad;
        this.costoEstimado = costoEstimado == null ? Dinero.CERO : costoEstimado;
        this.estado = estado;
        this.reportadoPor = reportadoPor;
    }

    /**
     * Reporta un daño. Reglas: la devolución no puede haberse recibido en buen estado y la herramienta
     * tiene que ser una de las que se alquilaron en esa reserva.
     */
    public static ReporteDano reportar(Devolucion devolucion, Reserva reserva, Long herramientaId, String descripcion,
                                       SeveridadDano severidad, Dinero costoEstimado, Long usuarioId) {
        if (!devolucion.admiteReporteDeDano()) {
            throw new ReglaDeNegocioException("La devolución se registró con el equipo en buen estado; "
                    + "no se pueden reportar daños");
        }
        boolean herramientaAlquilada = reserva.getDetalles().stream()
                .anyMatch(d -> d.getHerramientaId().equals(herramientaId));
        if (!herramientaAlquilada) {
            throw new ReglaDeNegocioException("La herramienta " + herramientaId + " no hace parte de la reserva "
                    + reserva.getCodigo());
        }
        return new ReporteDano(null, devolucion.getId(), herramientaId, descripcion, severidad, costoEstimado,
                EstadoIncidencia.ABIERTO, usuarioId);
    }

    public static ReporteDano reconstruir(Long id, Long devolucionId, Long herramientaId, String descripcion,
                                          SeveridadDano severidad, Dinero costoEstimado, EstadoIncidencia estado,
                                          Long reportadoPor) {
        return new ReporteDano(id, devolucionId, herramientaId, descripcion, severidad, costoEstimado, estado,
                reportadoPor);
    }

    /** El ADMIN avanza la incidencia un paso. */
    public void avanzarA(EstadoIncidencia siguiente) {
        if (!estado.puedePasarA(siguiente)) {
            throw new ReglaDeNegocioException("La incidencia está " + estado + " y no puede pasar a " + siguiente
                    + " (el orden es ABIERTO → EN_REVISION → RESUELTO)");
        }
        this.estado = siguiente;
    }

    public boolean sacaDeServicio() {
        return severidad.sacaDeServicio();
    }

    public boolean estaResuelto() {
        return estado == EstadoIncidencia.RESUELTO;
    }

    public Long getId() { return id; }
    public Long getDevolucionId() { return devolucionId; }
    public Long getHerramientaId() { return herramientaId; }
    public String getDescripcion() { return descripcion; }
    public SeveridadDano getSeveridad() { return severidad; }
    public Dinero getCostoEstimado() { return costoEstimado; }
    public EstadoIncidencia getEstado() { return estado; }
    public Long getReportadoPor() { return reportadoPor; }
}
