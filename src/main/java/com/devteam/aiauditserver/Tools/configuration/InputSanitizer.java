package com.devteam.aiauditserver.Tools.configuration;


import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class InputSanitizer {

    // Sanitize method to clean user input
    public static String sanitize(String input) {
        return Jsoup.clean(input, Safelist.none());
    }
}
