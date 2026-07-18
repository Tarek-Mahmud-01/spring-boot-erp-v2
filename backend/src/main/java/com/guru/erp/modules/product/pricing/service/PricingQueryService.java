package com.guru.erp.modules.product.pricing.service;

import com.guru.erp.modules.product.pricing.domain.PriceList;
import com.guru.erp.modules.product.pricing.domain.PriceListItem;
import com.guru.erp.modules.product.pricing.domain.ProductLocationOverride;
import com.guru.erp.modules.product.pricing.dto.PricingDtos.LocationOverrideResponse;
import com.guru.erp.modules.product.pricing.dto.PricingDtos.PriceHistoryResponse;
import com.guru.erp.modules.product.pricing.dto.PricingDtos.PriceListItemResponse;
import com.guru.erp.modules.product.pricing.dto.PricingDtos.PriceListResponse;
import com.guru.erp.modules.product.pricing.mapper.PricingMapper;
import com.guru.erp.modules.product.pricing.repository.PriceHistoryRepository;
import com.guru.erp.modules.product.pricing.repository.PriceListItemRepository;
import com.guru.erp.modules.product.pricing.repository.PriceListRepository;
import com.guru.erp.modules.product.pricing.repository.ProductLocationOverrideRepository;
import com.guru.erp.platform.error.DomainException;
import com.guru.erp.platform.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side use-cases for the pricing slice. All queries run read-only and lean
 * on the {@code @SQLRestriction} soft-delete filter from BaseEntity.
 */
@Service
@Transactional(readOnly = true)
public class PricingQueryService {

    private final PriceListRepository priceLists;
    private final PriceListItemRepository priceListItems;
    private final PriceHistoryRepository priceHistory;
    private final ProductLocationOverrideRepository overrides;
    private final PricingMapper mapper;

    public PricingQueryService(PriceListRepository priceLists,
                               PriceListItemRepository priceListItems,
                               PriceHistoryRepository priceHistory,
                               ProductLocationOverrideRepository overrides,
                               PricingMapper mapper) {
        this.priceLists = priceLists;
        this.priceListItems = priceListItems;
        this.priceHistory = priceHistory;
        this.overrides = overrides;
        this.mapper = mapper;
    }

    public PageResponse<PriceListResponse> listPriceLists(String companyId, Pageable pageable) {
        String company = (companyId == null || companyId.isBlank()) ? null : companyId;
        return PageResponse.of(priceLists.search(company, pageable), mapper::toResponse);
    }

    public PriceListResponse getPriceList(String publicId) {
        return mapper.toResponse(loadPriceList(publicId));
    }

    public PageResponse<PriceListItemResponse> listItems(String priceListPublicId, Pageable pageable) {
        PriceList pl = loadPriceList(priceListPublicId);
        return PageResponse.of(priceListItems.findByPriceListId(pl.getId(), pageable), mapper::toResponse);
    }

    public PriceListItemResponse getItem(String publicId) {
        PriceListItem item = priceListItems.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PriceListItem", publicId));
        return mapper.toResponse(item);
    }

    public PageResponse<PriceHistoryResponse> listPriceHistory(String productId, Pageable pageable) {
        return PageResponse.of(priceHistory.findByProductId(productId, pageable), mapper::toResponse);
    }

    public PageResponse<LocationOverrideResponse> listOverrides(String productId, Pageable pageable) {
        return PageResponse.of(overrides.findByProductId(productId, pageable), mapper::toResponse);
    }

    public LocationOverrideResponse getOverride(String publicId) {
        ProductLocationOverride o = overrides.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("ProductLocationOverride", publicId));
        return mapper.toResponse(o);
    }

    private PriceList loadPriceList(String publicId) {
        return priceLists.findByPublicId(publicId)
            .orElseThrow(() -> DomainException.notFound("PriceList", publicId));
    }
}
