package com.mitchell.fluro.checkout.application.checkout;

import com.mitchell.fluro.checkout.domain.model.Basket;
import com.mitchell.fluro.checkout.domain.model.Money;

/**
 * A single, thread-confined transaction. Failed scans leave the basket unchanged;
 * reading a total or snapshot does not consume items or promotions.
 */
public interface ICheckout {

    void scan(String sku);

    Money getTotal();

    Basket getBasket();
}
