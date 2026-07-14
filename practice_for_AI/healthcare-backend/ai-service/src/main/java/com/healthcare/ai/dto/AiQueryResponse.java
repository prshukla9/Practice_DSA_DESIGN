package com.healthcare.ai.dto;

import java.util.List;

public class AiQueryResponse {
    private String answer;
    private List<String> retrievedContext;
    private String disclaimer;
    private boolean usedFallback;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<String> getRetrievedContext() { return retrievedContext; }
    public void setRetrievedContext(List<String> retrievedContext) { this.retrievedContext = retrievedContext; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public boolean isUsedFallback() { return usedFallback; }
    public void setUsedFallback(boolean usedFallback) { this.usedFallback = usedFallback; }
}
