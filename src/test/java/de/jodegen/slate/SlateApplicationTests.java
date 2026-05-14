package de.jodegen.slate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=dGVzdC1zZWNyZXQtdmFsdWUtdGhhdC1pcy1hdC1sZWFzdC0yNTYtYml0cy1sb25nLWhtYWM=",
        "cors.allowed-origins=http://localhost:3000"
})
class SlateApplicationTests {

	@Test
	void contextLoads() {
	}

}
