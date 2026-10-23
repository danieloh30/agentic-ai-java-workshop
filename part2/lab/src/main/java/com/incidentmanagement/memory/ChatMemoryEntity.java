package com.incidentmanagement.memory;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Tier 2 — the durable memory row. One row per conversation ({@code memoryId}), holding
 * the retained message window as JSON. Evicted messages are not archived here.
 *
 * <p>We use an explicit String {@code id} (the incident id, as text) rather than a
 * generated one so the store can look a conversation up directly by its memory id.
 */
@Entity
public class ChatMemoryEntity extends PanacheEntityBase {

    @Id
    public String id;

    /** The conversation's messages, serialized by LangChain4j's {@code ChatMessageSerializer}. */
    @Column(columnDefinition = "text")
    public String messagesJson;
}
