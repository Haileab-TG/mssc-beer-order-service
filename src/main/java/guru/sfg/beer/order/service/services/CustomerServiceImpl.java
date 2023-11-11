package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.web.mappers.CustomerMapper;
import guru.sfg.beer.order.service.web.model.CustomerPagedList;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomerServiceImpl implements CustomerService{
    private final CustomerMapper customerMapper;
    private final CustomerRepository customerRepository;
    @Override
    public CustomerPagedList findAll(Integer pageSize, Integer pageNumber) {
       Page<Customer> customerPage = customerRepository.findAll(PageRequest.of(pageNumber, pageSize));

        return new CustomerPagedList(customerPage.get()
                .map(customerMapper::toDto)
                .toList(),
                PageRequest.of(customerPage.getPageable().getPageNumber(), customerPage.getPageable().getPageSize()),
                customerPage.getTotalElements()
                );
    }
}
