package com.demoproject.shoppingcart.dto;

public class DeliveryFeedbackStatusDTO {
    private boolean feedbackSubmitted;
    private Integer rating;
    private String comment;

    public DeliveryFeedbackStatusDTO() {
    }

    public DeliveryFeedbackStatusDTO(boolean feedbackSubmitted, Integer rating, String comment) {
        this.feedbackSubmitted = feedbackSubmitted;
        this.rating = rating;
        this.comment = comment;
    }

    public boolean isFeedbackSubmitted() {
        return feedbackSubmitted;
    }

    public void setFeedbackSubmitted(boolean feedbackSubmitted) {
        this.feedbackSubmitted = feedbackSubmitted;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
