package com.github.jenkins.multiLastChanges.model;

public class MultiLastChangesConfig {
	
	private FormatType format = FormatType.LINE;
	private MatchingType matching = MatchingType.NONE;
	private String matchWordsThreshold = "0.25";
	private String matchingMaxComparisons = "1000";
	private Boolean showFiles = Boolean.TRUE;
	private Boolean synchronisedScroll = Boolean.TRUE;
	
	
	
	public MultiLastChangesConfig() {
	}

	public MultiLastChangesConfig(FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold, String matchingMaxComparisons) {
		super();
		if(format != null){
			this.format = format;
		}
		if(matching != null){
			this.matching = matching;
		}
		if(showFiles != null){
			this.showFiles = showFiles;
		}
		if(synchronisedScroll != null){
			this.synchronisedScroll = synchronisedScroll;
		}
		if(matchingMaxComparisons != null){
			try{
				this.matchingMaxComparisons = String.valueOf(Double.parseDouble(matchingMaxComparisons));
			}catch(NumberFormatException e){
				//invalid number stay with default
			}
		}
		
		if(matchWordsThreshold != null){
			try{
				this.matchWordsThreshold = String.valueOf(Integer.parseInt(matchWordsThreshold));
			}catch(NumberFormatException e){
				//invalid number stay with default
			}
		}
		
	}
	
	public FormatType format() {
		return format;
	}
	public MatchingType matching() {
		return matching;
	}
	
	
	public String showFiles(){
		return showFiles.toString();
	}
	
	public String synchronisedScroll(){
		return synchronisedScroll.toString();
	}

	public String matchingMaxComparisons(){
		return matchingMaxComparisons;
	}
	
	public String matchWordsThreshold(){
		return matchWordsThreshold;
	}
}
