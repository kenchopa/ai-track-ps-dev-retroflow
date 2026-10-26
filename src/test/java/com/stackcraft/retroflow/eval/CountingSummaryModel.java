package com.stackcraft.retroflow.eval;

import com.stackcraft.retroflow.service.SummaryModel;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps the real summary model and adds up the tokens it costs, so the eval
 * can report what a run costs. It changes nothing about the answers.
 */
class CountingSummaryModel implements SummaryModel {

    private final SummaryModel real;
    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();

    CountingSummaryModel(SummaryModel real) {
        this.real = real;
    }

    @Override
    public Answer summarise(Input input) {
        Answer answer = real.summarise(input);
        inputTokens.addAndGet(answer.inputTokens());
        outputTokens.addAndGet(answer.outputTokens());
        return answer;
    }

    long inputTokens() {
        return inputTokens.get();
    }

    long outputTokens() {
        return outputTokens.get();
    }

}
