package com.abhi.aws.ecs.controller;

import com.abhi.aws.ecs.model.GiftCard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/giftcards")
public class GiftCardController {

    private final Map<Long, GiftCard> giftCards = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public GiftCardController() {
        // Initialize with sample gift card purchases
        addGiftCard(new GiftCard(idCounter.getAndIncrement(), "Abhishek Kumar", "Amazon", 150.00, LocalDate.now().toString()));
        addGiftCard(new GiftCard(idCounter.getAndIncrement(), "John Doe", "Starbucks", 50.00, LocalDate.now().toString()));
        addGiftCard(new GiftCard(idCounter.getAndIncrement(), "Jane Smith", "McDonalds", 25.00, LocalDate.now().toString()));
    }

    private void addGiftCard(GiftCard giftCard) {
        giftCards.put(giftCard.getId(), giftCard);
    }

    @GetMapping
    public ResponseEntity<List<GiftCard>> getAllGiftCards() {
        return ResponseEntity.ok(new ArrayList<>(giftCards.values()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GiftCard> getGiftCardById(@PathVariable Long id) {
        GiftCard giftCard = giftCards.get(id);
        if (giftCard == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(giftCard);
    }

    @PostMapping
    public ResponseEntity<GiftCard> createGiftCard(@RequestBody GiftCard giftCard) {
        giftCard.setId(idCounter.getAndIncrement());
        if (giftCard.getDate() == null || giftCard.getDate().isEmpty()) {
            giftCard.setDate(LocalDate.now().toString());
        }
        giftCards.put(giftCard.getId(), giftCard);
        return ResponseEntity.ok(giftCard);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GiftCard> updateGiftCard(@PathVariable Long id, @RequestBody GiftCard updatedGiftCard) {
        GiftCard giftCard = giftCards.get(id);
        if (giftCard == null) {
            return ResponseEntity.notFound().build();
        }
        updatedGiftCard.setId(id);
        giftCards.put(id, updatedGiftCard);
        return ResponseEntity.ok(updatedGiftCard);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGiftCard(@PathVariable Long id) {
        GiftCard removed = giftCards.remove(id);
        if (removed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getCount() {
        return ResponseEntity.ok(Map.of(
                "count", giftCards.size(),
                "timestamp", LocalDateTime.now()
        ));
    }

    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> getTotalAmount() {
        double total = giftCards.values().stream()
                .mapToDouble(GiftCard::getAmount)
                .sum();
        return ResponseEntity.ok(Map.of(
                "totalAmount", total,
                "totalPurchases", giftCards.size(),
                "timestamp", LocalDateTime.now()
        ));
    }

    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<GiftCard>> getGiftCardsByType(@PathVariable String type) {
        List<GiftCard> filtered = giftCards.values().stream()
                .filter(gc -> gc.getGiftCardType().equalsIgnoreCase(type))
                .toList();
        return ResponseEntity.ok(filtered);
    }
}
