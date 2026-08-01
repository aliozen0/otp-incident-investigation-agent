package com.example.otpsentinel.api.dto;

import java.time.Instant;

public record TimeWindowDto(Instant startAt, Instant endAt, String timezone) {}
