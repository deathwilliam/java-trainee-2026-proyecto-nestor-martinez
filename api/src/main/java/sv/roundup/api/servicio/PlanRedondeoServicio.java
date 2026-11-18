package sv.roundup.api.servicio;

import org.springframework.stereotype.Service;
import sv.roundup.api.entidad.PlanRedondeo;
import sv.roundup.api.repositorio.PlanRedondeoRepositorio;

import java.util.List;

@Service
public class PlanRedondeoServicio {
    private final PlanRedondeoRepositorio planRedondeoRepositorio;

    public PlanRedondeoServicio(PlanRedondeoRepositorio planRedondeoRepositorio){
        this.planRedondeoRepositorio = planRedondeoRepositorio;
    }

    public List<PlanRedondeo> listar(){return planRedondeoRepositorio.findAll();}
}
