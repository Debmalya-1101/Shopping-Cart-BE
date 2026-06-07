package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminAttributeDTO {
    private Long keyId;       // AttributeKey ID (FK)
    private String keyName;   // AttributeKey display name (read-only in GET, ignored in POST/PUT)
    private String value;
}

