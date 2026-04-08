package com.tinyspring.garderie.service;

public interface BadWordFilterService {

    String censorText(String text);

    boolean containsBadWord(String text);
}