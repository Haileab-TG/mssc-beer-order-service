package guru.sfg.beer.order.service.stateMachine.action;

import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FailedAllocationCompensationAction implements Action<OrderState, OrderEvent> {
    private final JmsTemplate jmsClient;

    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
//        jmsClient.convertAndSend(
//
//        );
    }
}
