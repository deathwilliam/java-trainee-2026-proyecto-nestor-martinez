package sv.roundup.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.roundup.api.entidad.Comercio;
import sv.roundup.api.excepcion.OperacionRechazadaException;
import sv.roundup.api.repositorio.ComercioRepositorio;

import java.util.List;
import java.util.Optional;

@Service
public class ComercioServicio {
    private final ComercioRepositorio comercioRepositorio;

    public ComercioServicio(ComercioRepositorio comercioRepositorio){
        this.comercioRepositorio = comercioRepositorio;
    }

    public List<Comercio> listar(){return comercioRepositorio.findAll();}

    public Optional<Comercio> porCodigo(String codigo){return comercioRepositorio.findByCodigo(codigo);}

    @Transactional
    public Comercio crear(String codigo, String nombre){
        if (comercioRepositorio.existsByCodigo(codigo)){
            throw new OperacionRechazadaException("Ya existe un comercio con el codigo " + codigo);
        }

        return comercioRepositorio.save(new Comercio(codigo, nombre));
    }

    @Transactional
    public Optional<Comercio> editar(String codigo, String nombre){
        Optional<Comercio> comercio = comercioRepositorio.findByCodigo(codigo);
        comercio.ifPresent(c -> c.setNombre(nombre));
        return comercio;
    }

    @Transactional
    public boolean desactivar(String codigo){
        Optional<Comercio> comercio = comercioRepositorio.findByCodigo(codigo);
        comercio.ifPresent(c -> c.setActivo(false));
        return comercio.isPresent();
    }
}
