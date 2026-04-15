package com.example.springai.dto;

public class ChatResponse {

    private String answer;
    private String model;
    private Long usage;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String model, Long usage) {
        this.answer = answer;
        this.model = model;
        this.usage = usage;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getUsage() {
        return usage;
    }

    public void setUsage(Long usage) {
        this.usage = usage;
    }
}