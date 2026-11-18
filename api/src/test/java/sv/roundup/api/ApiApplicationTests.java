package sv.roundup.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.clave=clave-solo-para-las-pruebas-automaticas-de-roundup")
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
