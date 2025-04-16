package com.example.ai4speech.utils;

import org.apache.commons.text.similarity.LevenshteinDistance;

public class SpeechMetricsUtils {

    public static double calculateAccuracy(String reference, String hypothesis) {
        String[] refWords = reference.trim().toLowerCase().split("\\s+");
        String[] hypWords = hypothesis.trim().toLowerCase().split("\\s+");

        int substitutions = 0, deletions = 0, insertions = 0;

        int[][] dp = new int[refWords.length + 1][hypWords.length + 1];

        for (int i = 0; i <= refWords.length; i++) {
            for (int j = 0; j <= hypWords.length; j++) {
                if (i == 0) dp[i][j] = j;
                else if (j == 0) dp[i][j] = i;
                else if (refWords[i - 1].equals(hypWords[j - 1])) dp[i][j] = dp[i - 1][j - 1];
                else dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], // substitution
                            Math.min(dp[i - 1][j],        // deletion
                                    dp[i][j - 1]));       // insertion
            }
        }

        int wer = dp[refWords.length][hypWords.length];
        return Math.max(0, 1 - ((double) wer / refWords.length)); // normalized
    }

    public static double calculateFluency(String transcript, double durationSeconds) {
        if (durationSeconds == 0) return 0;
        int wordCount = transcript.trim().split("\\s+").length;
        return (double) wordCount / durationSeconds;
    }

    public static double calculatePronunciation(String reference, String hypothesis) {
        LevenshteinDistance levenshtein = new LevenshteinDistance();
        int distance = levenshtein.apply(reference.toLowerCase(), hypothesis.toLowerCase());
        int maxLen = Math.max(reference.length(), hypothesis.length());
        return Math.max(0, 1 - ((double) distance / maxLen));
    }
}
