package guru.sfg.beer.order.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import guru.sfg.beer.order.service.RESTclient.beerService.BeerServiceRestTemplateImpl;
import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableWireMock(
        @ConfigureWireMock(name = "beer-service", port = 8084)
)
@SpringBootTest
public class BeerOrderMangerImplIT {
    @Autowired
    BeerOrderRepository beerOrderRepository;
    @Autowired
    BeerOrderManager beerOrderManager;
    @Autowired
    CustomerRepository customerRepository;
    @InjectWireMock("beer-service")
    WireMockServer wireMockServer;
    @Autowired
    ObjectMapper objectMapper;
    Customer customer;
    UUID beerId = UUID.randomUUID();


    @BeforeEach
    public void setUp(){
        customer = Customer.builder()
                .customerName("Test Customer")
                .build();
        customerRepository.save(customer);
    }
    @Test
    public void testNewOrderToAllocate() throws JsonProcessingException, InterruptedException {
        BeerDTO beerDTO = BeerDTO.builder()
                .id(beerId)
                .upc("1234")
                .build();
        wireMockServer.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_SERVICE_GET_BY_UPC_PATH + beerDTO.getUpc())
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDTO)))
        );

        BeerOrder allocatedBeerOrder = beerOrderManager.newBeerOrder(createBeerOrder());

        await().timeout(10, TimeUnit.SECONDS).until(() -> {
            BeerOrder beerOrderFound = beerOrderRepository.findById(allocatedBeerOrder.getId()).get();
            return beerOrderFound.getOrderState() == OrderState.PENDING_ALLOCATION;
        });

        assertNotNull(allocatedBeerOrder);
        assertEquals(OrderState.ALLOCATED, allocatedBeerOrder.getOrderState());

    }

    private BeerOrder createBeerOrder(){
       BeerOrder beerOrder =  BeerOrder.builder()
                .customer(customer)
                .build();
        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add( BeerOrderLine.builder()
                .beerId(beerId)
                .upc("1234")
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());
        beerOrder.setBeerOrderLines(lines);
        return beerOrder;
    }
}
