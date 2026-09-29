package com.insurmatch.dto;

import com.insurmatch.entity.Activity;
import com.insurmatch.entity.CustomerDocument;
import com.insurmatch.entity.Deal;
import com.insurmatch.entity.Note;
import com.insurmatch.entity.Task;
import com.insurmatch.entity.Ticket;
import lombok.*;

import java.util.List;

/**
 * ContactDetailDTO — Response đầy đủ cho trang Contact Detail 360°.
 * Bao gồm thông tin contact + notes, tasks, activities, deals, tickets, documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDetailDTO {

    private ContactDTO contact;
    private List<Note> notes;
    private List<Task> tasks;
    private List<Activity> activities;
    private List<Deal> deals;
    private List<Ticket> tickets;
    private List<CustomerDocument> documents;
}
