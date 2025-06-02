package com.lin.bot.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author Lin.
 * @Date 2025/1/11
 */
@Data
@Component
@ConfigurationProperties(prefix = "magic-regex")
public class MagicRegexConfig {
    private Map<String, RegexRule> rules;

    @Data
    public static class RegexRule {
        private String pattern;
        private String replace;
    }
}

