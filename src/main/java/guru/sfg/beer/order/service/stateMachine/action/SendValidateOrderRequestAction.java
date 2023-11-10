package guru.sfg.beer.order.service.stateMachine.action;

import common.event.ValidateOrderRequestEvent;
import common.model.BeerOrderDto;
import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.OrderEvent;
import guru.sfg.beer.order.service.domain.OrderState;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class SendValidateOrderRequestAction implements Action<OrderState, OrderEvent>{
    private final JmsTemplate jmsClient;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
        Optional.ofNullable(context.getMessageHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER, UUID.class))
                .ifPresentOrElse(orderId -> {
                    beerOrderRepository.findById(orderId)
                            .ifPresentOrElse(beerOrder -> {
                                jmsClient.convertAndSend(
                                        JmsConfig.VALIDATE_ORDER_REQ_QUEUE,
                                        ValidateOrderRequestEvent.builder()
                                                .beerOrderDto(beerOrderMapper.beerOrderToDto(beerOrder))
                                                .build()
                                );
                                log.debug("SendValidateOrderRequestAction: Validate order request sent for " + beerOrder);
                            }, () -> log.error("SendValidateOrderRequestAction : BeerOrder with this ID not found in DB"));
                }, () -> log.error("SendValidateOrderRequestAction : orderId not found in the message header"));
    }
}
