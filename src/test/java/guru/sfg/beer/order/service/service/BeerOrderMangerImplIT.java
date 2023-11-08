package guru.sfg.beer.order.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import common.event.FailedAllocationCompenReqEvent;
import guru.sfg.beer.order.service.RESTclient.beerService.BeerServiceRestTemplateImpl;
import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;
import guru.sfg.beer.order.service.config.JmsConfig;
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
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired
    JmsTemplate jmsClient;
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

        await().untilAsserted(() -> {
            BeerOrder beerOrderFound = beerOrderRepository.findById(allocatedBeerOrder.getId())
                    .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
            assertEquals(OrderState.ALLOCATED, beerOrderFound.getOrderState());
        });

        await().untilAsserted(()->{
            BeerOrder beerOrderFound = beerOrderRepository.findById(allocatedBeerOrder.getId())
                    .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
            beerOrderFound.getBeerOrderLines().forEach((line) ->{
                assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
            });
        });

        assertNotNull(allocatedBeerOrder);
        assertEquals(OrderState.ALLOCATED, allocatedBeerOrder.getOrderState());
        allocatedBeerOrder.getBeerOrderLines().forEach(line -> {
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

    }

    @Test
    public void testAllocatedToPickedUp() throws JsonProcessingException {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderState(OrderState.ALLOCATED);

        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);

        BeerOrder pickedUpOrder = beerOrderManager.pickUpBeerOrder(savedBeerOrder.getId());

//        await().untilAsserted(() ->{
//            assertNotNull(pickedUpOrder);
//            assertEquals(pickedUpOrder.getOrderState(), OrderState.PICKED_UP);
//        });

        assertNotNull(pickedUpOrder);
        assertEquals(pickedUpOrder.getOrderState(), OrderState.PICKED_UP);

    }

    @Test
    public void testValidationFailed() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .id(beerId)
                .upc("1234")
                .build();
        wireMockServer.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_SERVICE_GET_BY_UPC_PATH + beerDTO.getUpc())
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDTO)))
        );

        BeerOrder newBeerOrder = createBeerOrder();
        newBeerOrder.setCustomerRef("test-failed-validation"); // flag MQ that should fail

        BeerOrder failedValidationOrder = beerOrderManager.newBeerOrder(newBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder beerOrderFound = beerOrderRepository.findById(failedValidationOrder.getId())
                    .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
            assertEquals(OrderState.VALIDATION_EXCEPTION, beerOrderFound.getOrderState());
        });

        assertNotNull(failedValidationOrder);
        assertEquals(OrderState.VALIDATION_EXCEPTION, failedValidationOrder.getOrderState());
    }

    @Test
    public void testFailedAllocation() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .id(beerId)
                .upc("1234")
                .build();
        wireMockServer.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_SERVICE_GET_BY_UPC_PATH + beerDTO.getUpc())
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDTO)))
        );

        BeerOrder newBeerOrder = createBeerOrder();
        newBeerOrder.setCustomerRef("test-failed-allocation"); // flag MQ that should fail

        BeerOrder failedAllocationOrder = beerOrderManager.newBeerOrder(newBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder beerOrderFound = beerOrderRepository.findById(failedAllocationOrder.getId())
                    .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
            assertEquals(OrderState.ALLOCATION_EXCEPTION, beerOrderFound.getOrderState());
        });

        FailedAllocationCompenReqEvent failedAllocationCompenReqEvent =
                (FailedAllocationCompenReqEvent) jmsClient
                .receiveAndConvert(JmsConfig.FAILED_ALLOCATION_COMPEN_REQ_QUEUE);


        assertNotNull(failedAllocationOrder);
        assertEquals(OrderState.ALLOCATION_EXCEPTION, failedAllocationOrder.getOrderState());

        assertNotNull(failedAllocationCompenReqEvent);
        assertThat(failedAllocationCompenReqEvent.getBeerOrderId())
                .isEqualTo(failedAllocationOrder.getId());
    }


    @Test
    public void testPartialAllocation() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .id(beerId)
                .upc("1234")
                .build();
        wireMockServer.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_SERVICE_GET_BY_UPC_PATH + beerDTO.getUpc())
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDTO)))
        );

        BeerOrder newBeerOrder = createBeerOrder();
        newBeerOrder.setCustomerRef("test-partial-allocation"); // flag MQ that should fail

        BeerOrder failedPartialOrder = beerOrderManager.newBeerOrder(newBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder beerOrderFound = beerOrderRepository.findById(failedPartialOrder.getId())
                    .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
            assertEquals(OrderState.PENDING_INVENTORY, beerOrderFound.getOrderState());
        });

        assertNotNull(failedPartialOrder);
        assertEquals(OrderState.PENDING_INVENTORY, failedPartialOrder.getOrderState());
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
