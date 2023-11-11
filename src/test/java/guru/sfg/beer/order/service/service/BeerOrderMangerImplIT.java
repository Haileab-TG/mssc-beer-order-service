package guru.sfg.beer.order.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import common.event.DeallocateCancelledOrderReqEvent;
import common.event.FailedAllocationCompenReqEvent;
import guru.sfg.beer.order.service.RESTclient.beerService.BeerServiceRestTemplateImpl;
import guru.sfg.beer.order.service.RESTclient.beerService.model.BeerDTO;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.beer.order.service.testComponents.ConfirmFailedAllocationCompenEventSent;
import guru.sfg.beer.order.service.testComponents.ConfirmSentDeallocateCancellOrderReqEvevnt;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    @Autowired
    ConfirmFailedAllocationCompenEventSent confirmFailedAllocationCompenEventSent;
    @Autowired
    ConfirmSentDeallocateCancellOrderReqEvevnt sentDeallocateCancellOrderReqEvevnt;



    Customer customer;
    UUID beerId;


    @BeforeEach
    public void setUp(){
        customer = Customer.builder()
                .customerName("Test Customer")
                .build();
        customerRepository.save(customer);
        beerId = UUID.randomUUID();
    }
    @Test
    public void testNewOrderToAllocate() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .upc("1234")
                .id(beerId)
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
        BeerOrder beerOrderFound = beerOrderRepository.findById(allocatedBeerOrder.getId())
                .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
        assertEquals(OrderState.ALLOCATED, beerOrderFound.getOrderState());
        beerOrderFound.getBeerOrderLines().forEach(line -> {
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

    }

    @Test
    public void testAllocatedToPickedUp() throws JsonProcessingException {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setOrderState(OrderState.ALLOCATED);

        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);

        BeerOrder pickedUpOrder = beerOrderManager.pickUpBeerOrder(savedBeerOrder.getId());

        assertNotNull(pickedUpOrder);
        assertEquals(pickedUpOrder.getOrderState(), OrderState.PICKED_UP);

    }

    @Test
    public void testValidationFailed() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .upc("1234")
                .id(beerId)
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
        BeerOrder updateBeerOrder = beerOrderRepository.findById(failedValidationOrder.getId())
                        .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
        assertEquals(OrderState.VALIDATION_EXCEPTION, updateBeerOrder.getOrderState());
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
        log.debug("BeerOrderMangerImplIT - testFailedAllocation : waiting for FailedAllocationCompenReqEvent " +
                "to be availabe in the broker");

        await().untilAsserted(()->{
            FailedAllocationCompenReqEvent failedAllocationCompenReqEvent =
                    confirmFailedAllocationCompenEventSent.getFailedAllocationCompenReqEvent();
            assertNotNull(failedAllocationCompenReqEvent);
            assertThat(failedAllocationCompenReqEvent.getBeerOrderId())
                    .isEqualTo(failedAllocationOrder.getId());
        });
        assertNotNull(failedAllocationOrder);
        BeerOrder updateBeerOrder = beerOrderRepository.findById(failedAllocationOrder.getId())
                .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
        assertEquals(OrderState.ALLOCATION_EXCEPTION, updateBeerOrder.getOrderState());


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

        BeerOrder updatedBeerOrder = beerOrderRepository.findById(failedPartialOrder.getId())
                .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
        assertNotNull(updatedBeerOrder);
        assertEquals(OrderState.PENDING_INVENTORY, updatedBeerOrder.getOrderState());
    }

    @Test
    public void testPendingValidationToCancelled() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .id(beerId)
                .upc("1234")
                .build();
        wireMockServer.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_SERVICE_GET_BY_UPC_PATH + beerDTO.getUpc())
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDTO)))
        );

        BeerOrder beerToCancel = createBeerOrder();
        beerToCancel.setOrderState(OrderState.PENDING_VALIDATION);
        beerToCancel = beerOrderRepository.save(beerToCancel);

        BeerOrder cancelledBeerOrder = beerOrderManager.cancelBeerOrder(beerToCancel.getId());

        assertNotNull(cancelledBeerOrder);
        assertEquals(OrderState.CANCELLED, cancelledBeerOrder.getOrderState());
    }

    @Test
    public void testPendingAllocationToCancelled() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .id(beerId)
                .upc("1234")
                .build();
        wireMockServer.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_SERVICE_GET_BY_UPC_PATH + beerDTO.getUpc())
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDTO)))
        );

        BeerOrder beerToCancel = createBeerOrder();
        beerToCancel.setOrderState(OrderState.PENDING_ALLOCATION);
        beerToCancel = beerOrderRepository.save(beerToCancel);

        BeerOrder cancelledBeerOrder = beerOrderManager.cancelBeerOrder(beerToCancel.getId());

        assertNotNull(cancelledBeerOrder);
        assertEquals(OrderState.CANCELLED, cancelledBeerOrder.getOrderState());
    }

    @Test
    public void testAllocatedToCancelled() throws JsonProcessingException {
        BeerDTO beerDTO = BeerDTO.builder()
                .id(beerId)
                .upc("1234")
                .build();
        wireMockServer.stubFor(
                get(BeerServiceRestTemplateImpl.BEER_SERVICE_GET_BY_UPC_PATH + beerDTO.getUpc())
                        .willReturn(okJson(objectMapper.writeValueAsString(beerDTO)))
        );

        BeerOrder beerToCancel = createBeerOrder();
        beerToCancel.setOrderState(OrderState.ALLOCATED);
        beerToCancel = beerOrderRepository.save(beerToCancel);

        BeerOrder cancelledBeerOrder = beerOrderManager.cancelBeerOrder(beerToCancel.getId());

        await().untilAsserted(() -> {
            BeerOrder beerOrderFound = beerOrderRepository.findById(cancelledBeerOrder.getId())
                    .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
            assertEquals(OrderState.CANCELLED, beerOrderFound.getOrderState());
        });

        BeerOrder updatedBeerOrder = beerOrderRepository.findById(cancelledBeerOrder.getId())
                .orElseThrow(()->new RuntimeException("findById for beer order returned empty"));
        assertNotNull(updatedBeerOrder);
        assertEquals(OrderState.CANCELLED, updatedBeerOrder.getOrderState());

        await().untilAsserted(()->{
            DeallocateCancelledOrderReqEvent deallocateCancelledOrderReqEvent = sentDeallocateCancellOrderReqEvevnt.getDeallocateCancelledOrderReqEvent();
            assertNotNull(deallocateCancelledOrderReqEvent);
            assertThat(deallocateCancelledOrderReqEvent.getBeerOrderDto().getId())
                    .isEqualTo(cancelledBeerOrder.getId());
        });

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
