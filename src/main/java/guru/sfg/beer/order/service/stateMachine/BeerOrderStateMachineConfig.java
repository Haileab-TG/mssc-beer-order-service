package guru.sfg.beer.order.service.stateMachine;

import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

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

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal().source(OrderState.NEW).target(OrderState.NEW).event(OrderEvent.VALIDATE_ORDER)
                .and()
                .withExternal().source(OrderState.NEW).target(OrderState.VALIDATED).event(OrderEvent.VALIDATION_PASSED)
                .and()
                .withExternal().source(OrderState.NEW).target(OrderState.VALIDATION_EXCEPTION).event(OrderEvent.VALIDATION_FAILED)
                .and()

                .withExternal().source(OrderState.VALIDATED).target(OrderState.ALLOCATED).event(OrderEvent.ALLOCATION_SUCCESS)
                .and()
                .withExternal().source(OrderState.VALIDATED).target(OrderState.PENDING_INVENTORY).event(OrderEvent.ALLOCATION_NO_INVENTORY)
                .and()
                .withExternal().source(OrderState.VALIDATED).target(OrderState.ALLOCATION_EXCEPTION).event(OrderEvent.ALLOCATION_FAILED)
                .and()

                .withExternal().source(OrderState.ALLOCATED).target(OrderState.PICKED_UP).event(OrderEvent.ORDER_PICKED_UP);
//                .and()
//                .withExternal().source(OrderState.ALLOCATED).target(OrderState.DELIVERY_EXCEPTION).event(OrderEvent.)
//                .withExternal().source(OrderState.ALLOCATED).target(OrderState.DELIVERED).event(OrderEvent.)
    }
}
