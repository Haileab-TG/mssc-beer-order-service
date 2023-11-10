package guru.sfg.beer.order.service.stateMachine.action;

import common.event.DeallocateCancelledOrderReqEvent;
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
public class SendDeallocateCancelledOrderReqAction implements Action<OrderState, OrderEvent> {
    private final JmsTemplate jmsClient;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;


    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
        UUID orderIdNullable = context.getMessageHeaders().get(BeerOrderManagerImpl.ORDER_ID_HEADER, UUID.class);
        Optional.ofNullable(orderIdNullable)
            .ifPresentOrElse(
                orderId -> {
                    beerOrderRepository.findById(orderId)
                        .ifPresentOrElse(
                            beerOrder -> {
                                System.out.println("Trying deallocate MQ.....");
                                jmsClient.convertAndSend(
                                    JmsConfig.CANCELLED_ORDER_DEALLOCATE_REQ_QUEUE,
                                    DeallocateCancelledOrderReqEvent.builder()
                                        .beerOrderDto(
                                            beerOrderMapper.beerOrderToDto(beerOrder)
                                        )
                                        .build()
                                );
                            },
                            () -> log.error("BeerOrder findById returned empty : SendDeallocateCancelledOrderReqAction")
                        );
                },
                () -> log.error("SM messageHeader doesn't contain orderId : SendDeallocateCancelledOrderReqAction")
            );

    }
}
