package com.github.stepwise.network.models

enum class ItemStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED;

    fun russian(): String = when (this) {
        DRAFT -> "Черновик"
        SUBMITTED -> "Отправлен"
        APPROVED -> "Одобрено"
        REJECTED -> "Отклонено"
    }
}