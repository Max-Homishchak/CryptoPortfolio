package com.khomishchak.cryptoportfolio.model.goals;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.khomishchak.cryptoportfolio.model.User;
import com.khomishchak.cryptoportfolio.model.enums.GoalType;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "self_goals")
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
public class SelfGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;

    @Enumerated(EnumType.STRING)
    private GoalType goalType;

    private double goalAmount;

    @Transient
    private double currentAmount;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean isAchieved = false;
    // closed means that we won't have any running logic on this entity, we will be only getting the info about closed goals
    private boolean isClosed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
