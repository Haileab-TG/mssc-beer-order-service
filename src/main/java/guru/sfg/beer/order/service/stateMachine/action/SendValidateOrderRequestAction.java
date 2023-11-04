package guru.sfg.beer.order.service.stateMachine.action;

import common.event.ValidateOrderRequestEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class SendValidateOrderRequestAction implements Action<OrderState, OrderEvent>{
    private final JmsTemplate jmsClient;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
        Optional.ofNullable(context.getMessageHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER, UUID.class))
                .ifPresent(orderId -> {
                    beerOrderRepository.findById(orderId).ifPresent(beerOrder -> {
                        jmsClient.convertAndSend(
                                JmsConfig.VALIDATE_ORDER_REQUEST_QUEUE,
                                new ValidateOrderRequestEvent(beerOrderMapper.beerOrderToDto(beerOrder))
                        );
                    });
                });
    }
}
