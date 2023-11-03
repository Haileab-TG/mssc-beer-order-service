package guru.sfg.beer.order.service.stateMachine;

import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BeerOrderStateMachineInterceptor extends StateMachineInterceptorAdapter<OrderState, OrderEvent> {
    private final BeerOrderRepository beerOrderRepository;
    @Override
    public void preStateChange(State<OrderState, OrderEvent> state, Message<OrderEvent> message, Transition<OrderState, OrderEvent> transition, StateMachine<OrderState, OrderEvent> stateMachine, StateMachine<OrderState, OrderEvent> rootStateMachine) {
        Optional.ofNullable(message)
                .ifPresent(msg ->{
                    Optional.ofNullable(
                                msg.getHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER, UUID.class)
                            ).ifPresent(beerOrderId ->{
                                beerOrderRepository.findById(beerOrderId)
                                        .ifPresent(beerOrder -> {
                                            beerOrder.setOrderState(state.getId());
                                            beerOrderRepository.saveAndFlush(beerOrder); //over pass the lazy hibernate default and force to commit it to db right away
                                        }
                        );
                    });
                });
    }
}
