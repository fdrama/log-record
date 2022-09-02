package com.simple.log.service;

import com.simple.log.model.Order;

/**
 * @author fdrama
 * @date 2022年08月30日 17:21
 */
public interface OrderService {

    Order query(String orderId);

    Order update(Order orderBO);

    Order add(Order orderBO);

    boolean delete(String orderId);

    void diff(Order order, Order order1);
}
