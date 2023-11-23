package com.khomishchak.cryptoportfolio.services;

import com.khomishchak.cryptoportfolio.exceptions.GoalsTableNotFoundException;
import com.khomishchak.cryptoportfolio.exceptions.GoalsTableRecordNotFoundException;
import com.khomishchak.cryptoportfolio.model.Transaction;
import com.khomishchak.cryptoportfolio.model.TransactionType;
import com.khomishchak.cryptoportfolio.model.User;
import com.khomishchak.cryptoportfolio.model.enums.ExchangerCode;
import com.khomishchak.cryptoportfolio.model.enums.GoalType;
import com.khomishchak.cryptoportfolio.model.exchanger.Balance;
import com.khomishchak.cryptoportfolio.model.goals.CryptoGoalsRecordUpdateReq;
import com.khomishchak.cryptoportfolio.model.goals.CryptoGoalsTable;
import com.khomishchak.cryptoportfolio.model.goals.CryptoGoalsTableRecord;
import com.khomishchak.cryptoportfolio.model.goals.SelfGoal;
import com.khomishchak.cryptoportfolio.repositories.CryptoGoalsTableRepository;
import com.khomishchak.cryptoportfolio.repositories.SelfGoalRepository;
import com.khomishchak.cryptoportfolio.services.exchangers.ExchangerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GoalsServiceImpl implements GoalsService {

    public static final int END_OF_PREVIOUS_PERIOD = 2;
    public static final int END_OF_CURRENT_PERIOD = 1;
    public static final int PERCENTAGE_SCALE = 100;
    private final CryptoGoalsTableRepository cryptoGoalsTableRepository;
    private final UserService userService;
    private final SelfGoalRepository selfGoalRepository;
    private final ExchangerService exchangerService;

    public GoalsServiceImpl(CryptoGoalsTableRepository cryptoGoalsTableRepository, UserService userService,
            SelfGoalRepository selfGoalRepository, ExchangerService exchangerService) {
        this.cryptoGoalsTableRepository = cryptoGoalsTableRepository;
        this.userService = userService;
        this.selfGoalRepository = selfGoalRepository;
        this.exchangerService = exchangerService;
    }

    @Override
    public CryptoGoalsTable createCryptoGoalsTable(Long userId, CryptoGoalsTable tableRequest) {

        User user = userService.getUserById(userId);
        user.setCryptoGoalsTable(tableRequest);
        tableRequest.setUser(user);

        return saveCryptoTable(tableRequest);
    }

    @Override
    public CryptoGoalsTable getCryptoGoalsTable(Long userId) {
        CryptoGoalsTable table  = userService.getUserById(userId).getCryptoGoalsTable();
        table.getTableRecords().forEach(this::setPostQuantityValues);
        return table;
    }

    @Override
    public CryptoGoalsTable updateCryptoGoalsTable(CryptoGoalsTable cryptoGoalsTable) {
        return saveCryptoTable(cryptoGoalsTable);
    }

    @Override
    public CryptoGoalsTable updateCryptoGoalsTableRecords(List<CryptoGoalsRecordUpdateReq> recordUpdateReq, long tableId) {
       CryptoGoalsTable cryptoGoalsTable = getCryptoGoalsTableOrThrowException(tableId);

       recordUpdateReq.forEach(record -> {
           CryptoGoalsTableRecord tableRecord = cryptoGoalsTable.getTableRecords().stream()
                   .filter(r -> r.getName().equals(record.ticker()))
                   .findFirst()
                   .orElseThrow(() -> new GoalsTableRecordNotFoundException(String.format("Record with ticker:%s was not found for table with id:%d", record.ticker(), tableId)));

           tableRecord.setAverageCost(tableRecord.getAverageCost()
                   .multiply(tableRecord.getQuantity())
                   .add(record.price().multiply(BigDecimal.valueOf(record.amount())))
                   .divide(tableRecord.getQuantity().add(BigDecimal.valueOf(record.amount())),4, RoundingMode.DOWN));
           tableRecord.setQuantity(tableRecord.getQuantity().add(BigDecimal.valueOf(record.amount())));
       });

       return saveCryptoTable(cryptoGoalsTable);
    }

    @Override
    public List<SelfGoal> getSelfGoals(Long userId) {

        List<SelfGoal> result = selfGoalRepository.findAllByUserId(userId);

        result.forEach(goal -> {
            goal.setCurrentAmount(getDepositValueForPeriod(userService.getUserById(userId), goal.getTicker(), goal.getStartDate(), goal.getEndDate()));
            goal.setAchieved(goal.getCurrentAmount() > goal.getGoalAmount());
        });
        return result;
    }

    @Override
    @Transactional
    public List<SelfGoal> createSelfGoals(Long userId, List<SelfGoal> goals) {
        User user = userService.getUserById(userId);
        user.setSelfGoals(goals);

        goals.forEach(g -> {
            g.setUser(user);
            g.setStartDate(LocalDateTime.now());
            g.setEndDate(g.getGoalType().getEndTime());
            g.setAchieved(g.getCurrentAmount() > g.getGoalAmount());
            g.setCurrentAmount(getDepositValueForPeriod(user, g.getTicker(), g.getStartDate(), g.getEndDate()));
        });

        userService.saveUser(user);

        return goals;
    }

    // TODO: should be replaced with strategy pattern to handle multiple goal types, not only deposit
    @Override
    public boolean overdueGoalIsAchieved(SelfGoal goal) {
        GoalType goalType = goal.getGoalType();
        double depositValue = getDepositValueForPeriod(goal.getUser(), goal.getTicker(),
                goalType.getStartTime(END_OF_PREVIOUS_PERIOD), goalType.getStartTime(END_OF_CURRENT_PERIOD));

        goal.setCurrentAmount(depositValue);
        goal.setAchieved(depositValue > goal.getGoalAmount());
        return selfGoalRepository.save(goal).isAchieved();
    }

    private double getDepositValueForPeriod(User user, String ticker, LocalDateTime startingData, LocalDateTime endingDate) {
        return user.getBalances().stream()
                .map(Balance::getCode)
                .map(c -> getDepositValueForPeriodForSingleExchanger(user.getId(), ticker, startingData, endingDate, c))
                .reduce(0.0, Double::sum);
    }

    private double getDepositValueForPeriodForSingleExchanger(long userId, String ticker, LocalDateTime startingDate,
            LocalDateTime endingDate, ExchangerCode code) {

        return exchangerService.getWithdrawalDepositWalletHistory(userId, code)
                .stream()
                .filter(transaction -> transaction.getTicker().equalsIgnoreCase(ticker) &&
                        transaction.getTransactionType().equals(TransactionType.DEPOSIT) &&
                        transaction.getCreatedAt().isAfter(startingDate) && transaction.getCreatedAt().isBefore(endingDate))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
    }

    private CryptoGoalsTableRecord setPostQuantityValues(CryptoGoalsTableRecord entity) {
        BigDecimal goalQuantity = entity.getGoalQuantity();
        BigDecimal quantity = entity.getQuantity();

        BigDecimal leftToBuy = goalQuantity.subtract(quantity);

        entity.setLeftToBuy(leftToBuy.compareTo(BigDecimal.ZERO) >= 0 ? goalQuantity.subtract(quantity) : BigDecimal.ZERO);
        entity.setDonePercentage(quantity
                        .multiply(BigDecimal.valueOf(PERCENTAGE_SCALE))
                        .divide(goalQuantity, 1, RoundingMode.DOWN));
        entity.setFinished(quantity.compareTo(goalQuantity) >= 0);

        return entity;
    }

    CryptoGoalsTable saveCryptoTable(CryptoGoalsTable table) {
        CryptoGoalsTable createdTable = cryptoGoalsTableRepository.save(table);
        createdTable.getTableRecords().forEach(this::setPostQuantityValues);
        return createdTable;
    }

    private CryptoGoalsTable getCryptoGoalsTableOrThrowException(long tableId) {
        return cryptoGoalsTableRepository.findById(tableId)
                .orElseThrow(() -> new GoalsTableNotFoundException(String.format("CryptoGoalsTAble with id: %s was not found", tableId)));
    }
}
