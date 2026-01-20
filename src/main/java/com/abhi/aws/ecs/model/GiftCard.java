package com.abhi.aws.ecs.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCard {
    private Long id;
    private String userName;
    private String giftCardType;
    private Double amount;
    private String date;
}
