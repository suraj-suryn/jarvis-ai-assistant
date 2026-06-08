package com.jarus.ai.model;

import java.util.ArrayList;
import java.util.List;

public class CompanyResearch {
    private String companyName;
    private String overview;
    private int interviewRounds;
    private List<String> interviewQuestions = new ArrayList<>();
    private String tips;
    private String linkedInSearchUrl;
    private String glassdoorSearchUrl;
    private String googleSearchUrl;
    private List<String> searchResults = new ArrayList<>();

    public CompanyResearch() {}

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public int getInterviewRounds() { return interviewRounds; }
    public void setInterviewRounds(int interviewRounds) { this.interviewRounds = interviewRounds; }
    public List<String> getInterviewQuestions() { return interviewQuestions; }
    public void setInterviewQuestions(List<String> interviewQuestions) { this.interviewQuestions = interviewQuestions; }
    public String getTips() { return tips; }
    public void setTips(String tips) { this.tips = tips; }
    public String getLinkedInSearchUrl() { return linkedInSearchUrl; }
    public void setLinkedInSearchUrl(String linkedInSearchUrl) { this.linkedInSearchUrl = linkedInSearchUrl; }
    public String getGlassdoorSearchUrl() { return glassdoorSearchUrl; }
    public void setGlassdoorSearchUrl(String glassdoorSearchUrl) { this.glassdoorSearchUrl = glassdoorSearchUrl; }
    public String getGoogleSearchUrl() { return googleSearchUrl; }
    public void setGoogleSearchUrl(String googleSearchUrl) { this.googleSearchUrl = googleSearchUrl; }
    public List<String> getSearchResults() { return searchResults; }
    public void setSearchResults(List<String> searchResults) { this.searchResults = searchResults; }
}
