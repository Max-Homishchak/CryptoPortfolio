package com.khomishchak.cryproportfolio.model.exchanger;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.khomishchak.cryproportfolio.model.User;
import com.khomishchak.cryproportfolio.model.enums.ExchangerCode;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_key_settings")
public class ApiKeySetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private User user;

    @Enumerated(value = EnumType.STRING)
    private ExchangerCode code;

    @Embedded
    private ApiKeysPair apiKeys;
}