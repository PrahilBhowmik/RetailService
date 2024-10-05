package com.example.RetailService;

import com.example.RetailService.configurations.TestAuthentication;
import com.example.RetailService.entity.Transaction;
import com.example.RetailService.entity.User;
import com.example.RetailService.errors.*;
import com.example.RetailService.repository.TransactionRepository;
import com.example.RetailService.repository.UserRepository;
import com.example.RetailService.testUtils.TestUtility;
import com.example.RetailService.utils.TransactionsStatus;
import com.example.RetailService.utils.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class RetailServiceApplicationTests {

	private static WebTestClient webTestClient;

	@Value("${auth.email}")
	String userEmail;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	IAuthenticationFacade authenticationFacade;

	private static SimpleDateFormat sdf;

	@BeforeAll
	public static void setUp(){
		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:8080/retail-service")
				.responseTimeout(Duration.ofSeconds(10L))
				.build();

		sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	}

	@AfterEach
	public void cleanUp(){
        userRepository.deleteAll().block();
        transactionRepository.deleteAll().block();
    }

	@BeforeEach
	public void resetUp(){
		authenticationFacade.setAuthentication(new TestAuthentication(userEmail));
	}

	@Test
	void addUserSuccessTest(){
		User user = new User(null,"userName1",null,TestUtility.generateProductsMap(5,"PA"));
		webTestClient.post()
				.uri("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(user)
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class)
				.value(user1 -> {
					assertEquals(user.getName(),user1.getName());
					assertEquals(user.getProducts(),user1.getProducts());
					assertEquals(userEmail,user1.getEmail());
					assertNotNull(user1.getId());
					assertEquals(user1,userRepository.findById(user1.getId()).block());
				});
	}

	@Test
	void addUserSuccessTestNullProducts(){
		User user = new User(null,"userName1",userEmail,null);
		webTestClient.post()
				.uri("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(user)
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class)
				.value(user1 -> {
					assertEquals(user.getName(),user1.getName());
					assertNotNull(user1.getProducts());
					assertEquals(0,user1.getProducts().size());
					assertEquals(userEmail,user1.getEmail());
					assertNotNull(user1.getId());
					assertEquals(user1,userRepository.findById(user1.getId()).block());
				});
	}

	@Test
	void getUserSuccessTest(){
		User user = new User(null,"userName1",userEmail,TestUtility.generateProductsMap(3,"PB"));
		user =userRepository.save(user).block();

		assert user!=null;

		webTestClient.get()
				.uri("/user")
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class)
				.isEqualTo(user);
	}

	@Test
	void getTransactionsSuccessTest(){
		User user1 = new User(null,"user1",userEmail,null);
		User user2 = new User(null,"user2","alternate@email.com",null);

		user1 = userRepository.save(user1).block();
		user2 = userRepository.save(user2).block();

		List<Transaction> transactions1 = new ArrayList<>();
		List<Transaction> transactions2 = new ArrayList<>();

        assert user1 != null;
		assert user2 != null;

        transactions1.add(new Transaction(null, TestUtility.generateProducts(5,"PA"), TransactionType.BUY,BigDecimal.ZERO,new Date(2323223232L),user1.getId()));
		transactions1.add(new Transaction(null, TestUtility.generateProducts(5,"PB"),TransactionType.SELL,BigDecimal.ONE,new Date(3323223232L),user1.getId()));
		transactions2.add(new Transaction(null, TestUtility.generateProducts(5,"PC"),TransactionType.BUY,BigDecimal.TWO,new Date(4323223232L),user2.getId()));
		transactions2.add(new Transaction(null, TestUtility.generateProducts(5,"PD"),TransactionType.SELL,BigDecimal.TEN,new Date(5323223232L),user2.getId()));
		transactions1.add(new Transaction(null, TestUtility.generateProducts(5,"PE"),TransactionType.BUY,BigDecimal.valueOf(7523),new Date(6323223232L),user1.getId()));
		
		transactions1=transactionRepository.saveAll(transactions1).collectList().block();
		transactions2=transactionRepository.saveAll(transactions2).collectList().block();

        assert transactions1 != null;
        transactions1.sort(Comparator.comparing(Transaction::getDate).reversed());

		assert transactions2 != null;
		transactions2.sort(Comparator.comparing(Transaction::getDate).reversed());

		List<Transaction> finalTransactions = transactions1;
		webTestClient.get()
				.uri("/transactions")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Transaction.class)
				.value(transactions -> {
					assertEquals(3,transactions.size());
                    assertArrayEquals(finalTransactions.toArray(),transactions.toArray());
				});

		authenticationFacade.setAuthentication(new TestAuthentication("alternate@email.com"));

		List<Transaction> finalTransactions1 = transactions2;
		webTestClient.get()
				.uri("/transactions")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Transaction.class)
				.value(transactions -> {
					assertEquals(2,transactions.size());
                    assertArrayEquals(finalTransactions1.toArray(),transactions.toArray());
				});
	}

	@Test
	void addTransactionBuySuccessTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		userProducts.remove("PA2");
		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = TestUtility.generateProducts(2,"PA");
        assert user != null;
        Transaction transaction = new Transaction(null,transactionProducts,TransactionType.BUY,null,new Date(),null);

		userProducts.put("PA2",transactionProducts[1]);
		Product product1 = userProducts.get("PA1");
		product1.setUnits(product1.getUnits()+transactionProducts[0].getUnits());

		User finalUser = user;
		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Transaction.class)
				.value(transaction1 -> {
					assertNotNull(transaction1.getId());
					BigDecimal amount = BigDecimal.ZERO;
					for(Product product: transactionProducts){
						amount=amount.add(product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
					}
					transaction.setTotal(amount);
					transaction.setId(transaction1.getId());
					transaction.setUserId(finalUser.getId());
					assertEquals(transaction,transaction1);
					assertEquals(transaction1,transactionRepository.findById(transaction1.getId()).block());
				});

		HashMap<String,Product> updatedProducts = Objects.requireNonNull(userRepository.findById(user.getId()).block()).getProducts();
		assertEquals(userProducts.size(),updatedProducts.size());
		assertEquals(userProducts,updatedProducts);

	}

	@Test
	void addTransactionSellSuccessTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.SELL,null,new Date(),user.getId());

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Transaction.class)
				.value(transaction1 -> {
					assertNotNull(transaction1.getId());
					BigDecimal amount = BigDecimal.ZERO;
					for(Product product: transactionProducts){
						amount=amount.add(product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits())));
					}
					transaction.setTotal(amount);
					transaction.setId(transaction1.getId());
					assertEquals(transaction,transaction1);
					assertEquals(transaction1,transactionRepository.findById(transaction1.getId()).block());
				});

		for (Product product: transactionProducts){
			Product userProduct=userProducts.get(product.getId());
			userProduct.setUnits(userProduct.getUnits()-product.getUnits());
			userProducts.put(userProduct.getId(),userProduct);
		}

		HashMap<String,Product> updatedProducts = Objects.requireNonNull(userRepository.findById(user.getId()).block()).getProducts();
		assertEquals(userProducts.size(),updatedProducts.size());
		assertEquals(userProducts,updatedProducts);
	}

	@Test
	void addTransactionReturnSellSuccessTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		userProducts.remove("PA2");
		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = TestUtility.generateProducts(2,"PA");
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.RETURN_SELL,null,new Date(),user.getId());

		userProducts.put("PA2",transactionProducts[1]);
		Product product1 = userProducts.get("PA1");
		product1.setUnits(product1.getUnits()+transactionProducts[0].getUnits());

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Transaction.class)
				.value(transaction1 -> {
					assertNotNull(transaction1.getId());
					BigDecimal amount = BigDecimal.ZERO;
					for(Product product: transactionProducts){
						amount=amount.add(product.getMrp().multiply(product.getDiscount().negate().add(BigDecimal.ONE)).multiply(BigDecimal.valueOf(product.getUnits())));
					}
					transaction.setTotal(amount);
					transaction.setId(transaction1.getId());
					assertEquals(transaction,transaction1);
					assertEquals(transaction1,transactionRepository.findById(transaction1.getId()).block());
				});

		HashMap<String,Product> updatedProducts = Objects.requireNonNull(userRepository.findById(user.getId()).block()).getProducts();
		assertEquals(userProducts.size(),updatedProducts.size());
		assertEquals(userProducts,updatedProducts);
	}

	@Test
	void addTransactionReturnBuySuccessTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.RETURN_BUY,null,new Date(),null);

		User finalUser = user;
		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Transaction.class)
				.value(transaction1 -> {
					assertNotNull(transaction1.getId());
					BigDecimal amount = BigDecimal.ZERO;
					for(Product product: transactionProducts){
						amount=amount.add(product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
					}
					transaction.setTotal(amount);
					transaction.setId(transaction1.getId());
					transaction.setUserId(finalUser.getId());
					assertEquals(transaction,transaction1);
					assertEquals(transaction1,transactionRepository.findById(transaction1.getId()).block());
				});

		for (Product product: transactionProducts){
			Product userProduct=userProducts.get(product.getId());
			userProduct.setUnits(userProduct.getUnits()-product.getUnits());
			userProducts.put(userProduct.getId(),userProduct);
		}

		HashMap<String,Product> updatedProducts = Objects.requireNonNull(userRepository.findById(user.getId()).block()).getProducts();
		assertEquals(userProducts.size(),updatedProducts.size());
		assertEquals(userProducts,updatedProducts);
	}

	@Test
	void addTransactionDisposeSuccessTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.DISPOSE,null,new Date(),"randomId");

		User finalUser = user;
		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Transaction.class)
				.value(transaction1 -> {
					assertNotNull(transaction1.getId());
					BigDecimal amount = BigDecimal.ZERO;
					for(Product product: transactionProducts){
						amount=amount.add(product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
					}
					transaction.setTotal(amount);
					transaction.setId(transaction1.getId());
					transaction.setUserId(finalUser.getId());
					assertEquals(transaction,transaction1);
					assertEquals(transaction1,transactionRepository.findById(transaction1.getId()).block());
				});

		for (Product product: transactionProducts){
			Product userProduct=userProducts.get(product.getId());
			userProduct.setUnits(userProduct.getUnits()-product.getUnits());
			userProducts.put(userProduct.getId(),userProduct);
		}

		HashMap<String,Product> updatedProducts = Objects.requireNonNull(userRepository.findById(user.getId()).block()).getProducts();
		assertEquals(userProducts.size(),updatedProducts.size());
		assertEquals(userProducts,updatedProducts);
	}

	@Test
	void setDiscountByIdSuccessTest(){
		HashMap<String,Product> products = TestUtility.generateProductsMap(5,"PA");
		User user = new User(null,"userName",userEmail,products);
		user = userRepository.save(user).block();

        assert user != null;
        user.getProducts().get("PA3").setDiscount(BigDecimal.valueOf(0.15));

		User finalUser = user;
		webTestClient.put()
				.uri("/discount/productId=PA3/discount=0.15")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Product.class)
				.value(product -> assertEquals(finalUser.getProducts().get("PA3"),product));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void setDiscountByCategorySuccessTest(){
		HashMap<String,Product> products = TestUtility.generateProductsWithFrequentCategory(25,"PA","target");
		User user = new User(null,"userName",userEmail,products);
		user = userRepository.save(user).block();

		assert user != null;
		List<Product> expectedProducts = user.getProducts().values()
				.stream()
				.filter(product -> "target".equalsIgnoreCase(product.getCategory()))
				.peek(product -> product.setDiscount(BigDecimal.valueOf(0.15)))
				.toList();

		webTestClient.put()
				.uri("/discount/category=target/discount=0.15")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Product.class)
				.value(products1 -> assertEquals(expectedProducts,products1));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void setDiscountByBrandSuccessTest(){
		HashMap<String,Product> products = TestUtility.generateProductsWithFrequentBrand(25,"PA","target");
		User user = new User(null,"userName",userEmail,products);
		user = userRepository.save(user).block();

		assert user != null;
		List<Product> expectedProducts = user.getProducts().values()
				.stream()
				.filter(product -> "target".equalsIgnoreCase(product.getBrand()))
				.peek(product -> product.setDiscount(BigDecimal.valueOf(0.25)))
				.toList();

		webTestClient.put()
				.uri("/discount/brand=target/discount=0.25")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Product.class)
				.value(products1 -> assertEquals(expectedProducts,products1));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void generateReportSuccessTest() {
		User user1 = new User(null,"user1",userEmail,null);
		User user2 = new User(null,"user2","alternate@email.com",null);

		user1 = userRepository.save(user1).block();
		user2 = userRepository.save(user2).block();

		Transaction[] transactions1P,transactions1L,transactions1N,transactions2;
        assert user1 != null;
        transactions1N = TestUtility.generateTransactions(7,user1.getId(),100000000L,200000000L, TransactionsStatus.NONE);
		transactions1P = TestUtility.generateTransactions(6,user1.getId(),200000000L,300000000L, TransactionsStatus.PROFIT);
		transactions1L = TestUtility.generateTransactions(9,user1.getId(),300000000L,400000000L, TransactionsStatus.LOSS);
        assert user2 != null;
        transactions2 = TestUtility.generateTransactions(8,user2.getId(),100000000L,400000000L, TransactionsStatus.PROFIT);
		
		transactions1L = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions1L).toList()).collectList().block()).toArray(new Transaction[0]);
		transactions1N = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions1N).toList()).collectList().block()).toArray(new Transaction[0]);
		transactions1P = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions1P).toList()).collectList().block()).toArray(new Transaction[0]);
		transactions2 = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions2).toList()).collectList().block()).toArray(new Transaction[0]);

		Report reportN = TestUtility.analyseTransactions(transactions1N);
		reportN.setUserId(user1.getId());
		reportN.setStatus(TransactionsStatus.NONE);
		reportN.setFromDate(new Date(100000000L));
		reportN.setToDate(new Date(200000000L));

		webTestClient.get()
				.uri("/report/from="+sdf.format(new Date(100000000L))+"/to="+sdf.format(new Date(200000000L)))
				.exchange()
				.expectStatus().isOk()
				.expectBody(Report.class)
				.isEqualTo(reportN);


		Report reportP = TestUtility.analyseTransactions(transactions1P);
		reportP.setUserId(user1.getId());
		reportP.setStatus(TransactionsStatus.PROFIT);
		reportP.setFromDate(new Date(200000000L));
		reportP.setToDate(new Date(300000000L));

		webTestClient.get()
				.uri("/report/from="+sdf.format(new Date(200000000L))+"/to="+sdf.format(new Date(300000000L)))
				.exchange()
				.expectStatus().isOk()
				.expectBody(Report.class)
				.isEqualTo(reportP);

		Report reportL = TestUtility.analyseTransactions(transactions1L);
		reportL.setUserId(user1.getId());
		reportL.setStatus(TransactionsStatus.LOSS);
		reportL.setFromDate(new Date(300000000L));
		reportL.setToDate(new Date(400000000L));

		webTestClient.get()
				.uri("/report/from="+sdf.format(new Date(300000000L))+"/to="+sdf.format(new Date(400000000L)))
				.exchange()
				.expectStatus().isOk()
				.expectBody(Report.class)
				.isEqualTo(reportL);
	}

	@Test
	void addUserFailureTest(){
		User user = new User(null,"user1",userEmail,null);
		user = userRepository.save(user).block();

		User anotherUser = new User(null,"user2",null,TestUtility.generateProductsMap(5,"PA"));

		webTestClient.post()
				.uri("/user")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(anotherUser)
				.exchange()
				.expectStatus().is4xxClientError()
				.expectBody(UserAlreadyExistsException.class)
				.value(e -> assertEquals("User with this email already exists",e.getMessage()));

	}

	@Test
	void getUserFailureTest(){
		webTestClient.get()
				.uri("/user")
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(UserNotFoundException.class)
				.value(e -> assertEquals("User not found",e.getMessage()));
	}

    @Test
    void getTransactionsForInvalidUserFailureTest(){
        webTestClient.get()
                .uri("/transactions")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(UserNotFoundException.class)
                .value(e -> assertEquals("User not found",e.getMessage()));
    }

	@Test
	void getTransactionsForNoTransactionsTest(){
		User user = new User(null,"userName",userEmail,TestUtility.generateProductsMap(1,"PA"));
		user=userRepository.save(user).block();
        assert user != null;
        webTestClient.get()
				.uri("/transactions")
				.exchange()
				.expectStatus().isNoContent();
	}

	@Test
	void addInvalidTransactionTypeFailureTest() throws IOException {
		User user = new User(null,"name",userEmail,null);
		user = userRepository.save(user).block();

        assert user != null;
        Transaction transaction = new Transaction(null,TestUtility.generateProducts(2,"PB"),null,BigDecimal.valueOf(100),new Date(),user.getId());

		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(transaction);
		jsonString = jsonString.replace("\"type\":"+transaction.getType(), "\"type\":\"invalid\"");

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(jsonString)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(InvalidTransactionException.class)
				.value(e-> assertEquals("Transaction type is not valid",e.getMessage()));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void addInvalidSellUnitsFailureTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);
		transactionProducts[1].setUnits(transactionProducts[1].getUnits()*10);
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.SELL,null,new Date(),user.getId());

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(NotEnoughProductsException.class)
				.value(e -> assertEquals("Not enough/No products to sell/return",e.getMessage()));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void addTransactionsSellInvalidProductsTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);

		userProducts.remove("PA1");

		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.SELL,null,new Date(),user.getId());

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(NotEnoughProductsException.class)
				.value(e -> assertEquals("Not enough/No products to sell/return",e.getMessage()));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void addInvalidReturnBuyOrDisposeUnitsFailureTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);
		transactionProducts[1].setUnits(transactionProducts[1].getUnits()*10);
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.RETURN_BUY,null,new Date(),user.getId());

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(NotEnoughProductsException.class)
				.value(e -> assertEquals("Not enough/No products to sell/return",e.getMessage()));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void addTransactionsReturnBuyOrDisposeInvalidProductsTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);

		userProducts.remove("PA1");

		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.DISPOSE,null,new Date(),user.getId());

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(NotEnoughProductsException.class)
				.value(e -> assertEquals("Not enough/No products to sell/return",e.getMessage()));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void addNegativeTransaction(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp().negate(),product.getCost(),
						product.getDiscount(),-product.getUnits(),product.getBrand()))
				.toList().toArray(new Product[0]);

		User user = new User(null,"userName1",userEmail,userProducts);
		user = userRepository.save(user).block();
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.DISPOSE,null,new Date(),user.getId());

		webTestClient.post()
				.uri("/transactions")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(transaction)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(NotEnoughProductsException.class)
				.value(e -> assertEquals("Not enough/No products to sell/return",e.getMessage()));

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void generateReportInvalidDateTest(){
		User user = new User(null,"userName1",userEmail,TestUtility.generateProductsMap(3,"PB"));
		user =userRepository.save(user).block();

		webTestClient.get()
				.uri("/report/from="+sdf.format(new Date(200000000L))+"/to="+sdf.format(new Date(100000000L)))
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(InvalidDatesException.class);
	}

	@Test
	void generateReportNoTransactionsTest(){
		User user = new User(null,"userName1",userEmail,TestUtility.generateProductsMap(3,"PB"));
		user =userRepository.save(user).block();

		webTestClient.get()
				.uri("/report/from="+sdf.format(new Date(100000000L))+"/to="+sdf.format(new Date(200000000L)))
				.exchange()
				.expectStatus().isNoContent();
	}
	
}
