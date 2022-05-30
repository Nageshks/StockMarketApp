package com.plcoding.stockmarketapp.data.mapper

import com.plcoding.stockmarketapp.data.remote.dto.IntradayInfoDto
import com.plcoding.stockmarketapp.domain.model.IntradayInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private const val pattern = "yyyy-MM-dd HH:mm:ss"

fun IntradayInfoDto.toIntradayInfo(): IntradayInfo {
        return IntradayInfo(
            date = LocalDateTime.parse(
                timestamp,
                DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            ),
            close = close
        )
}