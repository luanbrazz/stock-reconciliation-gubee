package com.gubee.stock.infrastructure.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUtil {

    private final MessageSource messageSource;

    public String get(String chave, Object... args) {
        return messageSource.getMessage(
                chave,
                args,
                LocaleContextHolder.getLocale()
        );
    }
}