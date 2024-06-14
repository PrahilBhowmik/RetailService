package com.example.RetailService;

import com.example.RetailService.entity.User;
import com.example.RetailService.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class RetailServiceApplicationTests {

	private static WebTestClient webTestClient;

	@Autowired
	private UserRepository userRepository;

	@BeforeAll
	public static void setUp(){
		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:8082/retail-service")
				.build();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void addUserSuccessTest(){
		User user = new User(null,"userName1",null);
		webTestClient.post()
				.uri("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(user)
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class)
				.value(user1 -> {
					assertEquals(user.getName(),user1.getName());
					assertNotNull(user1.getId());
				});
	}

	@Test
	void getUserSuccessTest(){
		User user = new User(null,"userName1",null);
		user = Objects.requireNonNull(userRepository.save(user).block());

		webTestClient.get()
				.uri("/user/"+user.getId())
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class)
				.isEqualTo(user);
	}

}
