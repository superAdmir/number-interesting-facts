package com.nip.numberinterestingfacts;

import static org.junit.Assert.assertEquals;

import com.nip.numberinterestingfacts.facts.FactCategory;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class BannerMappingTest {
    @Test
    public void eachCategoryKeepsItsOwnBannerUnit() {
        assertEquals(R.string.admob_banner_random_unit_id, RandomActivity.bannerUnitFor(FactCategory.TRIVIA));
        assertEquals(R.string.admob_banner_year_unit_id, RandomActivity.bannerUnitFor(FactCategory.YEAR));
        assertEquals(R.string.admob_banner_date_unit_id, RandomActivity.bannerUnitFor(FactCategory.DATE));
        assertEquals(R.string.admob_banner_math_unit_id, RandomActivity.bannerUnitFor(FactCategory.MATH));
        Set<Integer> distinct = new HashSet<>();
        for (FactCategory c : FactCategory.values()) distinct.add(RandomActivity.bannerUnitFor(c));
        assertEquals(FactCategory.values().length, distinct.size());
    }
}
