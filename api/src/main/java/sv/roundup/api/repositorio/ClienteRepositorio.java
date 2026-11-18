package sv.roundup.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import sv.roundup.api.entidad.Cliente;

import java.util.Optional;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByDui(String dui);
}
