package com.paypulse.transaction.config;

import com.paypulse.common.GlobalExceptionHandler;

/**
 * Наследует общий обработчик исключений из paypulse-common
 * Добавляет специфичную для transaction-service обработку исключений
 */
public class TransactionGlobalExceptionHandler extends GlobalExceptionHandler {
    // Дополнительная обработка исключений специфичных для transaction-service
    // может быть добавлена здесь при необходимости
}

