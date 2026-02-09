package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public class FareCalculatorService {

    public void calculateFare(final Ticket ticket, final boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        final long inHour = ticket.getInTime().getTime();
        final long outHour = ticket.getOutTime().getTime();

        final Duration duration = Duration.ofMillis(outHour - inHour);

        if (duration.toMinutes() < 30) {
            ticket.setPrice(0d);
            return;
        }

        final double durationHours = BigDecimal.valueOf(duration.toMillis() / (1_000d * 60d * 60d))
                .doubleValue();

        double fare = 0;

        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                fare = Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                fare = Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default:
                throw new IllegalArgumentException("Unkown Parking Type");
        }

        double finalPrice = durationHours * fare;
        if (discount) {
            finalPrice *= 0.95;
        }
        ticket.setPrice(finalPrice);

    }

    public void calculateFare(final Ticket ticket) {
        calculateFare(ticket, false);
    }
}
