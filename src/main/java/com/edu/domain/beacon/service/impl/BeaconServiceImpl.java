package com.edu.domain.beacon.service.impl;

import com.edu.domain.beacon.dto.request.BeaconRequest;
import com.edu.domain.beacon.mapper.BeaconMapper;
import com.edu.domain.beacon.service.BeaconService;
import com.edu.domain.beacon.vo.Beacon;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BeaconServiceImpl implements BeaconService {

    /** rssi_threshold 미지정 시 기본값 (스키마 DEFAULT -75와 동일) */
    private static final int DEFAULT_RSSI_THRESHOLD = -75;

    private final BeaconMapper beaconMapper;

    public BeaconServiceImpl(BeaconMapper beaconMapper) {
        this.beaconMapper = beaconMapper;
    }

    @Override
    public List<Beacon> getList(Long classId) {
        return beaconMapper.findList(classId);
    }

    @Override
    public Beacon get(Long beaconId) {
        Beacon beacon = beaconMapper.findById(beaconId);
        if (beacon == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "비콘을 찾을 수 없습니다.");
        }
        return beacon;
    }

    @Override
    @Transactional
    public Beacon create(BeaconRequest request) {
        validate(request);
        Beacon beacon = toEntity(new Beacon(), request);
        beaconMapper.insert(beacon);
        return beaconMapper.findById(beacon.getBeaconId());
    }

    @Override
    @Transactional
    public Beacon update(Long beaconId, BeaconRequest request) {
        Beacon beacon = get(beaconId);        // 없으면 404
        validate(request);
        toEntity(beacon, request);
        beaconMapper.update(beacon);
        return beaconMapper.findById(beaconId);
    }

    @Override
    @Transactional
    public void delete(Long beaconId) {
        get(beaconId);                        // 없으면 404
        beaconMapper.deleteById(beaconId);
    }

    private void validate(BeaconRequest request) {
        if (request.beaconUuid() == null || request.beaconUuid().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비콘 UUID는 필수입니다.");
        }
    }

    /** 요청 값을 엔티티에 반영 (create/update 공용) */
    private Beacon toEntity(Beacon beacon, BeaconRequest r) {
        beacon.setClassId(r.classId());
        beacon.setBeaconUuid(r.beaconUuid() == null ? null : r.beaconUuid().trim());
        beacon.setMajorValue(r.majorValue());
        beacon.setMinorValue(r.minorValue());
        beacon.setRssiThreshold(r.rssiThreshold() == null ? DEFAULT_RSSI_THRESHOLD : r.rssiThreshold());
        beacon.setLocationName(r.locationName());
        beacon.setActiveYn("N".equalsIgnoreCase(r.activeYn()) ? "N" : "Y");
        beacon.setGpsLatitude(r.gpsLatitude());
        beacon.setGpsLongitude(r.gpsLongitude());
        return beacon;
    }
}
