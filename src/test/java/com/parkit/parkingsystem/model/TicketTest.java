package com.parkit.parkingsystem.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketTest {
    private Ticket ticket;

    @Test
    @DisplayName("should set id of ticket and get ticket Id")
    void getTicketId() {
        ticket = new Ticket();
        ticket.setId(42);
        assertEquals(42, ticket.getId());
    }

}