package onemg.analytics.dump.controller.mockcontroller;


import onemg.analytics.dump.model.ErrorModel;
import onemg.analytics.dump.model.MockDataModel;
import onemg.analytics.dump.model.MockDataVertical;
import onemg.analytics.dump.repository.MockDataRepository;
import onemg.analytics.dump.repository.MockDataVerticalRepository;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/mock-data")
public class MockAdminController
{
    private static final Logger LOGGER = Logger.getLogger(MockAdminController.class);
    @Autowired
    private MockDataRepository mockDataRepository;
    @Autowired
    private MockDataVerticalRepository mVerticalRepository;

    // 🔹 Create new mock data
    @PostMapping("/create")
    public ResponseEntity<?> createMock(@RequestBody MockDataModel request) {
        Optional<MockDataModel> existing = mockDataRepository.findByUriAndVerticalAndMethod(
                request.getUri().trim(), request.getVertical().trim(), request.getMethod());

        if (existing.isPresent()) {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.CONFLICT.value(),"Mock data already exists for this combination."), HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()));
        }

        MockDataModel model = toModel(request);
        mockDataRepository.save(model);
        return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.CREATED.value(),"Mock data created for : "+request.getUri().trim()), HttpStatusCode.valueOf(HttpStatus.CREATED.value()));
    }

    // 🔹 Update mock data (based on unique combo)
    @PutMapping("/update")
    public ResponseEntity<?> updateMock(@RequestBody MockDataModel request) {
        Optional<MockDataModel> existing = mockDataRepository.findByUriAndVerticalAndMethod(
                request.getUri().trim(), request.getVertical().trim(), request.getMethod());

        if (existing.isEmpty()) {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Mock data not found of for : "+request.getUri().trim()), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }

        MockDataModel model = existing.get();
        model.setResponse(request.getResponse());
        model.setResponseCode(request.getResponseCode());
        model.setUri(request.getUri());

        mockDataRepository.save(model);
        return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.OK.value(),"Mock data updated for : "+request.getUri().trim()), HttpStatusCode.valueOf(HttpStatus.OK.value()));
    }

    // 🔹 Fetch mock data (based on unique combo)
    @GetMapping("/fetch")
    public ResponseEntity<?> getMockData(
            @RequestParam String vertical,
            @RequestParam String uri,
            @RequestParam HttpMethod method
    ) {
        Optional<MockDataModel> optionalMock = null;
        List<MockDataModel> mockList = new ArrayList<>();
        if( null!=method && uri.isEmpty() || uri.isBlank()){
            mockList = mockDataRepository.findByVertical(vertical.trim());
        }
        else if(uri.isEmpty() || uri.isBlank()){
            mockList = mockDataRepository.findByVerticalAndMethod(vertical.trim(),method.name());
        }
        else {
            optionalMock = mockDataRepository.findByUriAndVerticalAndMethod(uri.trim(), vertical.trim(), method.name());

        }

        if ( null!=optionalMock &&optionalMock.isPresent()) {
            return ResponseEntity.ok(optionalMock.get());
        }
        else if(!mockList.isEmpty()){
            return ResponseEntity.ok(mockList);
        }else {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Mock data not found for given combination."), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteMockData(
            @RequestParam String vertical,
            @RequestParam String uri,
            @RequestParam HttpMethod method
    ) {
        Optional<MockDataModel> existing = mockDataRepository.findByUriAndVerticalAndMethod(uri.trim(), vertical.trim(), method.name());

        if (existing.isPresent()) {
            mockDataRepository.delete(existing.get());
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.OK.value(),"Mock data deleted successfully for : "+uri.trim()), HttpStatusCode.valueOf(HttpStatus.OK.value()));
        } else {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Mock data not found for : "+uri.trim()), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMockDataById(@PathVariable("id")String dataId){
        Optional<MockDataModel> data = mockDataRepository.findById(dataId.trim());

        if(data.isPresent()){
            mockDataRepository.delete(data.get());
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.OK.value(),"Mock data deleted successfully for Id : "+dataId), HttpStatusCode.valueOf(HttpStatus.OK.value()));

        }
        else {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Mock data not found for Id : "+dataId), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }
    }

    // 🔹 Create new Vertical data
    @PostMapping("/vertical/add")
    public ResponseEntity<?> createMockVertical(@RequestBody MockDataVertical request) {
        Optional<MockDataVertical> existing = mVerticalRepository.findByVerticalName(request.getVerticalName().toUpperCase());

        if (existing.isPresent()) {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.CONFLICT.value(),"Vertical already exist : "+request.getVerticalName()), HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()));
        }
        else if(!request.getVerticalName().matches("^[a-zA-Z]+$")){
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.BAD_REQUEST.value(),"Not a valid Vertical Name format | Expected name should be : "+request.getVerticalName().replaceAll("[^a-zA-Z]", "")), HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));

        }

        MockDataVertical model = new MockDataVertical();
        model.setVerticalName(request.getVerticalName().trim().toUpperCase());
        mVerticalRepository.save(model);
        return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.CREATED.value(),"Mock data created for : "+model.getVerticalName().trim()), HttpStatusCode.valueOf(HttpStatus.CREATED.value()));
    }

     // 🔹 Fetch mock vertical data
    @GetMapping("/vertical/fetch")
    public ResponseEntity<?> getMockVerticalData(){
        List<MockDataVertical> lVerticals = mVerticalRepository.fetchAllVerticals();
        if(lVerticals.isEmpty()){
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Vertical data set not present"), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
        }
        return ResponseEntity.ok(lVerticals);

    }

     // 🔹 Update Vertical data
    @PutMapping("/vertical/update")
    public ResponseEntity<?> updateMockVertical(@RequestBody MockDataVertical request){
        Optional<MockDataVertical> existing = mVerticalRepository.findByVerticalName(request.getVerticalName());
        if(null==request.getUpdatedVerticalName() || request.getUpdatedVerticalName().isEmpty()){
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.BAD_REQUEST.value(),"Updated Vertical Name is not provided, please check your request "), HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));
        }
        if(existing.isPresent() && !request.getVerticalName().equalsIgnoreCase(request.getUpdatedVerticalName())){
            MockDataVertical model = existing.get();
            model.setVerticalName(request.getUpdatedVerticalName().trim());
            mVerticalRepository.save(model);
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.OK.value(),"Vertical name updated from : "+request.getVerticalName() +" to : "+request.getUpdatedVerticalName()), HttpStatusCode.valueOf(HttpStatus.OK.value()));

        }
        return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.BAD_REQUEST.value(),"Vertical data set not present or expected and actual name is same "), HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()));

    }

        @DeleteMapping("/vertical/delete/{id}")
        public ResponseEntity<?> deleteMockVerticalDataById(@PathVariable("id")String dataId){
        Optional<MockDataVertical> data = mVerticalRepository.findById(dataId.trim());

        if(data.isPresent()){
            mVerticalRepository.delete(data.get());
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.OK.value(),"Vertical data deleted successfully for Id : "+dataId), HttpStatusCode.valueOf(HttpStatus.OK.value()));

        }
        else {
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.NOT_FOUND.value(),"Vertical data not found for Id : "+dataId), HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()));
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
