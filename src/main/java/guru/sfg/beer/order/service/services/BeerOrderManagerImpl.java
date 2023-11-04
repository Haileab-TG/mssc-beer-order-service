package guru.sfg.beer.order.service.services;

import common.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderLineRepository;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.stateMachine.BeerOrderStateMachineInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {
    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<OrderState, OrderEvent> factory;
    private final BeerOrderStateMachineInterceptor beerOrderStateMachineInterceptor;
    private final BeerOrderLineRepository beerOrderLineRepository;

    public final static String ORDER_ID_HEADER = "order-id";

    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null); // just making user the db is initilizing id
        beerOrder.setOrderState(OrderState.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendOrderEvent(savedBeerOrder, OrderEvent.VALIDATE_ORDER);
        return null;
    }

    @Override
    public void processValidationResult(UUID orderId, boolean isValid) {
        BeerOrder beerOrderInDB = beerOrderRepository.findOneById(orderId);
        OrderEvent event = isValid ? OrderEvent.VALIDATION_PASSED : OrderEvent.VALIDATION_FAILED;
        Optional.ofNullable(beerOrderInDB)
                .ifPresent(beerOrder -> {
                    sendOrderEvent(beerOrder, event);

                    BeerOrder beerOrderUpdatedState = beerOrderRepository.findOneById(beerOrder.getId());
                    sendOrderEvent(beerOrderUpdatedState, OrderEvent.ALLOCATE_ORDER);
                });
    }

    @Override
    public void processAllocationResult(BeerOrderDto beerOrderDto, boolean pendingInventory, boolean allocationError) {
        OrderEvent event;
        if(allocationError) event = OrderEvent.ALLOCATION_FAILED;
        else if (pendingInventory) {
            event = OrderEvent.ALLOCATION_NO_INVENTORY;
            updateOrderLineAllocatedQty(beerOrderDto);
        }
        else {
            event = OrderEvent.ALLOCATION_SUCCESS;
            updateOrderLineAllocatedQty(beerOrderDto);
        };

        BeerOrder beerOrderOrNull = beerOrderRepository.findOneById(beerOrderDto.getId());
        Optional.ofNullable(beerOrderOrNull)
                .ifPresent(beerOrder -> sendOrderEvent(beerOrder, event));

    }

    private void updateOrderLineAllocatedQty(BeerOrderDto beerOrderDto) {
        beerOrderDto.getBeerOrderLines()
                .forEach(beerOrderLineDto -> {
                    beerOrderLineRepository
                            .findById(beerOrderLineDto.getId())
                            .ifPresent(beerOrderLine -> {
                                beerOrderLine.setQuantityAllocated(
                                        beerOrderLineDto.getQuantityAllocated()
                                );
                                beerOrderLineRepository.save(beerOrderLine);
                            });
                });
    }

    private void sendOrderEvent(BeerOrder beerOrder, OrderEvent orderEvent) {
        StateMachine<OrderState, OrderEvent> sm = build(beerOrder);
        Message<OrderEvent> msg = buildMessage(orderEvent, beerOrder.getId());
        sm.sendEvent(msg);
    }

    private Message<OrderEvent> buildMessage(OrderEvent orderEvent, UUID orderId) {
        return MessageBuilder
                .withPayload(orderEvent)
                .setHeader(ORDER_ID_HEADER, orderId)
                .build();
    }

    private StateMachine<OrderState, OrderEvent> build(BeerOrder beerOrder) {
        StateMachine<OrderState, OrderEvent> sm = factory.getStateMachine(beerOrder.getId());
        sm.stop(); // stopping to reset the state

        sm.getStateMachineAccessor() //reseting the state to match state in db
                .doWithAllRegions(sma -> {
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderState(), null, null, null));
                    sma.addStateMachineInterceptor(beerOrderStateMachineInterceptor);
                });
        sm.start();
        return sm;
    }
}
