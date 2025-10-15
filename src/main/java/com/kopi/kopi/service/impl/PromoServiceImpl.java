package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.DiscountCode;
import com.kopi.kopi.entity.DiscountEvent;
import com.kopi.kopi.repository.DiscountCodeRepository;
import com.kopi.kopi.repository.DiscountEventRepository;
import com.kopi.kopi.service.IPromoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PromoServiceImpl implements IPromoService {
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountEventRepository discountEventRepository;

    public PromoServiceImpl(DiscountCodeRepository discountCodeRepository, DiscountEventRepository discountEventRepository) {
        this.discountCodeRepository = discountCodeRepository;
        this.discountEventRepository = discountEventRepository;
    }

    @Override
    public Map<String, Object> list(Integer page, Integer limit, String available, String status, String searchByName) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasStatus = status != null && !status.isBlank();
        boolean includeInactiveForAll = hasStatus && status.equalsIgnoreCase("all");
        boolean availableFlag = !hasStatus && "true".equalsIgnoreCase(available);

        List<Map<String, Object>> items = new ArrayList<>();

        for (DiscountCode dc : discountCodeRepository.findAll()) {
            boolean nameMatch = searchByName == null || searchByName.isBlank()
                    || dc.getCode().toLowerCase().contains(searchByName.toLowerCase())
                    || (dc.getDescription() != null && dc.getDescription().toLowerCase().contains(searchByName.toLowerCase()));
            if (!nameMatch) continue;

            boolean isCurrent = Boolean.TRUE.equals(dc.getActive()) && dc.getStartsAt() != null && dc.getEndsAt() != null
                    && (dc.getStartsAt().isBefore(now) || dc.getStartsAt().isEqual(now))
                    && (dc.getEndsAt().isAfter(now) || dc.getEndsAt().isEqual(now));
            boolean isUpcoming = Boolean.TRUE.equals(dc.getActive()) && dc.getStartsAt() != null && dc.getStartsAt().isAfter(now);
            boolean isAvailable = Boolean.TRUE.equals(dc.getActive()) && dc.getEndsAt() != null && (dc.getEndsAt().isAfter(now) || dc.getEndsAt().isEqual(now));

            boolean matched = true;
            if (includeInactiveForAll) {
                matched = true;
            } else if (hasStatus) {
                if (status.equalsIgnoreCase("current")) matched = isCurrent;
                else if (status.equalsIgnoreCase("upcoming")) matched = isUpcoming;
                else if (status.equalsIgnoreCase("available") || status.equalsIgnoreCase("current_or_upcoming")) matched = isAvailable;
                else if (status.equalsIgnoreCase("all")) matched = true;
            } else if (availableFlag) {
                matched = isAvailable;
            }
            if (!matched) continue;

            items.add(Map.of(
                    "id", dc.getDiscountCodeId(),
                    "kind", "CODE",
                    "title", dc.getCode(),
                    "description", dc.getDescription(),
                    "discountType", dc.getDiscountType() != null ? dc.getDiscountType().name() : null,
                    "discountValue", dc.getDiscountValue(),
                    "startsAt", dc.getStartsAt(),
                    "endsAt", dc.getEndsAt(),
                    "active", dc.getActive(),
                    "createdAt", dc.getCreatedAt()
            ));
        }

        for (DiscountEvent ev : discountEventRepository.findAll()) {
            boolean nameMatch = searchByName == null || searchByName.isBlank()
                    || ev.getName().toLowerCase().contains(searchByName.toLowerCase())
                    || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(searchByName.toLowerCase()));
            if (!nameMatch) continue;

            boolean isCurrent = Boolean.TRUE.equals(ev.getActive()) && ev.getStartsAt() != null && ev.getEndsAt() != null
                    && (ev.getStartsAt().isBefore(now) || ev.getStartsAt().isEqual(now))
                    && (ev.getEndsAt().isAfter(now) || ev.getEndsAt().isEqual(now));
            boolean isUpcoming = Boolean.TRUE.equals(ev.getActive()) && ev.getStartsAt() != null && ev.getStartsAt().isAfter(now);
            boolean isAvailable = Boolean.TRUE.equals(ev.getActive()) && ev.getEndsAt() != null && (ev.getEndsAt().isAfter(now) || ev.getEndsAt().isEqual(now));

            boolean matchedEv = true;
            if (includeInactiveForAll) {
                matchedEv = true;
            } else if (hasStatus) {
                if (status.equalsIgnoreCase("current")) matchedEv = isCurrent;
                else if (status.equalsIgnoreCase("upcoming")) matchedEv = isUpcoming;
                else if (status.equalsIgnoreCase("available") || status.equalsIgnoreCase("current_or_upcoming")) matchedEv = isAvailable;
                else if (status.equalsIgnoreCase("all")) matchedEv = true;
            } else if (availableFlag) {
                matchedEv = isAvailable;
            }
            if (!matchedEv) continue;

            items.add(Map.of(
                    "id", ev.getDiscountEventId(),
                    "kind", "EVENT",
                    "title", ev.getName(),
                    "description", ev.getDescription(),
                    "discountType", ev.getDiscountType() != null ? ev.getDiscountType().name() : null,
                    "discountValue", ev.getDiscountValue(),
                    "startsAt", ev.getStartsAt(),
                    "endsAt", ev.getEndsAt(),
                    "active", ev.getActive(),
                    "createdAt", ev.getCreatedAt()
            ));
        }

        items.sort(Comparator.comparing((Map<String, Object> m) -> (LocalDateTime) (m.get("startsAt") != null ? m.get("startsAt") : m.get("createdAt")))
                .reversed());

        int safeLimit = Math.max(1, limit == null ? 8 : limit);
        int safePage = Math.max(1, page == null ? 1 : page);
        int from = Math.min((safePage - 1) * safeLimit, Math.max(items.size() - 1, 0));
        int to = Math.min(from + safeLimit, items.size());
        List<Map<String, Object>> pageItems = from >= to ? List.of() : items.subList(from, to);

        int totalPages = (int) Math.ceil(items.size() / (double) safeLimit);
        Map<String, Object> meta = Map.of(
                "currentPage", safePage,
                "totalPage", totalPages,
                "prev", safePage > 1,
                "next", safePage < totalPages
        );

        return Map.of(
                "data", pageItems,
                "meta", meta
        );
    }
}


