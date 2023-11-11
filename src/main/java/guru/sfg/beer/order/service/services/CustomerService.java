package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.web.model.CustomerPagedList;

public interface CustomerService {

    CustomerPagedList findAll(Integer pageSize, Integer pageNumber);
}
