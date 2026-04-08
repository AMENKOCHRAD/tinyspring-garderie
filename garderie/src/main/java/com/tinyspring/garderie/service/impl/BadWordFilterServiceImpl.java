package com.tinyspring.garderie.service.impl;

import com.tinyspring.garderie.service.BadWordFilterService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BadWordFilterServiceImpl implements BadWordFilterService {

    private final List<String> badWords = Arrays.asList(
            "idiot",
            "imbecile",
            "nul",
            "stupide",
            "haine",
            "sale",
            "insulte1",
            "insulte2",
            "badword1",
            "badword2"
    );

    @Override
    public String censorText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String filteredText = text;

        for (String badWord : badWords) {
            String regex = "(?i)\\b" + Pattern.quote(badWord) + "\\b";
            filteredText = filteredText.replaceAll(regex, "***");
        }

        return filteredText;
    }

    @Override
    public boolean containsBadWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        for (String badWord : badWords) {
            String regex = "(?i).*\\b" + Pattern.quote(badWord) + "\\b.*";
            if (text.matches(regex)) {
                return true;
            }
        }

        return false;
    }
}