package guru.sfg.beer.order.service.services;

import common.model.BeerOrderDto;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderLineRepository;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.stateMachine.BeerOrderStateMachineInterceptor;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {
    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<OrderState, OrderEvent> factory;
    private final BeerOrderStateMachineInterceptor beerOrderStateMachineInterceptor;
    private final BeerOrderLineRepository beerOrderLineRepository;

    public final static String ORDER_ID_HEADER = "order-id";

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null); // just making user the db is initilizing id
        beerOrder.setOrderState(OrderState.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        log.debug("BeerOrderManagerImpl - newBeerOrder : Beer order saved SUCCESS in DB with ID " + savedBeerOrder.getId());
        sendOrderEvent(savedBeerOrder, OrderEvent.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processValidationResult(UUID orderId, boolean isValid) {
        Optional<BeerOrder> beerOrderInDB = beerOrderRepository.findById(orderId);
        OrderEvent event = isValid ? OrderEvent.VALIDATION_PASSED : OrderEvent.VALIDATION_FAILED;
        beerOrderInDB
                .ifPresentOrElse(beerOrder -> {
                    sendOrderEvent(beerOrder, event);
                    BeerOrder beerOrderUpdatedState = beerOrderRepository.findById(beerOrder.getId()).orElseThrow();
                    sendOrderEvent(beerOrderUpdatedState, OrderEvent.ALLOCATE_ORDER);
                }, () -> log.error("BeerOrderManagerImpl : beerOrder with given Id Not found in DB for id " + orderId));
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

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());
       beerOrderOptional
                .ifPresentOrElse(
                        beerOrder -> sendOrderEvent(beerOrder, event)
                , () -> log.error("BeerOrder findById is empty : processAllocationResult - BeerOrderManagerImpl"));
    }

    @Override
    public BeerOrder pickUpBeerOrder(UUID beerOrderId) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);
        beerOrderOptional.ifPresentOrElse(
                beerOrderInDB -> sendOrderEvent(beerOrderInDB, OrderEvent.ORDER_PICKED_UP),
                () -> log.error("BeerOrder findById is empty : pickUpBeerOrder - BeerOrderManagerImpl")
        );

        beerOrderOptional = beerOrderRepository.findById(beerOrderId); //returning the newly updated one
        return beerOrderOptional.orElse(null);
    }

    @Override
    public BeerOrder cancelBeerOrder(UUID beerOrderId) {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);
        beerOrderOptional.ifPresentOrElse(
                beerOrder -> sendOrderEvent(beerOrder, OrderEvent.CANCEL_ORDER),
                () -> log.error("BeerOrder findById is empty : cancelBeerOrder - BeerOrderManagerImpl")
        );
        return beerOrderRepository.findById(beerOrderId).orElse(null);
    }

    private void updateOrderLineAllocatedQty(BeerOrderDto beerOrderDto) {
        beerOrderDto.getBeerOrderLines()
                .forEach(beerOrderLineDto -> {
                    beerOrderLineRepository.findById(beerOrderLineDto.getId())
                            .ifPresentOrElse(beerOrderLine -> {
                                beerOrderLine.setQuantityAllocated(
                                        beerOrderLineDto.getQuantityAllocated()
                                );
                                beerOrderLineRepository.save(beerOrderLine);
                            }, () -> log.error("BeerOrder findById is empty : updateOrderLineAllocatedQty - BeerOrderManagerImpl"));
                });
    }

    private void sendOrderEvent(BeerOrder beerOrder, OrderEvent orderEvent) {
        StateMachine<OrderState, OrderEvent> sm = build(beerOrder);
        Message<OrderEvent> msg = buildMessage(orderEvent, beerOrder.getId());
        sm.sendEvent(msg);
        log.debug("BeerOrderManagerImpl : state event sent " + msg.getPayload());
    }

    private Message<OrderEvent> buildMessage(OrderEvent orderEvent, UUID orderId) {
        return MessageBuilder
                .withPayload(orderEvent)
                .setHeader(ORDER_ID_HEADER, orderId)
                .build();
    }

    private StateMachine<OrderState, OrderEvent> build(BeerOrder beerOrder) {
        StateMachine<OrderState, OrderEvent> sm = factory.getStateMachine(beerOrder.getId().toString());
        sm.stop(); // stopping to reset the state

        sm.getStateMachineAccessor() //reseting the state to match state in db
                .doWithAllRegions(sma -> {
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderState(), null, null, null));
                    sma.addStateMachineInterceptor(beerOrderStateMachineInterceptor);
                });
        sm.start();
        return sm;
    }
//
//    private void awaitForStatus(UUID beerOrderId, OrderState statusEnum) {
//
//        AtomicBoolean found = new AtomicBoolean(false);
//        AtomicInteger loopCount = new AtomicInteger(0);
//
//        while (!found.get()) {
//            if (loopCount.incrementAndGet() > 10) {
//                found.set(true);
//                log.debug("Loop Retries exceeded");
//            }
//
//            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
//                if (beerOrder.getOrderState().equals(statusEnum)) {
//                    found.set(true);
//                    log.debug("Order Found");
//                } else {
//                    log.debug("Order Status Not Equal. Expected: " + statusEnum.name() + " Found: " + beerOrder.getOrderState().name());
//                }
//            }, () -> {
//                log.debug("Order Id Not Found");
//            });
//
//            if (!found.get()) {
//                try {
//                    log.debug("Sleeping for retry");
//                    Thread.sleep(100);
//                } catch (Exception e) {
//                    // do nothing
//                }
//            }
//        }
//    }
}
