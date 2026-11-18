package sv.roundup.api.servicio;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.roundup.api.dto.TokenRespuesta;
import sv.roundup.api.entidad.Usuario;
import sv.roundup.api.repositorio.UsuarioRepositorio;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AutenticacionServicio {
    private static final Duration VIGENCIA = Duration.ofHours(1);

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AutenticacionServicio(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional(readOnly = true)
    public TokenRespuesta login(String username, String clave){
        Usuario usuario = usuarioRepositorio.findByUsername(username)
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(clave, u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Usuario o clave incorrectos"));

        List<String> roles = usuarioRepositorio.rolesDe(usuario.getId());

        Instant ahora = Instant.now();
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("roundup")
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(VIGENCIA))
                .claim("roles", roles)
                .build();

        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(encabezado, datos)).getTokenValue();

        return new TokenRespuesta(token, "Bearer", VIGENCIA.toSeconds());
    }
}
