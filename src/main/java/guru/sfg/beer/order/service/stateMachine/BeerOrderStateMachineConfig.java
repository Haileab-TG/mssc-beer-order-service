package guru.sfg.beer.order.service.stateMachine;

import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;

import java.util.EnumSet;

@EnableStateMachineFactory
@Configuration
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {
    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(OrderState.NEW)

                .states(EnumSet.allOf(OrderState.class))

                .end(OrderState.PICKED_UP)
                .end(OrderState.DELIVERED)
                .end(OrderState.VALIDATION_EXCEPTION)
                .end(OrderState.ALLOCATION_EXCEPTION)
                .end(OrderState.DELIVERY_EXCEPTION);
    }
}
