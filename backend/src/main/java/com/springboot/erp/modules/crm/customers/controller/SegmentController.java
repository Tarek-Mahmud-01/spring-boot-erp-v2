package com.springboot.erp.modules.crm.customers.controller;

import com.springboot.erp.modules.crm.customers.dto.SegmentDtos.SegmentCreateRequest;
import com.springboot.erp.modules.crm.customers.dto.SegmentDtos.SegmentMemberChangeRequest;
import com.springboot.erp.modules.crm.customers.dto.SegmentDtos.SegmentResponse;
import com.springboot.erp.modules.crm.customers.dto.SegmentDtos.SegmentUpdateRequest;
import com.springboot.erp.modules.crm.customers.service.SegmentCommandService;
import com.springboot.erp.modules.crm.customers.service.SegmentMemberService;
import com.springboot.erp.modules.crm.customers.service.SegmentQueryService;
import com.springboot.erp.platform.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ENT-073 CustomerSegment + ENT-073a CustomerSegmentMember endpoints
 * (US-042) — thin controller, {@code @PreAuthorize} per method. Business
 * rules live in the command/query services.
 */
@RestController
@RequestMapping("/api/crm/segments")
public class SegmentController {

    private final SegmentCommandService command;
    private final SegmentMemberService members;
    private final SegmentQueryService query;

    public SegmentController(SegmentCommandService command, SegmentMemberService members,
                             SegmentQueryService query) {
        this.command = command;
        this.members = members;
        this.query = query;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm.segment.read')")
    public PageResponse<SegmentResponse> list(@RequestParam(required = false) String companyId,
                                              @PageableDefault(size = 50) Pageable pageable) {
        return query.list(companyId, pageable);
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('crm.segment.read')")
    public SegmentResponse get(@PathVariable String publicId) {
        return query.get(publicId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('crm.segment.write')")
    public SegmentResponse create(@Valid @RequestBody SegmentCreateRequest request) {
        return command.create(request);
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('crm.segment.write')")
    public SegmentResponse update(@PathVariable String publicId,
                                  @Valid @RequestBody SegmentUpdateRequest request) {
        return command.update(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('crm.segment.write')")
    public void delete(@PathVariable String publicId) {
        command.delete(publicId);
    }

    /** FR-219/220 — add a batch of customers to a STATIC segment. */
    @PostMapping("/{publicId}/members")
    @PreAuthorize("hasAuthority('crm.segment.member.write')")
    public SegmentResponse addMembers(@PathVariable String publicId,
                                      @Valid @RequestBody SegmentMemberChangeRequest request) {
        return members.addMembers(publicId, request);
    }

    /** FR-219/220 — remove a batch of customers from a STATIC segment. */
    @DeleteMapping("/{publicId}/members")
    @PreAuthorize("hasAuthority('crm.segment.member.write')")
    public SegmentResponse removeMembers(@PathVariable String publicId,
                                         @Valid @RequestBody SegmentMemberChangeRequest request) {
        return members.removeMembers(publicId, request);
    }
}
