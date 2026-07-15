package com.edu.domain.beacon.controller;

import com.edu.common.dto.PageResponse;
import com.edu.domain.beacon.dto.request.BeaconRequest;
import com.edu.domain.beacon.service.BeaconService;
import com.edu.domain.beacon.vo.Beacon;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비콘 관리 REST API (BCN-01~03 = ATT-18).
 * 조회는 ADMIN/TEACHER, 등록/수정/삭제는 ADMIN.
 */
@RestController
@RequestMapping("/api/beacons")
public class BeaconController {

    private final BeaconService beaconService;

    public BeaconController(BeaconService beaconService) {
        this.beaconService = beaconService;
    }

    /** BCN-01 목록 조회 (반 필터 선택, 페이징) */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public PageResponse<Beacon> list(@RequestParam(required = false) Long classId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return beaconService.getList(classId, page, size);
    }

    /** 단건 조회 */
    @GetMapping("/{beaconId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Beacon get(@PathVariable Long beaconId) {
        return beaconService.get(beaconId);
    }

    /** BCN-02 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Beacon create(@RequestBody BeaconRequest request) {
        return beaconService.create(request);
    }

    /** BCN-02 수정 */
    @PutMapping("/{beaconId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Beacon update(@PathVariable Long beaconId, @RequestBody BeaconRequest request) {
        return beaconService.update(beaconId, request);
    }

    /** BCN-03 삭제 */
    @DeleteMapping("/{beaconId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long beaconId) {
        beaconService.delete(beaconId);
    }
}
