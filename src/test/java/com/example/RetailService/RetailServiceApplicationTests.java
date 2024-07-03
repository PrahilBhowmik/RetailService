package com.example.RetailService;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class RetailServiceApplicationTests {

	private static WebTestClient webTestClient;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@BeforeAll
	public static void setUp(){
		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://localhost:8082/retail-service")
				.build();
	}

	@AfterEach
	public void cleanUp(){
        userRepository.deleteAll().block();
        transactionRepository.deleteAll().block();
    }

	@Test
	void addUserSuccessTest(){
		User user = new User(null,"userName1",TestUtility.generateProductsMap(5,"PA"));
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
					assertNotNull(user1.getId());
					assertEquals(user1,userRepository.findById(user1.getId()).block());
				});
	}

	@Test
	void addUserSuccessTestNullProducts(){
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
					assertNotNull(user1.getProducts());
					assertEquals(0,user1.getProducts().size());
					assertNotNull(user1.getId());
					assertEquals(user1,userRepository.findById(user1.getId()).block());
				});
	}

	@Test
	void getUserSuccessTest(){
		User user = new User(null,"userName1",TestUtility.generateProductsMap(3,"PB"));
		user = Objects.requireNonNull(userRepository.save(user).block());

		webTestClient.get()
				.uri("/user/"+user.getId())
				.exchange()
				.expectStatus().isOk()
				.expectBody(User.class)
				.isEqualTo(user);
	}

	@Test
	void getTransactionsSuccessTest(){
		List<Transaction> transactions1 = new ArrayList<>();
		List<Transaction> transactions2 = new ArrayList<>();

		transactions1.add(new Transaction(null, TestUtility.generateProducts(5,"PA"), TransactionType.BUY,BigDecimal.ZERO,new Date(2323223232L),"userId1"));
		transactions1.add(new Transaction(null, TestUtility.generateProducts(5,"PB"),TransactionType.SELL,BigDecimal.ONE,new Date(3323223232L),"userId1"));
		transactions2.add(new Transaction(null, TestUtility.generateProducts(5,"PC"),TransactionType.BUY,BigDecimal.TWO,new Date(4323223232L),"userId2"));
		transactions2.add(new Transaction(null, TestUtility.generateProducts(5,"PD"),TransactionType.SELL,BigDecimal.TEN,new Date(5323223232L),"userId2"));
		transactions1.add(new Transaction(null, TestUtility.generateProducts(5,"PE"),TransactionType.BUY,BigDecimal.valueOf(7523),new Date(6323223232L),"userId1"));
		
		transactions1=transactionRepository.saveAll(transactions1).collectList().block();
		transactions2=transactionRepository.saveAll(transactions2).collectList().block();

        assert transactions1 != null;
        transactions1.sort(Comparator.comparing(Transaction::getDate).reversed());

		assert transactions2 != null;
		transactions2.sort(Comparator.comparing(Transaction::getDate).reversed());

		List<Transaction> finalTransactions = transactions1;
		webTestClient.get()
				.uri("/transactions/userId1")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Transaction.class)
				.value(transactions -> {
					assertEquals(3,transactions.size());
                    assertArrayEquals(finalTransactions.toArray(),transactions.toArray());
				});

		List<Transaction> finalTransactions1 = transactions2;
		webTestClient.get()
				.uri("/transactions/userId2")
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
		User user = new User(null,"username1",userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = TestUtility.generateProducts(2,"PA");
        assert user != null;
        Transaction transaction = new Transaction(null,transactionProducts,TransactionType.BUY,null,new Date(),user.getId());

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
						amount=amount.add(product.getCost().multiply(BigDecimal.valueOf(product.getUnits())));
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
	void addTransactionSellSuccessTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"username1",userProducts);
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
		User user = new User(null,"username1",userProducts);
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
		User user = new User(null,"username1",userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.RETURN_BUY,null,new Date(),user.getId());

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
		User user = new User(null,"username1",userProducts);
		user = userRepository.save(user).block();

		Product[] transactionProducts = userProducts.values().stream()
				.map(product -> new Product(product.getId(),product.getName(),
						product.getCategory(), product.getMrp(),product.getCost(),
						product.getDiscount(),product.getUnits()/2,product.getBrand()))
				.toList().toArray(new Product[0]);
		assert user != null;
		Transaction transaction = new Transaction(null,transactionProducts,TransactionType.DISPOSE,null,new Date(),user.getId());

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
		User user = new User(null,"userName",products);
		user = userRepository.save(user).block();

        assert user != null;
        user.getProducts().get("PA3").setDiscount(BigDecimal.valueOf(0.15));

		User finalUser = user;
		webTestClient.put()
				.uri("/user/"+user.getId()+"/productId=PA3/discount=0.15")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Product.class)
				.value(product -> {
					assertEquals(finalUser.getProducts().get("PA3"),product);
				});

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void setDiscountByCategorySuccessTest(){
		HashMap<String,Product> products = TestUtility.generateProductsWithFrequentCategory(25,"PA","target");
		User user = new User(null,"userName",products);
		user = userRepository.save(user).block();

		assert user != null;
		List<Product> expectedProducts = user.getProducts().values()
				.stream()
				.filter(product -> "target".equalsIgnoreCase(product.getCategory()))
				.peek(product -> product.setDiscount(BigDecimal.valueOf(0.15)))
				.toList();

		webTestClient.put()
				.uri("/user/"+user.getId()+"/category=target/discount=0.15")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Product.class)
				.value(products1 -> {
					assertEquals(expectedProducts,products1);
				});

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void setDiscountByBrandSuccessTest(){
		HashMap<String,Product> products = TestUtility.generateProductsWithFrequentBrand(25,"PA","target");
		User user = new User(null,"userName",products);
		user = userRepository.save(user).block();

		assert user != null;
		List<Product> expectedProducts = user.getProducts().values()
				.stream()
				.filter(product -> "target".equalsIgnoreCase(product.getBrand()))
				.peek(product -> product.setDiscount(BigDecimal.valueOf(0.25)))
				.toList();

		webTestClient.put()
				.uri("/user/"+user.getId()+"/brand=target/discount=0.25")
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Product.class)
				.value(products1 -> {
					assertEquals(expectedProducts,products1);
				});

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void generateReportSuccessTest() {
		Transaction[] transactions1P,transactions1L,transactions1N,transactions2;
		transactions1N = TestUtility.generateTransactions(7,"userId1",100000000L,200000000L, TransactionsStatus.NONE);
		transactions1P = TestUtility.generateTransactions(6,"userId1",200000000L,300000000L, TransactionsStatus.PROFIT);
		transactions1L = TestUtility.generateTransactions(9,"userId1",300000000L,400000000L, TransactionsStatus.LOSS);
		transactions2 = TestUtility.generateTransactions(8,"userId2",100000000L,400000000L, TransactionsStatus.PROFIT);
		
		transactions1L = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions1L).toList()).collectList().block()).toArray(new Transaction[0]);
		transactions1N = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions1N).toList()).collectList().block()).toArray(new Transaction[0]);
		transactions1P = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions1P).toList()).collectList().block()).toArray(new Transaction[0]);
		transactions2 = Objects.requireNonNull(transactionRepository.saveAll(Arrays.stream(transactions2).toList()).collectList().block()).toArray(new Transaction[0]);

		Report reportN = TestUtility.analyseTransactions(transactions1N);
		reportN.setUserId("userId1");
		reportN.setStatus(TransactionsStatus.NONE);
		reportN.setFromDate(new Date(100000000L));
		reportN.setToDate(new Date(200000000L));

		webTestClient.get()
				.uri("/report/userId1/from="+100000000L+"/to="+200000000L)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Report.class)
				.isEqualTo(reportN);


		Report reportP = TestUtility.analyseTransactions(transactions1P);
		reportP.setUserId("userId1");
		reportP.setStatus(TransactionsStatus.PROFIT);
		reportP.setFromDate(new Date(200000000L));
		reportP.setToDate(new Date(300000000L));

		webTestClient.get()
				.uri("/report/userId1/from="+200000000L+"/to="+300000000L)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Report.class)
				.isEqualTo(reportP);

		Report reportL = TestUtility.analyseTransactions(transactions1L);
		reportL.setUserId("userId1");
		reportL.setStatus(TransactionsStatus.LOSS);
		reportL.setFromDate(new Date(300000000L));
		reportL.setToDate(new Date(400000000L));

		webTestClient.get()
				.uri("/report/userId1/from="+300000000L+"/to="+400000000L)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Report.class)
				.isEqualTo(reportL);
	}

	@Test
	void getUserFailureTest(){
		webTestClient.get()
				.uri("/user/invalid")
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(UserNotFoundException.class)
				.value(e -> {
					assertEquals("User not found",e.getMessage());
				});
	}

    @Test
    void getTransactionsForInvalidUserFailureTest(){
        webTestClient.get()
                .uri("/transactions/invalid")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(UserNotFoundException.class)
                .value(e -> {
                    assertEquals("User not found",e.getMessage());
                });
    }

	@Test
	void getTransactionsForNoTransactionsTest(){
		User user = new User(null,"username",TestUtility.generateProductsMap(1,"PA"));
		user=userRepository.save(user).block();
        assert user != null;
        webTestClient.get()
				.uri("/transactions/"+user.getId())
				.exchange()
				.expectStatus().isNoContent();
	}

	@Test
	void addInvalidTransactionTypeFailureTest() throws IOException {
		User user = new User(null,"name",null);
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
				.value(e->{
					assertEquals("Transaction type is not valid",e.getMessage());
				});

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void addInvalidSellUnitsFailureTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"username1",userProducts);
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
				.value(e -> {
					assertEquals("Not enough/No products to sell/return",e.getMessage());
				});

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

		User user = new User(null,"username1",userProducts);
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
				.value(e -> {
					assertEquals("Not enough/No products to sell/return",e.getMessage());
				});

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}

	@Test
	void addInvalidReturnBuyOrDisposeUnitsFailureTest(){
		HashMap<String,Product> userProducts = TestUtility.generateProductsMap(4,"PA");
		User user = new User(null,"username1",userProducts);
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
				.value(e -> {
					assertEquals("Not enough/No products to sell/return",e.getMessage());
				});

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

		User user = new User(null,"username1",userProducts);
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
				.value(e -> {
					assertEquals("Not enough/No products to sell/return",e.getMessage());
				});

		User updatedUser = userRepository.findById(user.getId()).block();
		assertEquals(user,updatedUser);
	}


}
