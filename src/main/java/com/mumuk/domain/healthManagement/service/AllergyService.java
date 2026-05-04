package com.mumuk.domain.healthManagement.service;

import com.mumuk.domain.healthManagement.dto.request.AllergyRequest;
import com.mumuk.domain.healthManagement.dto.response.AllergyResponse;

public interface AllergyService {

    AllergyResponse.AllergyListRes getAllergyList(Long userId);
    AllergyResponse.AllergyListRes setAllergyList(Long userId, AllergyRequest.SetAllergyReq request);

}
