package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.DiscountCode;
import com.kopi.kopi.entity.DiscountEvent;
import com.kopi.kopi.repository.DiscountCodeRepository;
import com.kopi.kopi.repository.DiscountEventRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoServiceImplTest {

    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private DiscountEventRepository discountEventRepository;

    private PromoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PromoServiceImpl(discountCodeRepository, discountEventRepository);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    // ---------- Helpers ----------
    private DiscountCode dc(Integer id, String code, String desc,
                            Boolean active, LocalDateTime starts, LocalDateTime ends,
                            LocalDateTime created, BigDecimal value) {
        DiscountCode d = new DiscountCode();
        d.setDiscountCodeId(id);
        d.setCode(code);
        d.setDescription(desc);
        d.setActive(active);
        d.setStartsAt(starts);
        d.setEndsAt(ends);
        d.setCreatedAt(created);
        d.setDiscountValue(value);
        // d.setDiscountType(null); // không cần cho test này
        return d;
    }

    private DiscountEvent ev(Integer id, String name, String desc,
                             Boolean active, LocalDateTime starts, LocalDateTime ends,
                             LocalDateTime created, BigDecimal value) {
        DiscountEvent e = new DiscountEvent();
        e.setDiscountEventId(id);
        e.setName(name);
        e.setDescription(desc);
        e.setActive(active);
        e.setStartsAt(starts);
        e.setEndsAt(ends);
        e.setCreatedAt(created);
        e.setDiscountValue(value);
        // e.setDiscountType(null);
        return e;
    }

    // ================================= list =================================

    @Test
    void should_FilterCurrentStatus_CodesAndEvents() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        // current: active & starts <= now & ends >= now
        var codeCurrent = dc(1, "NEWYEAR", "desc", true, now.minusDays(1), now.plusDays(1), now.minusDays(2), new BigDecimal("10"));
        // upcoming: starts > now
        var codeUpcoming = dc(2, "UPCOMING", "desc", true, now.plusDays(1), now.plusDays(5), now.minusDays(1), new BigDecimal("5"));
        // inactive
        var codeInactive = dc(3, "INACTIVE", "desc", false, now.minusDays(5), now.plusDays(5), now.minusDays(5), new BigDecimal("1"));

        var eventCurrent = ev(10, "Sale Day", "hot", true, now.minusHours(2), now.plusHours(2), now.minusDays(1), new BigDecimal("20"));
        var eventUpcoming = ev(11, "Coming Soon", "warm", true, now.plusDays(2), now.plusDays(3), now, new BigDecimal("15"));

        when(discountCodeRepository.findAll()).thenReturn(List.of(codeCurrent, codeUpcoming, codeInactive));
        when(discountEventRepository.findAll()).thenReturn(List.of(eventCurrent, eventUpcoming));

        // When
        Map<String, Object> res = service.list(1, 20, null, "current", null);

        // Then
        var data = (List<Map<String,Object>>) res.get("data");
        assertThat(data).hasSize(2);
        assertThat(data).extracting(m -> m.get("title")).containsExactlyInAnyOrder("NEWYEAR", "Sale Day");
        assertThat(data).extracting(m -> m.get("kind")).containsExactlyInAnyOrder("CODE", "EVENT");
        verify(discountCodeRepository).findAll();
        verify(discountEventRepository).findAll();
    }

    @Test
    void should_FilterAvailable_When_NoStatusAndAvailableTrue() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        // available = active && endsAt >= now (bất kể starts ở đâu, miễn còn hạn)
        var current = dc(1, "CUR", null, true, now.minusDays(2), now.plusDays(2), now.minusDays(3), null);
        var upcomingEndsFuture = dc(2, "UP", null, true, now.plusDays(1), now.plusDays(10), now, null);
        var expired = dc(3, "OLD", null, true, now.minusDays(10), now.minusDays(1), now.minusDays(11), null);
        var inactive = dc(4, "OFF", null, false, now.minusDays(1), now.plusDays(1), now.minusDays(1), null);

        when(discountCodeRepository.findAll()).thenReturn(List.of(current, upcomingEndsFuture, expired, inactive));
        when(discountEventRepository.findAll()).thenReturn(List.of()); // không có event

        // When
        Map<String, Object> res = service.list(1, 50, "true", null, null);

        // Then
        var data = (List<Map<String,Object>>) res.get("data");
        assertThat(data).extracting(m -> m.get("title")).containsExactlyInAnyOrder("CUR", "UP");
        assertThat(data).allSatisfy(m -> assertThat(m.get("kind")).isEqualTo("CODE"));
    }

    @Test
    void should_SearchByNameOrDescription_ForCodeAndEvent() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        var codeByCode = dc(1, "BLACKFRIDAY", "great", true, now.minusDays(1), now.plusDays(1), now, null);
        var codeByDesc = dc(2, "ANY", "super SALE day", true, now.minusDays(1), now.plusDays(1), now, null);

        var eventByName = ev(10, "Mega Sale", "desc", true, now.minusDays(1), now.plusDays(1), now, null);
        var eventNoMatch = ev(11, "Random", "boring", true, now.minusDays(1), now.plusDays(1), now, null);

        when(discountCodeRepository.findAll()).thenReturn(List.of(codeByCode, codeByDesc));
        when(discountEventRepository.findAll()).thenReturn(List.of(eventByName, eventNoMatch));

        // When (search = "sale" → match codeByDesc.description & eventByName.name; "BLACKFRIDAY" không chứa "sale")
        Map<String, Object> res = service.list(1, 20, null, "available", "sale");

        // Then
        var data = (List<Map<String,Object>>) res.get("data");
        assertThat(data).extracting(m -> m.get("title")).containsExactlyInAnyOrder("ANY", "Mega Sale");
        assertThat(data).extracting(m -> m.get("kind")).containsExactlyInAnyOrder("CODE", "EVENT");
    }

    @Test
    void should_SortDescAndPaginate_WithStatusAll() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        // Sort theo startsAt nếu có, otherwise theo createdAt; tất cả included khi status=all
        var a = dc(1, "A", null, false, now.minusDays(1), now.plusDays(1), now.minusDays(5), null);
        var b = dc(2, "B", null, true,  now.plusDays(2),  now.plusDays(5), now.minusDays(4), null);
        var c = dc(3, "C", null, true,  null,             null,            now.minusDays(1), null); // sort by createdAt
        var d = dc(4, "D", null, true,  now.plusDays(1),  now.plusDays(3), now.minusDays(3), null);

        // expected order by key: B(start +2), D(+1), A(-1), C(created -1)
        when(discountCodeRepository.findAll()).thenReturn(List.of(a,b,c,d));
        when(discountEventRepository.findAll()).thenReturn(List.of());

        // page=1, limit=2 => first two items after sort desc: B, D
        Map<String, Object> p1 = service.list(1, 2, null, "all", null);
        // page=2, limit=2 => next two: A, C
        Map<String, Object> p2 = service.list(2, 2, null, "all", null);

        // Then
        var data1 = (List<Map<String,Object>>) p1.get("data");
        assertThat(data1).extracting(m -> m.get("title")).containsExactly("B", "D");
        var meta1 = (Map<String, Object>) p1.get("meta");
        assertThat(meta1.get("currentPage")).isEqualTo(1);
        assertThat(meta1.get("totalPage")).isEqualTo(2);
        assertThat(meta1.get("prev")).isEqualTo(false);
        assertThat(meta1.get("next")).isEqualTo(true);

        var data2 = (List<Map<String,Object>>) p2.get("data");
        assertThat(data2).extracting(m -> m.get("title")).containsExactly("A", "C");
        var meta2 = (Map<String, Object>) p2.get("meta");
        assertThat(meta2.get("currentPage")).isEqualTo(2);
        assertThat(meta2.get("totalPage")).isEqualTo(2);
        assertThat(meta2.get("prev")).isEqualTo(true);
        assertThat(meta2.get("next")).isEqualTo(false);
    }
}
