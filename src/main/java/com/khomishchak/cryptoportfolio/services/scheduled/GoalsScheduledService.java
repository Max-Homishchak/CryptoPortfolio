package com.khomishchak.cryptoportfolio.services.scheduled;

import com.khomishchak.cryptoportfolio.model.goals.GoalType;
import com.khomishchak.cryptoportfolio.model.goals.SelfGoal;
import com.khomishchak.cryptoportfolio.repositories.SelfGoalRepository;
import com.khomishchak.cryptoportfolio.services.GoalsService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalsScheduledService implements ScheduledService {

    private final SelfGoalRepository goalRepository;
    private final GoalsService goalsService;

    public GoalsScheduledService(SelfGoalRepository goalRepository, GoalsService goalsService) {
        this.goalRepository = goalRepository;
        this.goalsService = goalsService;
    }

    @Override
    @Scheduled(cron = "0 43 12 * * ?") // every day at 00:00
    public void doAtTheStartOfTheDay() {
        List<SelfGoal> overdueGoals = goalRepository.getAllOverdueGoals();

        overdueGoals.forEach(oldGoal -> {
            GoalType goalType = oldGoal.getGoalType();
            SelfGoal newGoal = SelfGoal.builder()
                    .ticker(oldGoal.getTicker())
                    .user(oldGoal.getUser())
                    .goalAmount(oldGoal.getGoalAmount())
                    .goalType(goalType)
                    .startDate(goalType.getStartTime(1))
                    .endDate(goalType.getEndTime())
                    .build();

            oldGoal.setAchieved(goalsService.overdueGoalIsAchieved(oldGoal));
            oldGoal.setClosed(true);

            goalRepository.saveAll(List.of(oldGoal, newGoal));
        });
    }
}
