package guru.sfg.beer.order.service.stateMachine.action;

import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class FailedValidationCompensationAction  implements Action<OrderState, OrderEvent> {
    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
        UUID orderId = context.getMessageHeaders()
                .get(BeerOrderManagerImpl.ORDER_ID_HEADER, UUID.class);
        log.error("Compensating Transaction >>>>> validation failed for order " + orderId);

    }
}
