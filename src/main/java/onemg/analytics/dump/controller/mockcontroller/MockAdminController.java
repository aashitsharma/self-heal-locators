package onemg.analytics.dump.controller.mockcontroller;


import onemg.analytics.dump.model.ErrorModel;
import onemg.analytics.dump.model.MockDataModel;
import onemg.analytics.dump.repository.MockDataRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/admin/mock-data")
public class MockAdminController
{
    private static final Logger LOGGER = Logger.getLogger(MockAdminController.class);
    @Autowired
    private MockDataRepository mockDataRepository;

    // 🔹 Create new mock data
    @PostMapping("/create")
    public ResponseEntity<?> createMock(@RequestBody MockDataModel request) {
        Optional<MockDataModel> existing = mockDataRepository.findByUriAndVerticalAndMethod(
                request.getUri(), request.getVertical(), request.getMethod());

        if (existing.isPresent()) {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.CONFLICT.value(),"Mock data already exists for this combination."), HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()));
        }

        MockDataModel model = toModel(request);
        mockDataRepository.save(model);
        return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.CREATED.value(),"Mock data created."), HttpStatusCode.valueOf(HttpStatus.CREATED.value()));
    }

    // 🔹 Update mock data (based on unique combo)
    @PutMapping("/update")
    public ResponseEntity<?> updateMock(@RequestBody MockDataModel request) {
        Optional<MockDataModel> existing = mockDataRepository.findByUriAndVerticalAndMethod(
                request.getUri(), request.getVertical(), request.getMethod());

        if (existing.isEmpty()) {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Mock data not found for update."), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }

        MockDataModel model = existing.get();
        model.setResponse(request.getResponse());
        model.setResponseCode(request.getResponseCode());
        model.setUri(request.getUri());

        mockDataRepository.save(model);
        return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.OK.value(),"Mock data updated."), HttpStatusCode.valueOf(HttpStatus.OK.value()));
    }

    // 🔹 Fetch mock data (based on unique combo)
    @GetMapping("/fetch")
    public ResponseEntity<?> getMockData(
            @RequestParam String vertical,
            @RequestParam String uri,
            @RequestParam HttpMethod method
    ) {
        Optional<MockDataModel> optionalMock = mockDataRepository.findByUriAndVerticalAndMethod(uri, vertical, method.name());

        if (optionalMock.isPresent()) {
            return ResponseEntity.ok(optionalMock.get());
        } else {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Mock data not found."), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMockData(
            @RequestParam String vertical,
            @RequestParam String uri,
            @RequestParam HttpMethod method
    ) {
        Optional<MockDataModel> existing = mockDataRepository.findByUriAndVerticalAndMethod(uri, vertical, method.name());

        if (existing.isPresent()) {
            mockDataRepository.delete(existing.get());
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.OK.value(),"Mock data deleted successfully."), HttpStatusCode.valueOf(HttpStatus.OK.value()));
        } else {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Mock data not found."), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }
    }

    // 🔸 Reusable mapper
    private MockDataModel toModel(MockDataModel req) {
        MockDataModel model = new MockDataModel();
        model.setUri(req.getUri());
        model.setVertical(req.getVertical());
        model.setMethod(req.getMethod());
        model.setResponse(req.getResponse());
        model.setResponseCode(req.getResponseCode());
        return model;
    }
}
