package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl implements BeerOrderManager {
    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<OrderState, OrderEvent> factory;
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        beerOrder.setId(null); // just making user the db is initilizing id
        beerOrder.setOrderState(OrderState.NEW);
        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendOrderEvent(savedBeerOrder, OrderEvent.VALIDATE_ORDER);
        return null;
    }

    private void sendOrderEvent(BeerOrder beerOrder, OrderEvent orderEvent) {
        StateMachine<OrderState, OrderEvent> sm = build(beerOrder);
        Message<OrderEvent> msg = MessageBuilder
                .withPayload(orderEvent)
                .build();
        sm.sendEvent(msg);
    }

    private StateMachine<OrderState, OrderEvent> build(BeerOrder beerOrder) {
        StateMachine<OrderState, OrderEvent> sm = factory.getStateMachine(beerOrder.getId());
        sm.stop(); // stopping to reset the state

        sm.getStateMachineAccessor() //reseting the state to match state in db
                .doWithAllRegions(sma -> {
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderState(), null, null, null));
                });
        sm.start();
        return sm;
    }
}
