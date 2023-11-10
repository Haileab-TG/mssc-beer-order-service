package guru.sfg.beer.order.service.stateMachine;

import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.stateMachine.action.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@RequiredArgsConstructor
@EnableStateMachineFactory
@Configuration
public class BeerOrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {
    private final SendValidateOrderRequestAction sendValidateOrderRequestAction;
    private final SendAllocateOrderRequestAction sendAllocateOrderRequestAction;
    private final FailedValidationCompensationAction failedValidationCompensationAction;
    private final FailedAllocationCompensationAction failedAllocationCompensationAction;
    private final SendDeallocateCancelledOrderReqAction sendDeallocateCancelledOrderReqAction;

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(OrderState.NEW)

                .states(EnumSet.allOf(OrderState.class))

                .end(OrderState.PICKED_UP)
                .end(OrderState.DELIVERED)
                .end(OrderState.CANCELLED)
                .end(OrderState.VALIDATION_EXCEPTION)
                .end(OrderState.ALLOCATION_EXCEPTION)
                .end(OrderState.DELIVERY_EXCEPTION);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(OrderState.NEW)
                    .target(OrderState.PENDING_VALIDATION)
                    .event(OrderEvent.VALIDATE_ORDER)
                    .action(sendValidateOrderRequestAction)
                .and()

                .withExternal()
                    .source(OrderState.PENDING_VALIDATION)
                    .target(OrderState.VALIDATED)
                    .event(OrderEvent.VALIDATION_PASSED)
                .and()
                .withExternal()
                    .source(OrderState.PENDING_VALIDATION)
                    .target(OrderState.VALIDATION_EXCEPTION)
                    .event(OrderEvent.VALIDATION_FAILED)
                    .action(failedValidationCompensationAction)
                .and()
                .withExternal()
                    .source(OrderState.PENDING_VALIDATION)
                    .target(OrderState.CANCELLED)
                    .event(OrderEvent.CANCEL_ORDER)
                .and()


                .withExternal()
                    .source(OrderState.VALIDATED)
                    .target(OrderState.PENDING_ALLOCATION)
                    .event(OrderEvent.ALLOCATE_ORDER)
                    .action(sendAllocateOrderRequestAction)
                .and()


                .withExternal()
                    .source(OrderState.PENDING_ALLOCATION)
                    .target(OrderState.ALLOCATED)
                    .event(OrderEvent.ALLOCATION_SUCCESS)
                .and()
                .withExternal()
                    .source(OrderState.PENDING_ALLOCATION)
                    .target(OrderState.PENDING_INVENTORY)
                    .event(OrderEvent.ALLOCATION_NO_INVENTORY)
                .and()
                .withExternal()
                    .source(OrderState.PENDING_ALLOCATION)
                    .target(OrderState.ALLOCATION_EXCEPTION)
                    .event(OrderEvent.ALLOCATION_FAILED)
                    .action(failedAllocationCompensationAction)
                .and()
                .withExternal()
                    .source(OrderState.PENDING_ALLOCATION)
                    .target(OrderState.CANCELLED)
                    .event(OrderEvent.CANCEL_ORDER)
                .and()

                .withExternal()
                    .source(OrderState.ALLOCATED)
                    .target(OrderState.PICKED_UP)
                    .event(OrderEvent.ORDER_PICKED_UP)
                .and()
                .withExternal()
                    .source(OrderState.ALLOCATED)
                    .target(OrderState.CANCELLED)
                    .event(OrderEvent.CANCEL_ORDER)
                    .action(sendDeallocateCancelledOrderReqAction);
//                .and()
//                .withExternal().source(OrderState.ALLOCATED).target(OrderState.DELIVERY_EXCEPTION).event(OrderEvent.)
//                .withExternal().source(OrderState.ALLOCATED).target(OrderState.DELIVERED).event(OrderEvent.)
    }
}
