package guru.sfg.beer.order.service.testComponents;

import common.event.ValidateOrderRequestEvent;
import common.event.ValidateOrderResultEvent;
import guru.sfg.beer.order.service.config.JmsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateBeerOrderListener {

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_REQ_QUEUE)
    @SendTo(JmsConfig.VALIDATE_ORDER_RES_QUEUE)
    public ValidateOrderResultEvent listener(ValidateOrderRequestEvent event){
        String failedValidationTestFlag = event.getBeerOrderDto().getCustomerRef();
        boolean isValid = failedValidationTestFlag == null ||
                !failedValidationTestFlag.equals("test-failed-validation");
        log.debug("ValidateBeerOrderListener : ValidateOrderResultEvent response sent and validity was "+ isValid);

        return ValidateOrderResultEvent.builder()
                .isValid(isValid)
                .orderId(event.getBeerOrderDto().getId())
                .build();
    }
}
