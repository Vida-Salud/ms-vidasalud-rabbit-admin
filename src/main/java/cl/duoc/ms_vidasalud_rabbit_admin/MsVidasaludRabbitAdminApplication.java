package cl.duoc.ms_vidasalud_rabbit_admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsVidasaludRabbitAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsVidasaludRabbitAdminApplication.class, args);
	}

}
