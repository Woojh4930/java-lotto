package lotto.user;

import lotto.LottoInformation;
import lotto.store.Lotto;
import lotto.store.LottoDraw;
import lotto.store.LottoMachine;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Customer {
    private static final String NUMERIC_ERROR = "[ERROR] 숫자를 입력해 주시기 바랍니다.";
    private static final String DUPLICATION_ERROR = "[ERROR] 당첨 번호와 보너스 번호에 중복된 숫자가 있습니다.";
    private static final String NUMBER_OF_PURCHASES_PHRASE = "개를 구매했습니다.";
    private static final String RESULT_PHRASE = "당첨 통계\n---";
    private static final String RESULT_EARNING_RATE_PHRASE = "총 수익률은 %.1f%%입니다.";

    private final List<Lotto> lotteries;
    private final Map<LottoInformation, Integer> rankings = createRankings();
    private final int pay;

    public Customer(String readline) {
        validateNumeric(readline);
        this.pay = Integer.parseInt(readline);
        lotteries = buyLotteries(pay);
    }

    private List<Lotto> buyLotteries(int pay) {
        return LottoMachine.getInstance().pickLotteries(pay);
    }

    private void validateNumeric(String readline) {
        if (Pattern.matches("^\\d", readline)) {
            throw new IllegalArgumentException(NUMERIC_ERROR);
        }
    }

    private void validateDuplication(int bonusNumber, List<Integer> matchNumbers) {
        if (matchNumbers.contains(bonusNumber)) {
            throw new IllegalArgumentException(DUPLICATION_ERROR);
        }
    }

    public String toLottoString() {
        StringJoiner currentLotteries = new StringJoiner("\n");

        currentLotteries.add(lotteries.size() + NUMBER_OF_PURCHASES_PHRASE);
        lotteries.forEach(lotto -> currentLotteries.add(lotto.toString()));
        return currentLotteries.toString();
    }

    public List<Integer> matchWinNumbers(List<Integer> winNumbers) {
        return lotteries.stream()
                .map(lottoNumbers -> lottoNumbers.compareWinNumbers(winNumbers))
                .collect(Collectors.toList());
    }

    public List<Boolean> matchBonusNumber(int bonusNumber, List<Integer> matchNumbers) {
        validateDuplication(bonusNumber, matchNumbers);
        AtomicInteger startIndex = new AtomicInteger();

        return lotteries.stream().map(matchNumber -> {
            if (matchNumbers.get(startIndex.getAndIncrement()) == 5) {
                return matchNumber.compareBonusNumber(bonusNumber);
            }
            return false;
        }).collect(Collectors.toList());
    }

    public Map<LottoInformation, Integer> createWinnings(String winNumbers, String bonusNumber) {
        List<Integer> matchNumbers = matchWinNumbers(LottoDraw.getInstance().pickWinNumbers(winNumbers));
        List<Boolean> matchBonus = matchBonusNumber(LottoDraw.getInstance().pickBonusNumber(bonusNumber), matchNumbers);
        return inputRankings(matchNumbers, matchBonus);
    }

    private Map<LottoInformation, Integer> inputRankings(List<Integer> matchNumbers, List<Boolean> matchBonus) {
        AtomicInteger startIndex = new AtomicInteger();

        while (startIndex.get() < matchNumbers.size()) {
            int matchNumber = matchNumbers.get(startIndex.get());
            LottoInformation lottoInformation = LottoInformation
                    .makeLottoInformation(matchNumber, matchBonus.get(startIndex.getAndIncrement()));
            rankings.put(lottoInformation, rankings.get(lottoInformation) + 1);
        }
        return rankings;
    }

    private Map<LottoInformation, Integer> createRankings() {
        return new HashMap<>() {{
            Arrays.asList(LottoInformation.values()).forEach(lottoInformation -> put(lottoInformation, 0));
        }};
    }

    private long calculateWinnings() {
        AtomicLong winnings = new AtomicLong();
        rankings.forEach((ranking, count) -> winnings.addAndGet(ranking.getWinnings(count)));
        return winnings.get();
    }

    private double calculateEarningRate(long winnings) {
        double earningRate = (winnings / (double) pay) * 100;
        return Math.round(earningRate * 10) / 10.0;
    }

    public String toResultString() {
        StringJoiner resultAlert = new StringJoiner("\n");
        resultAlert.add(RESULT_PHRASE);
        Arrays.stream(LottoInformation.values())
                .filter(lottoInformation -> lottoInformation.getMatchNumber() >= 3)
                .forEach(lottoInformation -> resultAlert.add(lottoInformation.getInformation(rankings.get(lottoInformation))));
        resultAlert.add(String.format(RESULT_EARNING_RATE_PHRASE, calculateEarningRate(calculateWinnings())));
        return resultAlert.toString();
    }

}
