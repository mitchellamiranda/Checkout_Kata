package com.mitchell.fluro.checkout.domain.promotion;

import com.mitchell.fluro.checkout.domain.model.Basket;

/**
 * Produces all eligible, repeatable bundle alternatives for the original basket.
 * Implementations must be immutable, deterministic and free of side effects.
 * This describes offers, not an allocation; the pricing engine chooses quantities.
 */
@FunctionalInterface
public interface Promotion {

    PromotionResult apply(Basket basket);
}
