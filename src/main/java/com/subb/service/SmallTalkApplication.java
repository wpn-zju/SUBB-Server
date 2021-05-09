package com.subb.service;

import com.subb.service.storage.StorageProperties;
import com.subb.service.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@RestController
@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class SmallTalkApplication {
	private static final Logger logger = LoggerFactory.getLogger(SmallTalkApplication.class);

	public static void main(String[] args) throws Exception {
		//Object obj = null;
		//JsonObject jobj = new JsonObject(new LinkedHashMap<>());
		//jobj.put("ere", new JsonObject(4));
		//jobj.put("rer", new JsonObject(new ArrayList<>()));
		//jobj.get("rer").add(new JsonObject("ewewq"));
		//jobj.get("rer").add(new JsonObject(true));
		//obj = jobj;
		//System.out.println(jobj.toString());
		//System.out.println(new ObjectMapper().writeValueAsString(obj));
		//JsonObject t = new ObjectMapper().readValue(new ObjectMapper().writeValueAsString(obj), JsonObject.class);
		//System.out.println(t.toString());
		SpringApplication.run(SmallTalkApplication.class, args);
	}

	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return args -> storageService.init();
	}
}
